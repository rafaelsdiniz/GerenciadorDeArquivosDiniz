package diniz.contabilidade.arquivos.service.ia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import diniz.contabilidade.arquivos.dto.response.DocumentoAnalisado;
import diniz.contabilidade.arquivos.dto.response.DocumentoAnalisado.Alerta;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/**
 * Núcleo da leitura inteligente (sem dependência de CDI/banco, para ser testado isoladamente):
 *
 *   texto extraído → leitura por padrões (sempre) → IA, se configurada (12 mil caracteres no máximo)
 *   → fusão validada (IA vence quando o dado é válido; linha digitável com DV correto vence a IA)
 *   → conferências contra a empresa/obrigação (alertas).
 */
public class AnalisadorDocumento {

    private static final Logger LOG = Logger.getLogger(AnalisadorDocumento.class);
    static final int MAX_TEXTO_IA = 12_000;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final String FONTE_IA = "IA";
    public static final String FONTE_PADROES = "PADROES";

    static final String SISTEMA = """
            Você é o assistente de leitura de documentos de um escritório de contabilidade brasileiro.
            Leia o TEXTO extraído de um documento (guia de imposto, nota fiscal, extrato, certidão etc.) e responda
            SOMENTE com um objeto JSON com exatamente os campos abaixo. Use null quando a informação não estiver no
            texto — nunca invente, estime ou complete dados que não aparecem.

            Campos:
            - "tipoDocumento": um de DAS, DARF, GPS, FGTS, ICMS, ISS, NFE, NFSE, EXTRATO_BANCARIO, CERTIDAO, CONTRATO,
              BALANCETE, FOLHA, OUTRO. (DAS = Simples Nacional; GPS = guia da previdência/INSS; FGTS = GRF ou GFD do
              FGTS Digital; ICMS = DARE/DAE estadual; ISS = guia municipal de ISS; NFE = DANFE/NF-e; NFSE = nota de serviço)
            - "descricaoSugerida": descrição curta do arquivo em português (até 80 caracteres),
              ex.: "DAS — Simples Nacional — competência 09/2026".
            - "cnpj": CNPJ do contribuinte/empregador da guia (em notas fiscais, o do emitente), somente os 14 dígitos.
            - "razaoSocial": nome empresarial correspondente ao cnpj.
            - "competencia": mês de referência / período de apuração no formato "MM/aaaa".
            - "vencimento": data de vencimento ou "pagar até", formato "aaaa-mm-dd".
            - "valor": valor total a pagar (já com multa e juros, se houver) como número com ponto decimal, ex.: 1847.32.
            - "linhaDigitavel": linha digitável / código de barras numérico, somente dígitos (47 ou 48).
            - "numeroDocumento": número do documento ou da nota.
            - "validade": data de validade (certidões), formato "aaaa-mm-dd".
            - "confianca": número de 0 a 1 com a sua confiança na leitura.
            - "observacoes": lista (no máximo 3) de observações curtas e úteis ao contador, ex.: "guia com multa e juros".
              Lista vazia se não houver nada relevante.
            """;

    private final DeepSeekCliente ia;
    private final Supplier<LocalDate> hoje;

    /** @param ia cliente da IA, ou null para usar somente a leitura por padrões */
    public AnalisadorDocumento(DeepSeekCliente ia) {
        this(ia, LocalDate::now);
    }

    AnalisadorDocumento(DeepSeekCliente ia, Supplier<LocalDate> hoje) {
        this.ia = ia;
        this.hoje = hoje;
    }

    public DocumentoAnalisado analisar(byte[] conteudo, String nomeArquivo, ContextoLeitura ctx) {
        ExtratorTexto.Texto t = ExtratorTexto.extrair(conteudo, nomeArquivo);
        return analisarTexto(t.vazio() ? "" : t.conteudo(), t.formato(), nomeArquivo, ctx);
    }

    public DocumentoAnalisado analisarTexto(String texto, String formato, String nomeArquivo, ContextoLeitura ctx) {
        if (ctx == null) ctx = ContextoLeitura.vazio();
        long inicio = System.currentTimeMillis();
        List<Alerta> alertas = new ArrayList<>();

        if (texto == null || texto.isBlank()) {
            return semTexto(formato, nomeArquivo, ctx, alertas);
        }

        String compacto = compactar(texto);
        Leitura p = LeitorPadroes.ler(compacto, ctx.cnpjEmpresa());
        Leitura final_ = p;
        String fonte = FONTE_PADROES;
        String modelo = null;

        if (ia != null) {
            try {
                String trecho = compacto.length() > MAX_TEXTO_IA ? compacto.substring(0, MAX_TEXTO_IA) : compacto;
                JsonNode json = ia.completarJson(SISTEMA, "Arquivo: " + nomeSeguro(nomeArquivo)
                        + "\n\nTEXTO DO DOCUMENTO:\n\"\"\"\n" + trecho + "\n\"\"\"");
                Leitura daIa = converter(json, ctx.cnpjEmpresa());
                final_ = fundir(p, daIa);
                fonte = FONTE_IA;
                modelo = ia.modelo();
                for (String obs : daIa.observacoes) alertas.add(new Alerta("IA_OBSERVACAO", "info", obs));
            } catch (DeepSeekCliente.FalhaIa e) {
                LOG.warnf("[ia] leitura por IA indisponível (%s) — usando padrões para \"%s\"", e.getMessage(), nomeSeguro(nomeArquivo));
                alertas.add(new Alerta("IA_INDISPONIVEL", "info", "IA indisponível — usada leitura por padrões."));
            } catch (RuntimeException e) {
                LOG.warnf("[ia] erro ao interpretar a resposta da IA (%s) — usando padrões", e.getClass().getSimpleName());
                alertas.add(new Alerta("IA_INDISPONIVEL", "info", "IA indisponível — usada leitura por padrões."));
            }
        }

        conferir(final_, ctx, alertas);
        DocumentoAnalisado r = montar(final_, ctx, fonte, modelo, true, alertas, compacto);
        LOG.infof("[ia] \"%s\" lido por %s em %d ms: %s", nomeSeguro(nomeArquivo), fonte,
                System.currentTimeMillis() - inicio, r.tipoDocumento());
        return r;
    }

    // ------------------------------------------------------------------ sem texto

    private DocumentoAnalisado semTexto(String formato, String nomeArquivo, ContextoLeitura ctx, List<Alerta> alertas) {
        String msg = switch (formato == null ? "" : formato) {
            case "IMAGEM", "PDF" -> "Documento sem texto (imagem/escaneado) — não foi possível ler. Confira os dados manualmente.";
            case "NAO_SUPORTADO" -> "Formato não suportado pela leitura automática (use PDF, XML, TXT ou CSV).";
            case "ILEGIVEL" -> "Não foi possível abrir o arquivo (corrompido ou protegido por senha).";
            default -> "Documento sem texto legível — não foi possível ler.";
        };
        alertas.add(new Alerta("SEM_TEXTO", "warning", msg));
        Leitura l = new Leitura();
        // o nome do arquivo ainda ajuda a sugerir o tipo ("das-setembro.pdf")
        l.tipo = LeitorPadroes.detectarTipo(LeitorPadroes.normalizar(nomeSeguro(nomeArquivo)).replaceAll("[-_.]", " "));
        l.descricao = l.tipo != TipoDocumento.OUTRO ? LeitorPadroes.descricaoPadrao(l) : null;
        l.confianca = l.tipo != TipoDocumento.OUTRO ? 0.15 : 0.0;
        return montar(l, ctx, FONTE_PADROES, null, false, alertas, "");
    }

    // ------------------------------------------------------------------ IA → Leitura (validando cada campo)

    static Leitura converter(JsonNode j, String cnpjEmpresa) {
        Leitura l = new Leitura();
        l.tipo = TipoDocumento.de(texto(j, "tipoDocumento"));

        String cnpj = digitos(texto(j, "cnpj"));
        String empresa = digitos(cnpjEmpresa);
        if (cnpj != null && cnpj.length() == 14 && (LeitorPadroes.cnpjValido(cnpj) || cnpj.equals(empresa))) l.cnpj = cnpj;

        l.razaoSocial = curto(texto(j, "razaoSocial"), 120);
        l.competencia = LeitorPadroes.normalizarCompetencia(texto(j, "competencia"));
        l.vencimento = dataIa(texto(j, "vencimento"));
        l.validade = dataIa(texto(j, "validade"));
        l.valor = valorIa(j.get("valor"));

        String linha = digitos(texto(j, "linhaDigitavel"));
        CodigoBarras.Info info = linha != null ? CodigoBarras.analisar(linha) : null;
        if (info != null) {
            l.linhaDigitavel = info.linhaDigitavel();
            l.codigoBarras = info.codigoBarras();
            l.linhaValida = info.valido();
        }
        l.numeroDocumento = curto(texto(j, "numeroDocumento"), 60);
        l.descricao = curto(texto(j, "descricaoSugerida"), 120);
        JsonNode c = j.get("confianca");
        if (c != null && c.isNumber()) l.confianca = Math.max(0, Math.min(1, c.asDouble()));
        JsonNode obs = j.get("observacoes");
        if (obs != null && obs.isArray()) {
            for (JsonNode o : obs) {
                String s = curto(o.isTextual() ? o.asText() : null, 160);
                if (s != null && l.observacoes.size() < 3) l.observacoes.add(s);
            }
        }
        return l;
    }

    /** IA vence quando o dado dela é válido; a linha digitável conferida (DV) e o valor codificado nela vencem a IA. */
    static Leitura fundir(Leitura p, Leitura ia) {
        Leitura f = new Leitura();
        f.tipo = ia.tipo != null && !(ia.tipo == TipoDocumento.OUTRO && p.tipo != TipoDocumento.OUTRO) ? ia.tipo : p.tipo;
        f.cnpjs = new ArrayList<>(p.cnpjs);
        if (ia.cnpj != null && !f.cnpjs.contains(ia.cnpj)) f.cnpjs.add(ia.cnpj);
        f.cnpj = ia.cnpj != null ? ia.cnpj : p.cnpj;
        f.razaoSocial = primeiro(ia.razaoSocial, p.razaoSocial);
        f.competencia = primeiro(ia.competencia, p.competencia);
        f.vencimento = primeiro(ia.vencimento, p.vencimento);
        f.validade = primeiro(ia.validade, p.validade);
        f.numeroDocumento = primeiro(ia.numeroDocumento, p.numeroDocumento);

        if (p.linhaValida) {
            f.linhaDigitavel = p.linhaDigitavel; f.codigoBarras = p.codigoBarras; f.linhaValida = true;
        } else if (ia.linhaValida) {
            f.linhaDigitavel = ia.linhaDigitavel; f.codigoBarras = ia.codigoBarras; f.linhaValida = true;
        } else {
            f.linhaDigitavel = primeiro(p.linhaDigitavel, ia.linhaDigitavel);
            f.codigoBarras = primeiro(p.codigoBarras, ia.codigoBarras);
        }
        f.valorDaLinha = p.valorDaLinha;
        f.valor = (p.linhaValida && p.valorDaLinha != null) ? p.valorDaLinha : primeiro(ia.valor, p.valor);

        f.descricao = (f.tipo != null && f.tipo.isGuia()) || ia.descricao == null ? LeitorPadroes.descricaoPadrao(f) : ia.descricao;
        if (f.descricao == null) f.descricao = ia.descricao;
        double base = Math.min(0.95, 0.4 + 0.09 * f.camposEncontrados());
        f.confianca = Math.min(0.98, (ia.confianca != null ? (ia.confianca + base) / 2 : base) + (f.linhaValida ? 0.03 : 0));
        return f;
    }

    // ------------------------------------------------------------------ conferências

    void conferir(Leitura l, ContextoLeitura ctx, List<Alerta> alertas) {
        TipoDocumento tipo = l.tipo != null ? l.tipo : TipoDocumento.OUTRO;
        String empresa = digitos(ctx.cnpjEmpresa());

        if (empresa != null && empresa.length() == 14) {
            if (!l.cnpjs.isEmpty() || l.cnpj != null) {
                if (cnpjConfere(l, empresa) == Boolean.FALSE) {
                    alertas.add(new Alerta("CNPJ_DIVERGENTE", "danger", "CNPJ do documento (" + formatarCnpj(l.cnpj)
                            + ") é diferente do CNPJ da empresa" + (ctx.nomeEmpresa() != null ? " " + ctx.nomeEmpresa() : "")
                            + " (" + formatarCnpj(empresa) + ")."));
                }
            } else if (tipo.isGuia()) {
                alertas.add(new Alerta("CNPJ_AUSENTE", "warning", "CNPJ não encontrado no documento — confira se a guia é desta empresa."));
            }
        }

        Set<TipoDocumento> esperados = ctx.nomeObrigacao() != null
                ? tiposEsperados(ctx.nomeObrigacao()) : EnumSet.noneOf(TipoDocumento.class);
        if (tipo != TipoDocumento.OUTRO && !esperados.isEmpty() && !esperados.contains(tipo)) {
            alertas.add(new Alerta("TIPO_INCOMPATIVEL", "danger", "O documento parece ser " + tipo.getRotulo()
                    + ", mas a obrigação é \"" + ctx.nomeObrigacao() + "\"."));
        }

        // o vencimento só se compara em obrigações de guia (nas de documentos do cliente é o prazo de envio)
        boolean obrigacaoDeGuia = esperados.isEmpty() || esperados.stream().anyMatch(TipoDocumento::isGuia);
        if ((tipo.isGuia() || tipo == TipoDocumento.OUTRO) && obrigacaoDeGuia) {
            if (ctx.vencimentoObrigacao() != null && l.vencimento != null && !l.vencimento.equals(ctx.vencimentoObrigacao())) {
                alertas.add(new Alerta("VENCIMENTO_DIVERGENTE", "warning", "Vencimento do documento (" + BR.format(l.vencimento)
                        + ") difere do vencimento da obrigação (" + BR.format(ctx.vencimentoObrigacao()) + ")."));
            }
        }

        String compObr = ctx.competenciaObrigacao();
        if (compObr != null && compObr.matches("\\d{2}/\\d{4}") && l.competencia != null && !l.competencia.equals(compObr)
                && tipo != TipoDocumento.CERTIDAO && tipo != TipoDocumento.CONTRATO) {
            alertas.add(new Alerta("COMPETENCIA_DIVERGENTE", "warning", "Competência do documento (" + l.competencia
                    + ") difere da competência da obrigação (" + compObr + ")."));
        }

        if (tipo.isGuia()) {
            if (l.vencimento != null && l.vencimento.isBefore(hoje.get())) {
                alertas.add(new Alerta("VENCIDO", "warning", "A guia venceu em " + BR.format(l.vencimento)
                        + " — pode haver multa e juros; confira se precisa ser recalculada."));
            }
            if (l.valor == null) {
                alertas.add(new Alerta("VALOR_AUSENTE", "warning", "Valor da guia não encontrado — confira no documento."));
            }
        }
        if (l.linhaDigitavel != null && !l.linhaValida) {
            alertas.add(new Alerta("LINHA_INVALIDA", "warning",
                    "A linha digitável não passou na conferência dos dígitos verificadores — confira antes de pagar."));
        }
        if (tipo == TipoDocumento.CERTIDAO && l.validade != null && l.validade.isBefore(hoje.get())) {
            alertas.add(new Alerta("CERTIDAO_VENCIDA", "danger", "Certidão vencida em " + BR.format(l.validade) + "."));
        }
    }

    /** true se algum CNPJ do documento é o da empresa (ou da mesma raiz — filial); false se nenhum é; null se não há CNPJ. */
    static Boolean cnpjConfere(Leitura l, String empresa) {
        if (empresa == null || empresa.length() != 14) return null;
        List<String> todos = new ArrayList<>(l.cnpjs);
        if (l.cnpj != null && !todos.contains(l.cnpj)) todos.add(l.cnpj);
        if (todos.isEmpty()) return null;
        for (String c : todos) {
            if (c.substring(0, 8).equals(empresa.substring(0, 8))) {
                l.cnpj = c;
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static final Pattern OBR_DAS = Pattern.compile("^das\\b|simples nacional|\\bpgdas|\\bdas\\s*-?\\s*mei");
    private static final Pattern OBR_FGTS = Pattern.compile("\\bfgts\\b|\\bgfd\\b|\\bgrf\\b");
    private static final Pattern OBR_INSS = Pattern.compile("\\binss\\b|\\bgps\\b|previd|\\bcpp\\b|dctfweb");
    private static final Pattern OBR_DARF = Pattern.compile("\\bdarf\\b|\\birpj\\b|\\bcsll\\b|\\bpis\\b|\\bcofins\\b|\\birrf\\b|\\bipi\\b");
    private static final Pattern OBR_ICMS = Pattern.compile("\\bicms\\b|\\bdare\\b|\\bdifal\\b");
    private static final Pattern OBR_ISS = Pattern.compile("\\biss\\b|\\bissqn\\b");

    public static Set<TipoDocumento> tiposEsperados(String nomeObrigacao) {
        String n = LeitorPadroes.normalizar(nomeObrigacao);
        if (OBR_DAS.matcher(n).find()) return EnumSet.of(TipoDocumento.DAS);
        if (OBR_FGTS.matcher(n).find()) return EnumSet.of(TipoDocumento.FGTS);
        if (OBR_INSS.matcher(n).find()) return EnumSet.of(TipoDocumento.GPS, TipoDocumento.DARF);
        if (OBR_DARF.matcher(n).find()) return EnumSet.of(TipoDocumento.DARF);
        if (OBR_ICMS.matcher(n).find()) return EnumSet.of(TipoDocumento.ICMS);
        if (OBR_ISS.matcher(n).find()) return EnumSet.of(TipoDocumento.ISS);
        if (n.contains("extrato")) return EnumSet.of(TipoDocumento.EXTRATO_BANCARIO);
        if (n.contains("nota") || n.matches(".*\\bnfs?-?e\\b.*")) return EnumSet.of(TipoDocumento.NFE, TipoDocumento.NFSE);
        if (n.contains("balancete")) return EnumSet.of(TipoDocumento.BALANCETE);
        if (n.contains("certid")) return EnumSet.of(TipoDocumento.CERTIDAO);
        if (n.contains("contrato")) return EnumSet.of(TipoDocumento.CONTRATO);
        if (n.contains("folha de pagamento") || n.contains("holerite")) return EnumSet.of(TipoDocumento.FOLHA);
        return EnumSet.noneOf(TipoDocumento.class);
    }

    // ------------------------------------------------------------------ montagem

    private DocumentoAnalisado montar(Leitura l, ContextoLeitura ctx, String fonte, String modelo, boolean legivel,
                                      List<Alerta> alertas, String texto) {
        TipoDocumento tipo = l.tipo != null ? l.tipo : TipoDocumento.OUTRO;
        Boolean confere = cnpjConfere(l, digitos(ctx.cnpjEmpresa()));
        CategoriaFiscal categoria = tipo.getCategoria();
        if ((tipo == TipoDocumento.NFE || tipo == TipoDocumento.NFSE) && confere == Boolean.TRUE && !l.cnpjs.isEmpty()
                && l.cnpjs.get(0).substring(0, 8).equals(digitos(ctx.cnpjEmpresa()).substring(0, 8))) {
            // o primeiro CNPJ de uma nota é o do emitente: se é a própria empresa, é nota de saída
            categoria = CategoriaFiscal.NFE_SAIDA;
        }
        // ordena: danger → warning → info
        List<Alerta> ordenados = new ArrayList<>(alertas);
        ordenados.sort((a, b) -> Integer.compare(peso(a.nivel()), peso(b.nivel())));
        String resumo = texto == null ? "" : texto.strip();
        if (resumo.length() > 300) resumo = resumo.substring(0, 300) + "…";
        double conf = l.confianca != null ? l.confianca : 0;
        return new DocumentoAnalisado(
                tipo, tipo.getRotulo(), categoria, l.descricao,
                l.cnpj, confere, l.razaoSocial, l.competencia, l.vencimento, l.valor,
                l.linhaDigitavel, l.codigoBarras, l.numeroDocumento, l.validade,
                fonte, modelo, Math.round(conf * 100) / 100.0, legivel, List.copyOf(ordenados), resumo);
    }

    private static int peso(String nivel) {
        return switch (nivel) { case "danger" -> 0; case "warning" -> 1; default -> 2; };
    }

    // ------------------------------------------------------------------ apoio

    static String compactar(String t) {
        return t.replace(' ', ' ').replace("\r", "")
                .replaceAll("[ \\t\\f]{2,}", "  ")
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static String texto(JsonNode j, String campo) {
        JsonNode n = j.get(campo);
        if (n == null || n.isNull()) return null;
        String s = n.asText(null);
        if (s == null || s.isBlank() || s.equalsIgnoreCase("null")) return null;
        return s.trim();
    }

    private static String digitos(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static String curto(String s, int max) {
        if (s == null) return null;
        s = s.replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return null;
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static LocalDate dataIa(String s) {
        if (s == null) return null;
        return LeitorPadroes.primeiraData(s);
    }

    private static BigDecimal valorIa(JsonNode n) {
        if (n == null || n.isNull()) return null;
        BigDecimal v = null;
        if (n.isNumber()) v = n.decimalValue();
        else if (n.isTextual()) {
            String s = n.asText().replaceAll("[^\\d,.]", "");
            if (s.matches("\\d{1,3}(\\.\\d{3})*,\\d{1,2}|\\d+,\\d{1,2}")) v = LeitorPadroes.dinheiro(s);
            else if (s.matches("\\d+(\\.\\d{1,2})?")) v = new BigDecimal(s);
        }
        if (v == null) return null;
        v = v.setScale(2, java.math.RoundingMode.HALF_UP);
        return LeitorPadroes.valorPlausivel(v) ? v : null;
    }

    private static <T> T primeiro(T a, T b) {
        return a != null ? a : b;
    }

    private static String nomeSeguro(String nome) {
        if (nome == null) return "documento";
        String n = nome.replaceAll("[\\r\\n\\t]", " ").trim();
        return n.length() > 120 ? n.substring(0, 120) : n;
    }

    public static String formatarCnpj(String c) {
        if (c == null || c.length() != 14) return c;
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }
}
