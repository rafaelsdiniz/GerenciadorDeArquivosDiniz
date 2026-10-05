package diniz.contabilidade.arquivos.assistente;

import static diniz.contabilidade.arquivos.assistente.AssistenteContexto.corta;
import static diniz.contabilidade.arquivos.assistente.AssistenteContexto.data;
import static diniz.contabilidade.arquivos.assistente.AssistenteContexto.nomeObrigacao;
import static diniz.contabilidade.arquivos.assistente.AssistenteContexto.relativo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import diniz.contabilidade.arquivos.dto.response.CertidaoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;

/**
 * Respostas por regras, usadas quando a IA não está configurada ou não respondeu a tempo.
 * Usa os mesmos dados do contexto (já filtrados por empresa), então continua útil numa
 * demonstração sem chave de API. Texto em "markdown leve": **negrito**, "- " e "1. ".
 */
public final class AssistenteRespostaAutomatica {

    private static final int MAX_ITENS = 6;

    private AssistenteRespostaAutomatica() {
    }

    public static String responder(String mensagem, AssistenteContexto ctx) {
        // sem linhas em branco repetidas
        return montar(mensagem, ctx).replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String montar(String mensagem, AssistenteContexto ctx) {
        Set<AssistenteIntencao> intencoes = AssistenteIntencoes.detectar(mensagem);
        AssistenteIntencao principal = AssistenteIntencoes.principal(intencoes);
        boolean como = AssistenteIntencoes.pedeComoFazer(mensagem);
        String t = AssistenteIntencoes.normalizar(mensagem);

        // escritório perguntando de uma empresa específica ("como está a Padaria Pão Quente?")
        Map.Entry<Long, String> empresa = ctx.empresaCitada(mensagem);
        if (empresa != null) {
            AssistenteContexto recorte = ctx.daEmpresa(empresa.getKey(), empresa.getValue());
            if (principal == null || principal == AssistenteIntencao.RESUMO || principal == AssistenteIntencao.SAUDACAO) {
                return sobreEmpresa(recorte);
            }
            ctx = recorte;
        }

        if (principal == null) {
            return como ? ajuda(ctx) : naoEntendi(ctx);
        }
        return switch (principal) {
            case AGRADECIMENTO -> "Por nada! Se precisar de mais alguma coisa, é só perguntar.";
            case SAUDACAO -> saudacao(ctx);
            case CERTIDOES -> certidoes(ctx);
            case DEC -> dec(ctx, como);
            case VENCIDAS -> vencidas(ctx);
            case ANEXAR_GUIA -> anexarGuia(ctx);
            case PAGAMENTO -> pagamento(ctx, como);
            case ENVIAR -> enviar(ctx, como);
            case RESUMO -> resumo(ctx);
            case PRAZOS -> prazos(ctx, t);
            case ARQUIVOS -> arquivos(ctx);
            case CALENDARIO -> calendario(ctx);
            case RELATORIOS -> relatorios(ctx);
            case MENSAGENS -> mensagens(ctx);
            case CONTA -> conta();
            case AJUDA -> ajuda(ctx);
        };
    }

    // ------------------------------------------------------------------ assuntos

    static String saudacao(AssistenteContexto ctx) {
        String nome = ctx.primeiroNome();
        return "Olá" + (nome.isEmpty() ? "" : ", " + nome) + "! Sou o **Assistente Diniz**.\n\n" + destaque(ctx)
                + "\n\nPergunte, por exemplo, " + (ctx.escritorio
                        ? "\"Quais empresas estão com obrigações vencidas?\" ou \"Resuma a situação da carteira hoje\"."
                        : "\"O que eu preciso enviar este mês?\" ou \"Quais guias vencem esta semana?\".");
    }

    static String vencidas(AssistenteContexto ctx) {
        List<ObrigacaoPendenteResponseDTO> vencidas = ctx.vencidas();
        if (vencidas.isEmpty()) {
            String r = ctx.escritorio && ctx.nomeEmpresa == null
                    ? "Boa notícia: **nenhuma obrigação vencida** na carteira hoje (" + data(ctx.hoje) + ")."
                    : "Boa notícia: **nenhuma obrigação vencida**" + deEmpresa(ctx) + ".";
            return r + proximoVencimento(ctx);
        }
        StringBuilder sb = new StringBuilder();
        if (ctx.escritorio && ctx.nomeEmpresa == null) {
            Map<String, List<ObrigacaoPendenteResponseDTO>> porEmpresa = agrupar(vencidas);
            sb.append("**").append(vencidas.size()).append(plural(vencidas.size(), " obrigação vencida", " obrigações vencidas"))
              .append(" em ").append(porEmpresa.size()).append(plural(porEmpresa.size(), " empresa", " empresas")).append(":**\n");
            int n = 0;
            for (Map.Entry<String, List<ObrigacaoPendenteResponseDTO>> e : porEmpresa.entrySet()) {
                if (n++ == 8) {
                    sb.append("- … e mais ").append(porEmpresa.size() - 8).append(" empresa(s)\n");
                    break;
                }
                ObrigacaoPendenteResponseDTO maisAntiga = e.getValue().get(0);
                sb.append("- **").append(e.getKey()).append("** — ").append(e.getValue().size())
                  .append(plural(e.getValue().size(), " vencida", " vencidas")).append(" (mais antiga: ")
                  .append(nomeObrigacao(maisAntiga)).append(", venceu em ").append(data(maisAntiga.dataVencimento())).append(")\n");
            }
            sb.append("\nNa tela **Pendências**, filtro **Vencidas**, você prorroga o vencimento ou anexa a guia de cada uma.");
        } else {
            sb.append("**").append(vencidas.size()).append(plural(vencidas.size(), " obrigação vencida", " obrigações vencidas"))
              .append(deEmpresa(ctx)).append(":**\n");
            lista(sb, vencidas, false);
            boolean temDoCliente = vencidas.stream().anyMatch(AssistenteContexto::doCliente);
            if (ctx.escritorio) {
                sb.append("\nEm **Pendências** você prorroga o vencimento ou anexa a guia de cada uma.");
            } else if (temDoCliente) {
                sb.append("\nOs itens de **documento** dependem de você: envie-os o quanto antes em **Pendências** → filtro **A enviar**. "
                        + "Para as guias do escritório, em caso de dúvida fale com o escritório pela conversa da pendência.");
            } else {
                sb.append("\nEssas obrigações são preparadas pelo escritório. Em caso de dúvida, fale com o escritório pela conversa da pendência.");
            }
        }
        return sb.toString();
    }

    static String prazos(AssistenteContexto ctx, String t) {
        int dias = 7;
        String janela = "nos próximos 7 dias";
        if (t.matches(".*\\bhoje\\b.*") && !t.contains("semana")) { dias = 0; janela = "hoje"; }
        else if (t.contains("amanha")) { dias = 1; janela = "até amanhã"; }
        else if (t.matches(".*\\b15\\b.*") || t.contains("quinze") || t.contains("quinzena")) { dias = 15; janela = "nos próximos 15 dias"; }
        else if (t.matches(".*\\b(mes|30)\\b.*")) { dias = 30; janela = "nos próximos 30 dias"; }
        boolean soGuias = t.matches(".*\\bguias?\\b.*");

        final int janelaDias = dias;
        // guias já entregues pelo escritório e ainda não pagas também "vencem" na janela
        List<ObrigacaoPendenteResponseDTO> proximas = java.util.stream.Stream.concat(
                        ctx.proximas(dias).stream().filter(o -> !soGuias || !AssistenteContexto.doCliente(o)),
                        ctx.aPagar.stream().filter(o -> o.diasParaVencer() != null && o.diasParaVencer() >= 0 && o.diasParaVencer() <= janelaDias))
                .sorted(java.util.Comparator.comparing(ObrigacaoPendenteResponseDTO::dataVencimento,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        String oque = soGuias ? "guia" : "vencimento";
        StringBuilder sb = new StringBuilder();
        if (proximas.isEmpty()) {
            sb.append(soGuias ? "Nenhuma guia" : "Nenhuma obrigação").append(deEmpresa(ctx)).append(" vence ").append(janela).append(".");
            sb.append(proximoVencimento(ctx, soGuias));
        } else {
            sb.append("**").append(proximas.size()).append(plural(proximas.size(), " " + oque, " " + oque + "s"))
              .append(deEmpresa(ctx)).append(" ").append(janela).append(":**\n");
            proximas.stream().limit(MAX_ITENS).forEach(o -> sb.append(item(o, ctx.escritorio && ctx.nomeEmpresa == null))
                    .append(o.status() == StatusObrigacao.ENTREGUE ? " · **guia disponível**, aguardando pagamento" : "").append("\n"));
            if (proximas.size() > MAX_ITENS) sb.append("- … e mais ").append(proximas.size() - MAX_ITENS).append("\n");
            if (!ctx.escritorio && proximas.stream().anyMatch(o -> o.status() == StatusObrigacao.ENTREGUE)) {
                sb.append("\nAs guias disponíveis podem ser baixadas e pagas em **Pendências** → **A pagar**.");
            }
            if (!ctx.escritorio && proximas.stream().anyMatch(o -> o.status() != StatusObrigacao.ENTREGUE && AssistenteContexto.doCliente(o))) {
                sb.append("\nOs itens de documento dependem de você: envie em **Pendências** → **A enviar**.");
            }
        }
        int vencidas = ctx.vencidas().size();
        if (vencidas > 0) {
            sb.append("\n\n**Atenção:** há também ").append(vencidas).append(plural(vencidas, " obrigação vencida", " obrigações vencidas")).append(".");
        }
        return sb.toString();
    }

    static String enviar(AssistenteContexto ctx, boolean como) {
        List<ObrigacaoPendenteResponseDTO> enviar = ctx.aEnviar();
        StringBuilder sb = new StringBuilder();
        if (ctx.escritorio && ctx.nomeEmpresa == null) {
            if (enviar.isEmpty()) return "Nenhum documento aguardando os clientes no momento.";
            Map<String, List<ObrigacaoPendenteResponseDTO>> porEmpresa = agrupar(enviar);
            sb.append("**").append(enviar.size()).append(plural(enviar.size(), " documento aguardando", " documentos aguardando"))
              .append(" os clientes:**\n");
            porEmpresa.entrySet().stream().limit(8).forEach(e -> {
                long atrasados = e.getValue().stream().filter(AssistenteContexto::vencida).count();
                sb.append("- **").append(e.getKey()).append("** — ").append(e.getValue().size())
                  .append(atrasados > 0 ? " (" + atrasados + " em atraso)" : "").append("\n");
            });
            sb.append("\nO cliente envia pela tela **Pendências** (filtro **A enviar**) e o item fica entregue automaticamente.");
            return sb.toString();
        }
        if (enviar.isEmpty()) {
            sb.append("Você não tem **nenhum documento pendente de envio**").append(deEmpresa(ctx)).append(". Tudo em dia!");
        } else {
            sb.append("**Documentos que o escritório está aguardando (").append(enviar.size()).append("):**\n");
            lista(sb, enviar, false);
        }
        sb.append("\n\n**Como enviar:**\n")
          .append("1. Abra **Pendências** e use o filtro **A enviar**.\n")
          .append("2. Clique no item (ex.: extrato bancário) e escolha **Enviar documento**.\n")
          .append("3. Selecione ou arraste o arquivo. Ele vai para a pasta certa no Drive e o item fica como **entregue**.");
        return sb.toString();
    }

    static String pagamento(AssistenteContexto ctx, boolean como) {
        List<ObrigacaoPendenteResponseDTO> aPagar = ctx.aPagar;
        StringBuilder sb = new StringBuilder();
        boolean carteira = ctx.escritorio && ctx.nomeEmpresa == null;
        if (aPagar.isEmpty()) {
            sb.append(carteira ? "Nenhuma guia aguardando confirmação de pagamento na carteira."
                    : "Nenhuma guia aguardando confirmação de pagamento" + deEmpresa(ctx) + ".");
        } else {
            long atrasadas = aPagar.stream().filter(o -> "ATRASADO".equals(o.situacaoPagamento())).count();
            sb.append("**").append(aPagar.size()).append(plural(aPagar.size(), " guia aguardando", " guias aguardando"))
              .append(" confirmação de pagamento").append(atrasadas > 0 ? " — " + atrasadas + " com vencimento já passado" : "")
              .append(":**\n");
            lista(sb, aPagar, carteira);
        }
        if (ctx.escritorio) {
            sb.append("\n\nO cliente confirma o pagamento na própria pendência, informando a data e anexando o comprovante. "
                    + "O escritório também pode confirmar ou desfazer a confirmação nos detalhes da obrigação.");
        } else {
            sb.append("\n\n**Como confirmar o pagamento de uma guia:**\n")
              .append("1. Abra **Pendências** e use o filtro **A pagar**.\n")
              .append("2. Clique na guia para ver os detalhes e **baixar o PDF**.\n")
              .append("3. Depois de pagar, clique em **Confirmar pagamento**, informe a data e anexe o comprovante.");
        }
        return sb.toString();
    }

    static String dec(AssistenteContexto ctx, boolean como) {
        List<ComunicacaoDecResponseDTO> lista = ctx.decSemCiencia;
        boolean carteira = ctx.escritorio && ctx.nomeEmpresa == null;
        StringBuilder sb = new StringBuilder();
        if (lista.isEmpty()) {
            sb.append("Nenhuma comunicação do **DEC (SEFAZ-TO)** pendente de ciência").append(deEmpresa(ctx)).append(". Tudo tranquilo por aqui.");
        } else {
            int atencao = ctx.decComAtencao().size();
            sb.append("**").append(lista.size()).append(plural(lista.size(), " comunicação", " comunicações"))
              .append(" do DEC sem ciência").append(deEmpresa(ctx)).append("**")
              .append(atencao > 0 ? " — **" + atencao + plural(atencao, " exige", " exigem") + " atenção**" : "").append(":\n");
            lista.stream().limit(MAX_ITENS).forEach(c -> {
                sb.append("- **").append(AssistenteContexto.rotuloTipoDec(c.tipo())).append(c.numero() != null ? " nº " + c.numero() : "")
                  .append("** — ").append(corta(c.assunto(), 90)).append(" (urgência ").append(AssistenteContexto.rotuloUrgencia(c.urgencia())).append(")");
                if (c.cienciaTacitaEm() != null) {
                    sb.append(" · ciência tácita em ").append(data(c.cienciaTacitaEm())).append(" (").append(relativo(c.diasRestantes())).append(")");
                }
                if (carteira) sb.append(" · ").append(AssistenteContexto.nomeEmpresaDec(c));
                sb.append("\n");
            });
            if (lista.size() > MAX_ITENS) sb.append("- … e mais ").append(lista.size() - MAX_ITENS).append("\n");
            sb.append("\nA **ciência tácita** acontece sozinha na data indicada, mesmo que ninguém abra a comunicação, e a partir dela começam a correr os prazos.");
        }
        sb.append(ctx.escritorio
                ? "\n\nNa tela **Comunicações DEC** você vê o inteiro teor e filtra as que exigem atenção."
                : "\n\nO escritório acompanha essas comunicações com você. Se houver intimação ou cobrança, fale com o escritório antes do prazo.");
        return sb.toString();
    }

    static String certidoes(AssistenteContexto ctx) {
        List<CertidaoResponseDTO> lista = ctx.certidoesAlerta;
        boolean carteira = ctx.escritorio && ctx.nomeEmpresa == null;
        StringBuilder sb = new StringBuilder();
        if (lista.isEmpty()) {
            sb.append("Nenhuma certidão vencida ou vencendo nos próximos 15 dias").append(deEmpresa(ctx)).append(".");
        } else {
            long vencidas = lista.stream().filter(c -> "VENCIDA".equals(c.statusValidade())).count();
            sb.append("**").append(lista.size()).append(plural(lista.size(), " certidão", " certidões")).append(" em alerta")
              .append(deEmpresa(ctx)).append("**").append(vencidas > 0 ? " (" + vencidas + plural((int) vencidas, " vencida", " vencidas") + ")" : "")
              .append(":\n");
            lista.stream().limit(MAX_ITENS + 2).forEach(c -> sb.append("- **").append(c.tipoRotulo()).append("** — ")
                    .append("VENCIDA".equals(c.statusValidade()) ? "venceu em " : "vence em ").append(data(c.dataValidade()))
                    .append(" (").append(relativo(c.diasParaVencer())).append(")")
                    .append(carteira ? " · " + c.nomeEmpresa() : "").append("\n"));
            if (lista.size() > MAX_ITENS + 2) sb.append("- … e mais ").append(lista.size() - MAX_ITENS - 2).append("\n");
        }
        sb.append(ctx.escritorio
                ? "\n\nNa tela **Certidões** você registra a nova emissão e anexa o PDF."
                : "\n\nO escritório cuida da renovação das certidões; a versão atual fica disponível na tela **Certidões**.");
        return sb.toString();
    }

    static String resumo(AssistenteContexto ctx) {
        if (ctx.escritorio && ctx.nomeEmpresa == null) {
            StringBuilder sb = new StringBuilder("**Situação da carteira em ").append(data(ctx.hoje)).append("** (")
                    .append(ctx.totalEmpresas).append(plural((int) ctx.totalEmpresas, " empresa", " empresas")).append("):\n");
            List<AssistenteContexto.ResumoEmpresa> alerta = ctx.empresasComAlerta();
            long comVencidas = ctx.empresas.stream().filter(e -> e.vencidas() > 0).count();
            int venc = ctx.vencidas().size();
            sb.append("- **").append(venc).append("** ").append(plural(venc, "obrigação vencida", "obrigações vencidas"))
              .append(venc > 0 ? " em **" + comVencidas + "** " + plural((int) comVencidas, "empresa", "empresas") : "").append("\n")
              .append(qtd(ctx.proximas(7).size(), "vencimento nos próximos 7 dias", "vencimentos nos próximos 7 dias"))
              .append(qtd(ctx.aEnviar().size(), "documento aguardando os clientes", "documentos aguardando os clientes"))
              .append(qtd(ctx.aPagar.size(), "guia aguardando confirmação de pagamento", "guias aguardando confirmação de pagamento"))
              .append(qtd(ctx.decComAtencao().size(), "comunicação do DEC exigindo atenção", "comunicações do DEC exigindo atenção"))
              .append(qtd(ctx.certidoesAlerta.size(), "certidão vencendo ou vencida", "certidões vencendo ou vencidas"));
            if (!alerta.isEmpty()) {
                sb.append("\n**Empresas que exigem atenção primeiro:**\n");
                alerta.stream().limit(5).forEach(e -> sb.append("- **").append(e.nome()).append("** — ").append(resumoLinha(e)).append("\n"));
            } else {
                sb.append("\nNenhuma empresa com pendência urgente. Carteira em dia!");
            }
            return sb.toString().trim();
        }
        return sobreEmpresa(ctx);
    }

    /** Resumo de uma empresa (cliente: a própria; escritório: a citada). */
    static String sobreEmpresa(AssistenteContexto ctx) {
        StringBuilder sb = new StringBuilder("**Resumo").append(deEmpresa(ctx)).append(" em ").append(data(ctx.hoje)).append(":**\n")
                .append(qtd(ctx.vencidas().size(), "obrigação vencida", "obrigações vencidas"))
                .append(qtd(ctx.proximas(7).size(), "vencimento nos próximos 7 dias", "vencimentos nos próximos 7 dias"))
                .append(ctx.escritorio
                        ? qtd(ctx.aEnviar().size(), "documento aguardando o cliente", "documentos aguardando o cliente")
                        : qtd(ctx.aEnviar().size(), "documento para enviar", "documentos para enviar"))
                .append(qtd(ctx.aPagar.size(), "guia aguardando confirmação de pagamento", "guias aguardando confirmação de pagamento"))
                .append(qtd(ctx.decSemCiencia.size(), "comunicação do DEC sem ciência", "comunicações do DEC sem ciência"))
                .append(qtd(ctx.certidoesAlerta.size(), "certidão vencendo ou vencida", "certidões vencendo ou vencidas"));
        if (!ctx.escritorio) sb.append(qtd((int) ctx.totalArquivos, "arquivo no Drive", "arquivos no Drive"));
        String prioridade = prioridade(ctx);
        if (prioridade != null) sb.append("\n**Prioridade agora:** ").append(prioridade);
        else sb.append("\nTudo em dia, sem pendências urgentes.");
        return sb.toString().trim();
    }

    static String arquivos(AssistenteContexto ctx) {
        String qtd = ctx.escritorio
                ? "Há **" + ctx.totalArquivos + "** arquivos no Drive das empresas."
                : "A " + nomeOuSua(ctx) + " tem **" + ctx.totalArquivos + "** arquivos no Drive.";
        return qtd + "\n\nNa tela **Arquivos** você navega pelas pastas" + (ctx.escritorio ? " de cada empresa" : " da empresa")
                + ", no estilo Drive:\n"
                + "- envie ou arraste arquivos para a pasta aberta;\n"
                + "- crie pastas, mova, renomeie e baixe arquivos;\n"
                + "- alterne entre lista e grade e use a busca;\n"
                + "- itens excluídos vão para a **Lixeira** e podem ser restaurados.";
    }

    static String calendario(AssistenteContexto ctx) {
        StringBuilder sb = new StringBuilder("O **Calendário** mostra os vencimentos do mês dia a dia, coloridos por situação. "
                + "Alterne entre **Mês** e **Lista** e clique num dia para ver as obrigações.");
        List<ObrigacaoPendenteResponseDTO> prox = ctx.proximas(30);
        if (!prox.isEmpty()) {
            sb.append("\n\n**Próximos vencimentos:**\n");
            prox.stream().limit(3).forEach(o -> sb.append(item(o, ctx.escritorio && ctx.nomeEmpresa == null)).append("\n"));
        }
        return sb.toString().trim();
    }

    static String relatorios(AssistenteContexto ctx) {
        return "Na tela **Relatórios** você escolhe o tipo, ajusta a competência e depois **imprime, salva em PDF** ou **exporta para planilha (CSV)**:\n"
                + "- **Relatório mensal da empresa** — obrigações, documentos, certidões e DEC da competência;\n"
                + (ctx.escritorio ? "- **Relatório da carteira** — todas as empresas lado a lado, com ranking de atenção;\n" : "")
                + "- **Pendências em aberto** — tudo o que ainda falta entregar, por vencimento.\n\n"
                + "Para PDF, clique em **Imprimir** e escolha \"Salvar como PDF\".";
    }

    static String mensagens(AssistenteContexto ctx) {
        return "Cada obrigação tem uma **conversa** entre o escritório e o cliente:\n"
                + "1. Abra **Pendências** e clique na obrigação.\n"
                + "2. No painel de detalhes, use a conversa para escrever (Enter envia, Shift+Enter quebra a linha).\n"
                + "3. Mensagens novas aparecem no sino do topo e no filtro **Mensagens** das pendências.";
    }

    static String anexarGuia(AssistenteContexto ctx) {
        if (!ctx.escritorio) {
            int n = ctx.aPagar.size();
            return "Quem prepara e anexa as guias é o **escritório**. Assim que uma guia é anexada, ela aparece em **Pendências** → filtro **A pagar**, "
                    + "com o PDF para baixar." + (n > 0 ? " Hoje há **" + n + plural(n, " guia disponível", " guias disponíveis") + "** para pagar." : "")
                    + "\n\nSe precisar mudar um vencimento, fale com o escritório pela conversa da pendência.";
        }
        return "**Como anexar uma guia:**\n"
                + "1. Abra **Pendências** e localize a obrigação (use a busca ou o filtro da empresa).\n"
                + "2. Na linha, clique em **Anexar guia** e escolha o PDF.\n"
                + "3. A obrigação passa a **Entregue** e o cliente vê a guia no filtro **A pagar** para baixar e confirmar o pagamento.\n\n"
                + "**Para prorrogar um vencimento:** na mesma linha, use o ícone de calendário e informe a nova data.";
    }

    static String conta() {
        return "Em **Minha conta** (menu do seu avatar, no canto superior direito) você vê seus dados de usuário e pode **trocar a senha**. "
                + "Para sair ou trocar de perfil, use **Sair** no mesmo menu.";
    }

    static String ajuda(AssistenteContexto ctx) {
        StringBuilder sb = new StringBuilder("Posso responder com os **dados da sua conta** e explicar como usar o portal. Exemplos:\n");
        if (ctx.escritorio) {
            sb.append("- \"Quais empresas estão com obrigações vencidas?\"\n")
              .append("- \"Resuma a situação da carteira hoje\"\n")
              .append("- \"Quais comunicações do DEC exigem atenção?\"\n")
              .append("- \"Como está a [nome da empresa]?\"\n");
        } else {
            sb.append("- \"O que eu preciso enviar este mês?\"\n")
              .append("- \"Quais guias vencem esta semana?\"\n")
              .append("- \"Como confirmo o pagamento de uma guia?\"\n")
              .append("- \"Tenho comunicação nova da SEFAZ?\"\n");
        }
        sb.append("\n**Telas do portal:** Painel, Pendências, Calendário, Arquivos, Comunicações DEC, Certidões, Relatórios, Minha conta e Como usar.");
        return sb.toString();
    }

    static String naoEntendi(AssistenteContexto ctx) {
        return "Não encontrei uma resposta específica para isso nos dados da sua conta.\n\n" + destaque(ctx)
                + "\n\nPosso ajudar com **vencimentos**, **documentos a enviar**, **guias a pagar**, **comunicações do DEC**, "
                + "**certidões** ou explicar **como usar** alguma tela. Para assuntos fora do portal, fale com o escritório.";
    }

    // ------------------------------------------------------------------ apoio

    /** Uma frase com o que mais importa agora. */
    static String destaque(AssistenteContexto ctx) {
        int vencidas = ctx.vencidas().size();
        int semana = ctx.proximas(7).size();
        int dec = ctx.decComAtencao().size();
        if (ctx.escritorio && ctx.nomeEmpresa == null) {
            long empresas = ctx.empresas.stream().filter(e -> e.vencidas() > 0).count();
            if (vencidas + semana + dec == 0) return "Hoje (" + data(ctx.hoje) + ") a carteira está em dia: nada vencido nem vencendo na semana.";
            List<String> partes = new ArrayList<>();
            if (vencidas > 0) partes.add("**" + vencidas + plural(vencidas, " obrigação vencida", " obrigações vencidas") + "** em " + empresas + plural((int) empresas, " empresa", " empresas"));
            if (semana > 0) partes.add("**" + semana + plural(semana, " vencimento", " vencimentos") + "** nos próximos 7 dias");
            if (dec > 0) partes.add("**" + dec + plural(dec, " comunicação", " comunicações") + " do DEC** exigindo atenção");
            return "Hoje (" + data(ctx.hoje) + ") a carteira tem " + juntar(partes) + ".";
        }
        int enviar = ctx.aEnviar().size();
        int pagar = ctx.aPagar.size();
        List<String> partes = new ArrayList<>();
        if (vencidas > 0) partes.add("**" + vencidas + plural(vencidas, " obrigação vencida", " obrigações vencidas") + "**");
        if (enviar > 0) partes.add("**" + enviar + plural(enviar, " documento", " documentos") + " para enviar**");
        if (pagar > 0) partes.add("**" + pagar + plural(pagar, " guia aguardando", " guias aguardando") + " pagamento**");
        if (dec > 0) partes.add("**" + dec + plural(dec, " comunicação", " comunicações") + " do DEC** exigindo atenção");
        if (partes.isEmpty()) {
            return "Hoje (" + data(ctx.hoje) + ") está tudo em dia" + deEmpresa(ctx) + ": nenhuma pendência urgente." + proximoVencimento(ctx);
        }
        return "Hoje (" + data(ctx.hoje) + ") " + (ctx.escritorio ? "a " + nomeOuSua(ctx) + " tem " : "você tem ") + juntar(partes) + ".";
    }

    private static String prioridade(AssistenteContexto ctx) {
        List<ComunicacaoDecResponseDTO> dec = ctx.decComAtencao();
        List<ObrigacaoPendenteResponseDTO> vencidas = ctx.vencidas();
        if (!vencidas.isEmpty()) {
            ObrigacaoPendenteResponseDTO o = vencidas.get(0);
            return nomeObrigacao(o) + " (competência " + o.competencia() + ") venceu em " + data(o.dataVencimento())
                    + (AssistenteContexto.doCliente(o) && !ctx.escritorio ? " — envie o documento em Pendências." : ".");
        }
        if (!dec.isEmpty()) {
            ComunicacaoDecResponseDTO c = dec.get(0);
            return "comunicação do DEC \"" + corta(c.assunto(), 70) + "\""
                    + (c.cienciaTacitaEm() != null ? " — ciência tácita em " + data(c.cienciaTacitaEm()) + "." : ".");
        }
        List<ObrigacaoPendenteResponseDTO> semana = ctx.proximas(7);
        if (!semana.isEmpty()) {
            ObrigacaoPendenteResponseDTO o = semana.get(0);
            return nomeObrigacao(o) + " vence em " + data(o.dataVencimento()) + " (" + relativo(o.diasParaVencer()) + ").";
        }
        if (!ctx.aPagar.isEmpty()) {
            ObrigacaoPendenteResponseDTO o = ctx.aPagar.get(0);
            return "confirmar o pagamento da guia " + nomeObrigacao(o) + " (vencimento " + data(o.dataVencimento()) + ").";
        }
        return null;
    }

    private static String proximoVencimento(AssistenteContexto ctx) {
        return proximoVencimento(ctx, false);
    }

    private static String proximoVencimento(AssistenteContexto ctx, boolean soGuias) {
        return ctx.abertas.stream()
                .filter(o -> !AssistenteContexto.vencida(o) && o.diasParaVencer() != null && o.diasParaVencer() >= 0)
                .filter(o -> !soGuias || !AssistenteContexto.doCliente(o))
                .findFirst()
                .map(o -> (soGuias ? " A próxima guia é **" : " O próximo vencimento é **") + nomeObrigacao(o) + "** em " + data(o.dataVencimento())
                        + " (" + relativo(o.diasParaVencer()) + ")"
                        + (ctx.escritorio && ctx.nomeEmpresa == null ? ", da " + o.nomeEmpresa() : "") + ".")
                .orElse("");
    }

    private static void lista(StringBuilder sb, List<ObrigacaoPendenteResponseDTO> itens, boolean comEmpresa) {
        itens.stream().limit(MAX_ITENS).forEach(o -> sb.append(item(o, comEmpresa)).append("\n"));
        if (itens.size() > MAX_ITENS) sb.append("- … e mais ").append(itens.size() - MAX_ITENS).append("\n");
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.setLength(sb.length() - 1);
    }

    private static String item(ObrigacaoPendenteResponseDTO o, boolean comEmpresa) {
        StringBuilder sb = new StringBuilder("- **").append(nomeObrigacao(o)).append("**");
        if (o.competencia() != null) sb.append(" — competência ").append(o.competencia());
        boolean venceu = AssistenteContexto.vencida(o) || (o.status() == StatusObrigacao.ENTREGUE && o.diasParaVencer() != null && o.diasParaVencer() < 0);
        sb.append(" · ").append(venceu ? "venceu em " : "vence em ").append(data(o.dataVencimento()));
        if (o.diasParaVencer() != null) sb.append(" (").append(relativo(o.diasParaVencer())).append(")");
        if (o.valorGuia() != null) sb.append(" · **").append(AssistenteContexto.moeda(o.valorGuia())).append("**");
        if (comEmpresa) sb.append(" · ").append(o.nomeEmpresa());
        return sb.toString();
    }

    private static Map<String, List<ObrigacaoPendenteResponseDTO>> agrupar(List<ObrigacaoPendenteResponseDTO> itens) {
        Map<String, List<ObrigacaoPendenteResponseDTO>> m = new LinkedHashMap<>();
        for (ObrigacaoPendenteResponseDTO o : itens) {
            m.computeIfAbsent(o.nomeEmpresa() == null ? "Empresa" : o.nomeEmpresa(), k -> new ArrayList<>()).add(o);
        }
        return m;
    }

    private static String resumoLinha(AssistenteContexto.ResumoEmpresa e) {
        List<String> p = new ArrayList<>();
        if (e.vencidas() > 0) p.add(e.vencidas() + plural(e.vencidas(), " vencida", " vencidas"));
        if (e.decAtencao() > 0) p.add(e.decAtencao() + " DEC com atenção");
        if (e.certidoesAlerta() > 0) p.add(e.certidoesAlerta() + plural(e.certidoesAlerta(), " certidão", " certidões") + " em alerta");
        if (e.aPagar() > 0) p.add(e.aPagar() + plural(e.aPagar(), " guia a pagar", " guias a pagar"));
        if (e.semana() > 0) p.add(e.semana() + " vencendo na semana");
        if (e.aEnviar() > 0) p.add(e.aEnviar() + plural(e.aEnviar(), " documento aguardado", " documentos aguardados"));
        return String.join(" · ", p);
    }

    private static String deEmpresa(AssistenteContexto ctx) {
        return ctx.nomeEmpresa == null ? "" : " da " + ctx.nomeEmpresa;
    }

    private static String nomeOuSua(AssistenteContexto ctx) {
        return ctx.nomeEmpresa == null ? "sua empresa" : ctx.nomeEmpresa;
    }

    /** Linha "- **n** coisa(s)" com quebra de linha. */
    private static String qtd(int n, String um, String varios) {
        return "- **" + n + "** " + plural(n, um, varios) + "\n";
    }

    static String plural(int n, String um, String varios) {
        return n == 1 ? um : varios;
    }

    private static String juntar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " e " + partes.get(partes.size() - 1);
    }
}
