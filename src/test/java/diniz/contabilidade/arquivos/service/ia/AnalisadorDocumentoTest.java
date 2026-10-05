package diniz.contabilidade.arquivos.service.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import diniz.contabilidade.arquivos.dto.response.DocumentoAnalisado;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/**
 * Núcleo da leitura inteligente: fusão IA + padrões, conferências (alertas) e o cliente do DeepSeek
 * contra um servidor HTTP falso (nunca chama a API real).
 */
@DisplayName("Leitura inteligente — IA, fusão e conferências")
class AnalisadorDocumentoTest {

    static final String CNPJ = "22222222000102";
    static final LocalDate HOJE = LocalDate.of(2026, 10, 5);
    static final ObjectMapper JSON = new ObjectMapper();

    HttpServer servidor;
    final AtomicReference<String> resposta = new AtomicReference<>();
    final AtomicReference<Integer> status = new AtomicReference<>(200);
    final AtomicReference<Integer> atrasoMs = new AtomicReference<>(0);
    final AtomicReference<String> ultimoCorpo = new AtomicReference<>();
    final AtomicReference<String> ultimoAuth = new AtomicReference<>();

    @BeforeEach
    void subirServidorFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/chat/completions", ex -> {
            ultimoAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            ultimoCorpo.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(atrasoMs.get()); } catch (InterruptedException ignored) { }
            byte[] b = (resposta.get() == null ? "{}" : resposta.get()).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status.get(), b.length);
            try (OutputStream out = ex.getResponseBody()) { out.write(b); }
        });
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private DeepSeekCliente cliente(Duration timeout) {
        return new DeepSeekCliente("http://127.0.0.1:" + servidor.getAddress().getPort(), "chave-teste", "deepseek-chat", timeout, JSON);
    }

    /** Resposta no formato da API (OpenAI-compatível) com o JSON do modelo em choices[0].message.content. */
    private void iaResponde(String jsonDoModelo) throws Exception {
        resposta.set(JSON.writeValueAsString(java.util.Map.of("choices",
                List.of(java.util.Map.of("message", java.util.Map.of("role", "assistant", "content", jsonDoModelo))))));
    }

    private static byte[] dasExemplo(LocalDate vencimento) throws IOException {
        return new GeradorExemplos().das(new GeradorExemplos.Dados("Pão Quente Comércio LTDA", CNPJ,
                YearMonth.from(vencimento).minusMonths(1), vencimento, new BigDecimal("1847.32")));
    }

    private static ContextoLeitura ctx(String obrigacao, LocalDate venc, String comp) {
        return new ContextoLeitura(CNPJ, "Padaria Pão Quente", obrigacao, venc, comp);
    }

    // ------------------------------------------------------------------ sem IA

    @Test
    @DisplayName("Guia de exemplo (PDF gerado) é lida por padrões e confere com a obrigação")
    void exemploDasSemIa() throws Exception {
        LocalDate venc = LocalDate.of(2026, 10, 20);
        DocumentoAnalisado r = new AnalisadorDocumento(null, () -> HOJE)
                .analisar(dasExemplo(venc), "guia-das-exemplo.pdf", ctx("DAS — Simples Nacional", venc, "09/2026"));
        assertEquals(TipoDocumento.DAS, r.tipoDocumento());
        assertEquals(CategoriaFiscal.DAS, r.categoriaFiscalSugerida());
        assertEquals(new BigDecimal("1847.32"), r.valor());
        assertEquals(venc, r.vencimento());
        assertEquals("09/2026", r.competencia());
        assertEquals(Boolean.TRUE, r.cnpjConfere());
        assertEquals(48, r.linhaDigitavel().length());
        assertEquals(AnalisadorDocumento.FONTE_PADROES, r.fonte());
        assertTrue(r.textoLegivel());
        assertTrue(r.alertas().isEmpty(), "não deveria haver divergências: " + r.alertas());
    }

    @Test
    @DisplayName("Divergências: guia de DAS anexada no FGTS, vencimento e competência diferentes, CNPJ de outra empresa")
    void divergencias() throws Exception {
        LocalDate venc = LocalDate.of(2026, 10, 20);
        ContextoLeitura outraEmpresa = new ContextoLeitura("33333333000103", "Auto Peças", "FGTS", LocalDate.of(2026, 10, 7), "08/2026");
        DocumentoAnalisado r = new AnalisadorDocumento(null, () -> HOJE).analisar(dasExemplo(venc), "guia.pdf", outraEmpresa);
        List<String> codigos = r.alertas().stream().map(DocumentoAnalisado.Alerta::codigo).toList();
        assertTrue(codigos.contains("TIPO_INCOMPATIVEL"), codigos.toString());
        assertTrue(codigos.contains("VENCIMENTO_DIVERGENTE"), codigos.toString());
        assertTrue(codigos.contains("COMPETENCIA_DIVERGENTE"), codigos.toString());
        assertTrue(codigos.contains("CNPJ_DIVERGENTE"), codigos.toString());
        assertEquals("danger", r.alertas().get(0).nivel(), "alertas graves vêm primeiro");
        assertEquals(Boolean.FALSE, r.cnpjConfere());
        assertEquals(CNPJ, r.cnpj());
    }

    @Test
    @DisplayName("Guia vencida e sem valor geram alertas")
    void vencidaSemValor() {
        DocumentoAnalisado r = new AnalisadorDocumento(null, () -> HOJE).analisarTexto(
                "Documento de Arrecadação do Simples Nacional\nCNPJ 11.222.333/0001-81\nData de vencimento 20/09/2026",
                "PDF", "das.pdf", new ContextoLeitura("11222333000181", "Modelo", null, null, null));
        List<String> codigos = r.alertas().stream().map(DocumentoAnalisado.Alerta::codigo).toList();
        assertTrue(codigos.contains("VENCIDO"));
        assertTrue(codigos.contains("VALOR_AUSENTE"));
        assertEquals(Boolean.TRUE, r.cnpjConfere());
    }

    @Test
    @DisplayName("Imagem / PDF escaneado: resultado claro de documento sem texto (tipo sugerido pelo nome)")
    void semTexto() {
        DocumentoAnalisado r = new AnalisadorDocumento(null, () -> HOJE)
                .analisar(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, "fgts-setembro.png", ContextoLeitura.vazio());
        assertFalse(r.textoLegivel());
        assertEquals("SEM_TEXTO", r.alertas().get(0).codigo());
        assertEquals(TipoDocumento.FGTS, r.tipoDocumento());
        assertNull(r.valor());
    }

    @Test
    @DisplayName("PDF corrompido não quebra a leitura")
    void pdfCorrompido() {
        DocumentoAnalisado r = new AnalisadorDocumento(null, () -> HOJE)
                .analisar("%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8), "documento.pdf", ContextoLeitura.vazio());
        assertFalse(r.textoLegivel());
        assertEquals(AnalisadorDocumento.FONTE_PADROES, r.fonte());
    }

    // ------------------------------------------------------------------ com IA (servidor falso)

    @Test
    @DisplayName("IA: campos válidos vencem; CNPJ inválido e data impossível são descartados; linha com DV vence a IA")
    void fusaoComIa() throws Exception {
        iaResponde("""
                {"tipoDocumento":"DAS","descricaoSugerida":"DAS setembro","cnpj":"99.999.999/0001-00",
                 "razaoSocial":"Pão Quente Comércio LTDA","competencia":"2026-09","vencimento":"2026-02-31",
                 "valor":"1.900,00","linhaDigitavel":"123","numeroDocumento":"07.20.26","confianca":0.9,
                 "observacoes":["guia sem multa e juros"]}
                """);
        LocalDate venc = LocalDate.of(2026, 10, 20);
        DocumentoAnalisado r = new AnalisadorDocumento(cliente(Duration.ofSeconds(5)), () -> HOJE)
                .analisar(dasExemplo(venc), "guia-das-exemplo.pdf", ctx("DAS — Simples Nacional", venc, "09/2026"));

        assertEquals(AnalisadorDocumento.FONTE_IA, r.fonte());
        assertEquals("deepseek-chat", r.modelo());
        assertEquals(CNPJ, r.cnpj(), "CNPJ da IA sem DV válido deve ser descartado");
        assertEquals(venc, r.vencimento(), "data impossível da IA deve ser descartada");
        assertEquals(new BigDecimal("1847.32"), r.valor(), "valor codificado na linha digitável conferida vence a IA");
        assertEquals("09/2026", r.competencia());
        assertEquals("07.20.26", r.numeroDocumento());
        assertTrue(r.alertas().stream().anyMatch(a -> a.codigo().equals("IA_OBSERVACAO")));
        assertEquals("Bearer chave-teste", ultimoAuth.get());
        assertTrue(ultimoCorpo.get().contains("\"response_format\":{\"type\":\"json_object\"}"));
        assertTrue(ultimoCorpo.get().contains("\"temperature\":0"));
    }

    @Test
    @DisplayName("IA: o texto enviado nunca passa de 12 mil caracteres")
    void limiteDeTexto() throws Exception {
        iaResponde("{\"tipoDocumento\":\"OUTRO\"}");
        String enorme = "linha de teste com conteúdo ".repeat(2000);
        new AnalisadorDocumento(cliente(Duration.ofSeconds(5)), () -> HOJE).analisarTexto(enorme, "TEXTO", "a.txt", ContextoLeitura.vazio());
        String enviado = JSON.readTree(ultimoCorpo.get()).path("messages").path(1).path("content").asText();
        assertTrue(enviado.length() < AnalisadorDocumento.MAX_TEXTO_IA + 200, "enviado: " + enviado.length());
    }

    @Test
    @DisplayName("IA fora do ar (HTTP 500): usa leitura por padrões com alerta")
    void iaComErro() throws Exception {
        status.set(500);
        iaResponde("{}");
        LocalDate venc = LocalDate.of(2026, 10, 20);
        DocumentoAnalisado r = new AnalisadorDocumento(cliente(Duration.ofSeconds(5)), () -> HOJE)
                .analisar(dasExemplo(venc), "guia.pdf", ctx("DAS — Simples Nacional", venc, "09/2026"));
        assertEquals(AnalisadorDocumento.FONTE_PADROES, r.fonte());
        assertEquals(new BigDecimal("1847.32"), r.valor());
        assertTrue(r.alertas().stream().anyMatch(a -> a.codigo().equals("IA_INDISPONIVEL")));
    }

    @Test
    @DisplayName("IA lenta (tempo esgotado) e resposta sem JSON: usa padrões")
    void iaLentaOuInvalida() throws Exception {
        atrasoMs.set(1500);
        iaResponde("{\"tipoDocumento\":\"DAS\"}");
        DocumentoAnalisado lenta = new AnalisadorDocumento(cliente(Duration.ofMillis(300)), () -> HOJE)
                .analisarTexto("Guia DARF valor total R$ 10,00", "TEXTO", "a.txt", ContextoLeitura.vazio());
        assertEquals(AnalisadorDocumento.FONTE_PADROES, lenta.fonte());

        atrasoMs.set(0);
        resposta.set("{\"choices\":[{\"message\":{\"content\":\"não sei responder\"}}]}");
        DocumentoAnalisado invalida = new AnalisadorDocumento(cliente(Duration.ofSeconds(5)), () -> HOJE)
                .analisarTexto("Guia DARF valor total R$ 10,00", "TEXTO", "a.txt", ContextoLeitura.vazio());
        assertEquals(AnalisadorDocumento.FONTE_PADROES, invalida.fonte());
        assertEquals(TipoDocumento.DARF, invalida.tipoDocumento());
        assertEquals(new BigDecimal("10.00"), invalida.valor());
    }

    @Test
    @DisplayName("IA: tolera resposta em bloco ```json e preenche o que os padrões não acharam")
    void iaCompletaPadroes() throws Exception {
        iaResponde("```json\n{\"tipoDocumento\":\"CERTIDAO\",\"cnpj\":\"11222333000181\",\"validade\":\"2026-12-31\","
                + "\"razaoSocial\":\"Modelo LTDA\",\"descricaoSugerida\":\"CND federal\"}\n```");
        DocumentoAnalisado r = new AnalisadorDocumento(cliente(Duration.ofSeconds(5)), () -> HOJE).analisarTexto(
                "Certidão de débitos — documento emitido pela internet", "PDF", "cnd.pdf",
                new ContextoLeitura("11222333000181", "Modelo", null, null, null));
        assertEquals(TipoDocumento.CERTIDAO, r.tipoDocumento());
        assertEquals(LocalDate.of(2026, 12, 31), r.validade());
        assertEquals("CND federal", r.descricaoSugerida());
        assertEquals(Boolean.TRUE, r.cnpjConfere());
    }

    @Test
    @DisplayName("Obrigação → tipos esperados")
    void tiposEsperados() {
        assertEquals(java.util.EnumSet.of(TipoDocumento.DAS), AnalisadorDocumento.tiposEsperados("DAS — Simples Nacional"));
        assertEquals(java.util.EnumSet.of(TipoDocumento.FGTS), AnalisadorDocumento.tiposEsperados("FGTS"));
        assertTrue(AnalisadorDocumento.tiposEsperados("INSS Obra").contains(TipoDocumento.GPS));
        assertTrue(AnalisadorDocumento.tiposEsperados("Notas fiscais de entrada").contains(TipoDocumento.NFE));
        assertTrue(AnalisadorDocumento.tiposEsperados("Folha de ponto").isEmpty());
    }
}
