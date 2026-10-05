package diniz.contabilidade.arquivos.assistente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.assistente.AssistenteDto.Acao;
import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;

/** Detecção de assunto e respostas automáticas do Assistente Diniz (sem banco e sem rede). */
class AssistenteRespostaAutomaticaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 5);

    @Test
    void reconheceAsSugestoesDoCliente() {
        assertEquals(AssistenteIntencao.ENVIAR, principal("O que eu preciso enviar este mês?"));
        assertEquals(AssistenteIntencao.PRAZOS, principal("Quais guias vencem esta semana?"));
        assertEquals(AssistenteIntencao.DEC, principal("Tenho comunicação nova da SEFAZ?"));
        assertEquals(AssistenteIntencao.PAGAMENTO, principal("Como confirmo o pagamento de uma guia?"));
        assertTrue(AssistenteIntencoes.pedeComoFazer("Como confirmo o pagamento de uma guia?"));
    }

    @Test
    void reconheceAsSugestoesDoEscritorio() {
        assertEquals(AssistenteIntencao.VENCIDAS, principal("Quais empresas estão com obrigações vencidas?"));
        assertEquals(AssistenteIntencao.RESUMO, principal("Resuma a situação da carteira hoje"));
        assertEquals(AssistenteIntencao.DEC, principal("Quais comunicações do DEC exigem atenção?"));
        assertEquals(AssistenteIntencao.CERTIDOES, principal("Quais certidões vencem nos próximos 15 dias?"));
    }

    @Test
    void saudacaoAgradecimentoEPaginaNaoViramOutroAssunto() {
        assertEquals(AssistenteIntencao.SAUDACAO, principal("Olá, bom dia"));
        assertEquals(AssistenteIntencao.AGRADECIMENTO, principal("Obrigado!"));
        assertFalse(AssistenteIntencoes.detectar("Em qual página fica isso?").contains(AssistenteIntencao.PAGAMENTO));
    }

    @Test
    void acoesLevamParaATelaCerta() {
        List<Acao> acoes = AssistenteIntencoes.acoes("Quais empresas estão com obrigações vencidas?",
                AssistenteIntencoes.detectar("Quais empresas estão com obrigações vencidas?"), true);
        assertEquals("/obrigacoes-pendentes", acoes.get(0).rota());
        assertEquals(Map.of("filtro", "vencidas"), acoes.get(0).queryParams());
        assertTrue(acoes.size() <= 3);

        List<Acao> dec = AssistenteIntencoes.acoes("Tenho comunicação da SEFAZ?",
                AssistenteIntencoes.detectar("Tenho comunicação da SEFAZ?"), false);
        assertEquals("/dec", dec.get(0).rota());
    }

    @Test
    void clienteVeDocumentosAEnviarEPassoAPasso() {
        AssistenteContexto ctx = cliente();
        String r = AssistenteRespostaAutomatica.responder("O que eu preciso enviar este mês?", ctx);
        assertTrue(r.contains("Extrato bancário"), r);
        assertTrue(r.contains("A enviar"), r);
        assertFalse(r.contains("DAS"), "DAS é do escritório, não é documento a enviar: " + r);
    }

    @Test
    void escritorioVeVencidasAgrupadasPorEmpresa() {
        AssistenteContexto ctx = new AssistenteContexto();
        ctx.escritorio = true;
        ctx.hoje = HOJE;
        ctx.totalEmpresas = 2;
        ctx.abertas.add(obrigacao(1L, "Padaria Pão Quente", "DAS", -3, StatusObrigacao.VENCIDA, ResponsavelObrigacao.ESCRITORIO));
        ctx.abertas.add(obrigacao(2L, "Auto Peças", "FGTS", 2, StatusObrigacao.PENDENTE, ResponsavelObrigacao.ESCRITORIO));
        ctx.ordenar();
        String r = AssistenteRespostaAutomatica.responder("Quais empresas estão com obrigações vencidas?", ctx);
        assertTrue(r.contains("Padaria Pão Quente"), r);
        assertFalse(r.contains("Auto Peças"), r);
    }

    @Test
    void decListaComunicacoesSemCiencia() {
        AssistenteContexto ctx = cliente();
        String r = AssistenteRespostaAutomatica.responder("Tenho comunicação nova da SEFAZ?", ctx);
        assertTrue(r.contains("Intimação"), r);
        assertTrue(r.contains("ciência tácita"), r);
    }

    @Test
    void textoDoContextoRespeitaOLimite() {
        AssistenteContexto ctx = new AssistenteContexto();
        ctx.escritorio = true;
        ctx.hoje = HOJE;
        for (int i = 0; i < 400; i++) {
            ctx.abertas.add(obrigacao((long) i, "Empresa com nome bem comprido número " + i, "Obrigação mensal " + i, -i, StatusObrigacao.VENCIDA, ResponsavelObrigacao.CLIENTE));
        }
        ctx.ordenar();
        assertTrue(ctx.texto().length() <= AssistenteContexto.LIMITE_TEXTO);
    }

    @Test
    void limitadorBloqueiaDepoisDoMaximo() {
        AssistenteLimitador l = new AssistenteLimitador();
        for (int i = 0; i < AssistenteLimitador.MAXIMO; i++) assertTrue(l.permitir("u1", 1000));
        assertFalse(l.permitir("u1", 1000));
        assertTrue(l.permitir("u2", 1000));
        assertTrue(l.permitir("u1", 1000 + 61_000));
    }

    // ------------------------------------------------------------------ dados

    private static AssistenteIntencao principal(String msg) {
        return AssistenteIntencoes.principal(AssistenteIntencoes.detectar(msg));
    }

    private static AssistenteContexto cliente() {
        AssistenteContexto ctx = new AssistenteContexto();
        ctx.escritorio = false;
        ctx.hoje = HOJE;
        ctx.nomeUsuario = "Maria Souza";
        ctx.nomeEmpresa = "Padaria Pão Quente";
        ctx.idEmpresa = 1L;
        ctx.abertas.add(obrigacao(1L, "Padaria Pão Quente", "Extrato bancário", 5, StatusObrigacao.PENDENTE, ResponsavelObrigacao.CLIENTE));
        ctx.abertas.add(obrigacao(1L, "Padaria Pão Quente", "DAS", 15, StatusObrigacao.PENDENTE, ResponsavelObrigacao.ESCRITORIO));
        ctx.decSemCiencia.add(new ComunicacaoDecResponseDTO(1L, "x", 1L, "Padaria Pão Quente", "00000000000100", "Padaria", "123",
                "INTIMACAO", "Divergência no ICMS", null, "SEFAZ", HOJE.minusDays(2), null, null, null, null, "NOVA", "ALTA",
                null, 3L, HOJE.plusDays(3), null, false, false, null, null));
        ctx.ordenar();
        return ctx;
    }

    private static ObrigacaoPendenteResponseDTO obrigacao(Long idEmpresa, String empresa, String nome, long dias,
            StatusObrigacao status, ResponsavelObrigacao resp) {
        return new ObrigacaoPendenteResponseDTO(idEmpresa * 100 + dias, idEmpresa, 1L, nome, HOJE.plusDays(dias), null, status,
                dias, empresa, "09/2026", resp, 0, null, "NAO_SE_APLICA", null);
    }
}
