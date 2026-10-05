package diniz.contabilidade.arquivos.service.ia;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/**
 * Leitura determinística (sem IA) de guias e documentos fiscais por expressões regulares.
 *
 * Sempre roda — com ou sem IA — e serve de rede de segurança: CNPJ (com dígitos verificadores),
 * valores próximos de "valor total / total a recolher", vencimento, competência/período de apuração,
 * linha digitável (47/48 dígitos, validada) e o tipo do documento por palavras-chave.
 */
public final class LeitorPadroes {

    private LeitorPadroes() {}

    // ------------------------------------------------------------------ padrões de valor

    private static final Pattern CNPJ = Pattern.compile("(?<![\\d./-])(\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2})(?![\\d])");
    private static final Pattern MASCARA_CNPJ = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
    private static final Pattern DINHEIRO =Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+,\\d{2})(?![\\d,])");
    /** XML / sistemas: 1234.56 (ponto decimal). */
    private static final Pattern DINHEIRO_PONTO = Pattern.compile("(?<![\\d.,])(\\d+\\.\\d{2})(?![\\d.,])");
    private static final Pattern REAIS = Pattern.compile("r\\$\\s*(\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+,\\d{2})(?![\\d,])");
    private static final Pattern DATA = Pattern.compile("(?<![\\d/])(\\d{2})[/.-](\\d{2})[/.-](\\d{4})(?![\\d/])");
    private static final Pattern DATA_ISO = Pattern.compile("(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})(?!\\d)");
    private static final Pattern MES_ANO = Pattern.compile("(?<![\\d/])(0[1-9]|1[0-2])\\s?/\\s?((?:19|20)\\d{2})(?![\\d/])");
    private static final Pattern MES_EXTENSO = Pattern.compile(
            "(janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro|"
            + "jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)\\s*(?:/|de|-|\\s)\\s*((?:19|20)\\d{2})");
    private static final Pattern LINHA = Pattern.compile("(?<!\\d)(\\d[\\d .\\-]{42,75}\\d)(?!\\d)");
    private static final Pattern NUMERO_DOC = Pattern.compile("(?<![\\w/.,-])([0-9][0-9.\\-/]{2,40}[0-9])(?![\\w/])");

    private static final String[] MESES = {"janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho",
            "agosto", "setembro", "outubro", "novembro", "dezembro"};

    // ------------------------------------------------------------------ rótulos (texto sem acento, minúsculo)

    private static final List<Pattern> ROTULOS_VALOR = rotulos(
            "valor total do documento", "valor total a recolher", "total a recolher", "valor a recolher",
            "valor do documento", "valor cobrado", "valor total a pagar", "total a pagar", "valor a pagar",
            "valor da guia", "valor total da nota", "valor total da nfs-?e", "valor liquido da nota", "valor dos servicos",
            "vnf:", "valorservicos:", "vliq:", "valor total", "total geral", "valor liquido", "valor:");

    private static final List<Pattern> ROTULOS_VENCIMENTO = rotulos(
            "pagar este documento ate", "pagar ate", "pagavel ate", "data de vencimento", "data do vencimento",
            "data limite para pagamento", "valido para pagamento ate", "vencimento", "venc\\.", "dt\\.? venc", "dtvenc:");

    private static final List<Pattern> ROTULOS_COMPETENCIA = rotulos(
            "periodo de apuracao", "competencia", "mes/ano de referencia", "mes de referencia", "mes/ano competencia",
            "referencia", "periodo de referencia", "mes ref", "periodo:");

    private static final List<Pattern> ROTULOS_NUMERO = rotulos(
            "numero do documento", "n[o.º°]{0,2}\\s*do documento", "numero da nota", "nota fiscal n[o.º°]{0,2}", "nnf:",
            "numero da nfs-?e", "numero:", "n[º°]\\s");

    private static final List<Pattern> ROTULOS_VALIDADE = rotulos(
            "valida ate", "valido ate", "data de validade", "validade:", "validade", "prazo de validade");

    private static final Pattern ROTULO_RAZAO = Pattern.compile(
            "(nome empresarial|razao social|nome/razao social|razao social/nome|contribuinte|nome do empregador|empregador|"
            + "emitente|nome do contribuinte|xnome:)\\s*[:\\-]?", Pattern.CASE_INSENSITIVE);

    // ------------------------------------------------------------------ tipo por palavras-chave

    private static final Map<TipoDocumento, Map<Pattern, Integer>> PALAVRAS = new LinkedHashMap<>();

    static {
        palavras(TipoDocumento.DAS, "documento de arrecadacao do simples nacional", 10, "simples nacional", 3, "pgdas", 4,
                "\\bdas\\b", 2, "das-?mei|das do mei", 6);
        palavras(TipoDocumento.FGTS, "\\bfgts\\b", 5, "fgts digital", 5, "\\bgfd\\b", 5, "\\bgrf\\b", 5,
                "guia de recolhimento do fgts", 10, "fundo de garantia", 5);
        palavras(TipoDocumento.DARF, "documento de arrecadacao de receitas federais", 10, "\\bdarf\\b", 5,
                "codigo da receita", 2, "receita federal", 1);
        palavras(TipoDocumento.GPS, "guia da previdencia social", 10, "\\bgps\\b", 4, "\\binss\\b", 2,
                "previdencia social", 2);
        palavras(TipoDocumento.ICMS, "\\bicms\\b", 4, "\\bdare\\b", 5, "\\bdae\\b", 3, "documento de arrecadacao estadual", 8,
                "receitas estaduais", 4, "sefaz", 2);
        palavras(TipoDocumento.ISS, "\\bissqn\\b", 5, "\\biss\\b", 3, "imposto sobre servicos", 3, "\\bdam\\b", 3);
        palavras(TipoDocumento.NFE, "danfe", 10, "documento auxiliar da nota fiscal eletronica", 10, "infnfe", 10,
                "nfeproc", 10, "chave de acesso", 3, "nota fiscal eletronica", 5);
        palavras(TipoDocumento.NFSE, "nota fiscal de servicos? eletronica", 12, "nfs-?e", 8, "compnfse|infnfse", 12,
                "prestador de servicos?", 3, "tomador de servicos?", 3);
        palavras(TipoDocumento.EXTRATO_BANCARIO, "extrato", 5, "saldo anterior", 4, "saldo disponivel", 3,
                "conta corrente", 2, "lancamentos", 2, "ofxheader|<ofx>", 10);
        palavras(TipoDocumento.CERTIDAO, "certidao", 8, "certifica-se|certificamos", 4, "regularidade", 3,
                "negativa de debitos", 5);
        palavras(TipoDocumento.CONTRATO, "contrato social", 10, "alteracao contratual", 10, "clausula", 3);
        palavras(TipoDocumento.BALANCETE, "balancete", 10, "ativo circulante", 3, "passivo circulante", 3);
        palavras(TipoDocumento.FOLHA, "folha de pagamento", 10, "holerite", 8, "recibo de pagamento de salario", 10,
                "contracheque", 8, "salario base", 3, "proventos", 3);
    }

    // ------------------------------------------------------------------ API

    /**
     * @param texto        texto extraído do documento
     * @param cnpjEmpresa  CNPJ cadastrado da empresa (aceito mesmo sem dígito verificador válido — dados de demonstração)
     */
    public static Leitura ler(String texto, String cnpjEmpresa) {
        Leitura l = new Leitura();
        if (texto == null || texto.isBlank()) {
            l.tipo = TipoDocumento.OUTRO;
            return l;
        }
        String original = texto.replace('\u00A0', ' ');
        String norm = normalizar(original);

        l.tipo = detectarTipo(norm);
        lerCnpjs(norm, cnpjEmpresa, l);
        lerLinhaDigitavel(norm, l);
        l.valor = lerValor(norm);
        if (l.valorDaLinha != null && l.linhaValida) l.valor = l.valorDaLinha;
        if (l.valor == null && l.valorDaLinha != null) l.valor = l.valorDaLinha;

        l.vencimento = dataAposRotulo(norm, ROTULOS_VENCIMENTO, 90);
        l.competencia = lerCompetencia(norm);
        l.numeroDocumento = lerNumero(norm);
        if (l.tipo == TipoDocumento.CERTIDAO) l.validade = dataAposRotulo(norm, ROTULOS_VALIDADE, 60);
        l.razaoSocial = lerRazaoSocial(original, norm);
        l.descricao = descricaoPadrao(l);
        l.confianca = Math.min(0.85, 0.3 + 0.09 * l.camposEncontrados() + (l.linhaValida ? 0.05 : 0));
        return l;
    }

    // ------------------------------------------------------------------ tipo

    static TipoDocumento detectarTipo(String norm) {
        TipoDocumento melhor = TipoDocumento.OUTRO;
        int maior = 2;
        for (Map.Entry<TipoDocumento, Map<Pattern, Integer>> e : PALAVRAS.entrySet()) {
            int pontos = 0;
            for (Map.Entry<Pattern, Integer> p : e.getValue().entrySet()) {
                if (p.getKey().matcher(norm).find()) pontos += p.getValue();
            }
            if (pontos > maior) {
                maior = pontos;
                melhor = e.getKey();
            }
        }
        return melhor;
    }

    // ------------------------------------------------------------------ CNPJ

    private static void lerCnpjs(String norm, String cnpjEmpresa, Leitura l) {
        String empresa = cnpjEmpresa != null ? cnpjEmpresa.replaceAll("\\D", "") : null;
        Set<String> achados = new LinkedHashSet<>();
        Matcher m = CNPJ.matcher(norm);
        while (m.find()) {
            String bruto = m.group(1);
            String d = bruto.replaceAll("\\D", "");
            if (d.length() != 14) continue;
            boolean mascaraCompleta = MASCARA_CNPJ.matcher(bruto).matches();
            boolean formatado = bruto.contains("/") || bruto.contains(".");
            // máscara completa (00.000.000/0000-00) é evidência forte; 14 dígitos soltos só contam se o DV fechar
            // perto da palavra "CNPJ" (evita pegar pedaços de chave de acesso / linha digitável)
            if (mascaraCompleta || (formatado && cnpjValido(d)) || d.equals(empresa)
                    || (!formatado && cnpjValido(d) && contexto(norm, m.start(), "cnpj"))) {
                achados.add(d);
            }
        }
        l.cnpjs = new ArrayList<>(achados);
        if (achados.isEmpty()) return;
        if (empresa != null) {
            for (String c : achados) {
                if (c.equals(empresa) || c.startsWith(empresa.substring(0, Math.min(8, empresa.length())))) {
                    l.cnpj = c;
                    return;
                }
            }
        }
        l.cnpj = l.cnpjs.get(0);
    }

    private static boolean contexto(String norm, int pos, String palavra) {
        int ini = Math.max(0, pos - 40);
        return norm.substring(ini, pos).contains(palavra);
    }

    public static boolean cnpjValido(String cnpj) {
        if (cnpj == null) return false;
        String d = cnpj.replaceAll("\\D", "");
        if (d.length() != 14 || d.chars().distinct().count() == 1) return false;
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int s = 0;
        for (int i = 0; i < 12; i++) s += Character.digit(d.charAt(i), 10) * p1[i];
        int dv1 = s % 11 < 2 ? 0 : 11 - s % 11;
        s = 0;
        for (int i = 0; i < 13; i++) s += Character.digit(d.charAt(i), 10) * p2[i];
        int dv2 = s % 11 < 2 ? 0 : 11 - s % 11;
        return dv1 == Character.digit(d.charAt(12), 10) && dv2 == Character.digit(d.charAt(13), 10);
    }

    // ------------------------------------------------------------------ linha digitável

    private static void lerLinhaDigitavel(String norm, Leitura l) {
        Matcher m = LINHA.matcher(norm);
        CodigoBarras.Info melhor = null;
        while (m.find()) {
            String d = m.group(1).replaceAll("\\D", "");
            CodigoBarras.Info info = null;
            if (d.length() == 47 || d.length() == 48 || d.length() == 44) info = CodigoBarras.analisar(d);
            if (info == null) continue;
            if (melhor == null || (info.valido() && !melhor.valido())) melhor = info;
            if (melhor.valido()) break;
        }
        if (melhor == null) return;
        l.linhaDigitavel = melhor.linhaDigitavel();
        l.codigoBarras = melhor.codigoBarras();
        l.linhaValida = melhor.valido();
        if (melhor.valido()) {
            l.valorDaLinha = melhor.valor();
            if (melhor.vencimento() != null) l.vencimento = melhor.vencimento();
        }
    }

    // ------------------------------------------------------------------ valor

    static BigDecimal lerValor(String norm) {
        for (Pattern rotulo : ROTULOS_VALOR) {
            Matcher r = rotulo.matcher(norm);
            while (r.find()) {
                String janela = janela(norm, r.end(), 120);
                Matcher v = DINHEIRO.matcher(janela);
                if (v.find()) {
                    BigDecimal b = dinheiro(v.group(1));
                    if (valorPlausivel(b)) return b;
                }
                Matcher vp = DINHEIRO_PONTO.matcher(janela(norm, r.end(), 30));
                if (vp.find()) {
                    BigDecimal b = new BigDecimal(vp.group(1));
                    if (valorPlausivel(b)) return b;
                }
            }
        }
        // sem rótulo: o maior "R$" do documento (em guias costuma ser o total)
        BigDecimal maior = null;
        Matcher m = REAIS.matcher(norm);
        while (m.find()) {
            BigDecimal b = dinheiro(m.group(1));
            if (valorPlausivel(b) && (maior == null || b.compareTo(maior) > 0)) maior = b;
        }
        return maior;
    }

    static BigDecimal dinheiro(String s) {
        try {
            return new BigDecimal(s.replace(".", "").replace(",", ".")).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean valorPlausivel(BigDecimal b) {
        return b != null && b.signum() > 0 && b.compareTo(new BigDecimal("10000000000")) < 0;
    }

    // ------------------------------------------------------------------ datas

    private static LocalDate dataAposRotulo(String norm, List<Pattern> rotulos, int tamanho) {
        for (Pattern rotulo : rotulos) {
            Matcher r = rotulo.matcher(norm);
            while (r.find()) {
                String janela = janela(norm, r.end(), tamanho);
                LocalDate d = primeiraData(janela);
                if (d != null) return d;
            }
        }
        return null;
    }

    static LocalDate primeiraData(String s) {
        Matcher m = DATA.matcher(s);
        Matcher iso = DATA_ISO.matcher(s);
        LocalDate br = null;
        int posBr = Integer.MAX_VALUE;
        while (m.find()) {
            br = data(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            if (br != null) { posBr = m.start(); break; }
        }
        if (iso.find()) {
            LocalDate d = data(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
            if (d != null && iso.start() < posBr) return d;
        }
        return br;
    }

    static LocalDate data(int ano, int mes, int dia) {
        try {
            LocalDate d = LocalDate.of(ano, mes, dia);
            return dataPlausivel(d) ? d : null;
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static boolean dataPlausivel(LocalDate d) {
        return d != null && d.getYear() >= 2000 && d.getYear() <= 2100;
    }

    // ------------------------------------------------------------------ competência

    static String lerCompetencia(String norm) {
        for (Pattern rotulo : ROTULOS_COMPETENCIA) {
            Matcher r = rotulo.matcher(norm);
            while (r.find()) {
                String c = competenciaNoInicio(janela(norm, r.end(), 70));
                if (c != null) return c;
            }
        }
        return null;
    }

    /** A primeira competência que aparece no trecho: "09/2026", "setembro/2026" ou data "30/09/2026". */
    static String competenciaNoInicio(String s) {
        String melhor = null;
        int pos = Integer.MAX_VALUE;
        Matcher a = MES_ANO.matcher(s);
        if (a.find() && a.start() < pos) { pos = a.start(); melhor = a.group(1) + "/" + a.group(2); }
        Matcher b = MES_EXTENSO.matcher(s);
        if (b.find() && b.start() < pos) {
            int mes = mesPorNome(b.group(1));
            if (mes > 0) { pos = b.start(); melhor = String.format("%02d/%s", mes, b.group(2)); }
        }
        Matcher c = DATA.matcher(s);
        if (c.find() && c.start() < pos) {
            LocalDate d = data(Integer.parseInt(c.group(3)), Integer.parseInt(c.group(2)), Integer.parseInt(c.group(1)));
            if (d != null) melhor = String.format("%02d/%d", d.getMonthValue(), d.getYear());
        }
        return melhor;
    }

    private static int mesPorNome(String nome) {
        for (int i = 0; i < MESES.length; i++) {
            if (MESES[i].startsWith(nome)) return i + 1;
        }
        return 0;
    }

    /** Normaliza competência para "MM/aaaa" (aceita "9/2026", "2026-09", "setembro/2026"). Null se inválida. */
    public static String normalizarCompetencia(String c) {
        if (c == null || c.isBlank()) return null;
        String s = normalizar(c).trim();
        Matcher iso = Pattern.compile("^((?:19|20)\\d{2})-(0?[1-9]|1[0-2])(?:-\\d{2})?$").matcher(s);
        if (iso.find()) return String.format("%02d/%s", Integer.parseInt(iso.group(2)), iso.group(1));
        Matcher m = Pattern.compile("^(0?[1-9]|1[0-2])\\s?/\\s?((?:19|20)\\d{2})$").matcher(s);
        if (m.find()) return String.format("%02d/%s", Integer.parseInt(m.group(1)), m.group(2));
        return competenciaNoInicio(s);
    }

    // ------------------------------------------------------------------ número do documento

    private static String lerNumero(String norm) {
        for (Pattern rotulo : ROTULOS_NUMERO) {
            Matcher r = rotulo.matcher(norm);
            while (r.find()) {
                String janela = janela(norm, r.end(), 70);
                Matcher m = NUMERO_DOC.matcher(janela);
                while (m.find()) {
                    String n = m.group(1);
                    if (DATA.matcher(n).matches() || MES_ANO.matcher(n).matches() || DINHEIRO.matcher(n).matches()) continue;
                    int digitos = n.replaceAll("\\D", "").length();
                    if (digitos > 30 || digitos < 4) continue;
                    return n;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ razão social

    private static String lerRazaoSocial(String original, String norm) {
        boolean alinhado = original.length() == norm.length();
        String base = alinhado ? original : norm;
        Matcher r = ROTULO_RAZAO.matcher(norm);
        while (r.find()) {
            int fimLinha = norm.indexOf('\n', r.end());
            if (fimLinha < 0) fimLinha = norm.length();
            String resto = base.substring(r.end(), fimLinha);
            String nome = limparNome(resto);
            if (nome == null) {
                int fim2 = norm.indexOf('\n', fimLinha + 1);
                if (fimLinha < norm.length() && fim2 != fimLinha + 1) {
                    nome = limparNome(base.substring(Math.min(fimLinha + 1, base.length()), fim2 < 0 ? base.length() : fim2));
                }
            }
            if (nome != null) return nome;
        }
        return null;
    }

    private static String limparNome(String s) {
        if (s == null) return null;
        String t = CNPJ.matcher(s).replaceAll(" ").replaceAll("(?i)\\bcnpj\\b[:\\s]*", " ")
                .replaceAll("^[\\s:\\-|]+", "").replaceAll("\\s{2,}", " ").trim();
        if (t.length() < 3 || t.length() > 120) return null;
        if (!t.matches(".*[A-Za-zÀ-ÿ]{3,}.*")) return null;
        String n = normalizar(t);
        if (n.startsWith("cnpj") || n.startsWith("periodo") || n.startsWith("data") || n.startsWith("cpf")) return null;
        return t;
    }

    // ------------------------------------------------------------------ descrição

    static String descricaoPadrao(Leitura l) {
        TipoDocumento t = l.tipo != null ? l.tipo : TipoDocumento.OUTRO;
        String comp = l.competencia != null ? " — competência " + l.competencia : "";
        return switch (t) {
            case DAS -> "DAS — Simples Nacional" + comp;
            case DARF -> "DARF" + comp;
            case GPS -> "GPS / INSS" + comp;
            case FGTS -> "Guia do FGTS" + comp;
            case ICMS -> "Guia de ICMS" + comp;
            case ISS -> "Guia de ISS" + comp;
            case NFE -> "NF-e" + (l.numeroDocumento != null ? " nº " + l.numeroDocumento : "") + (l.razaoSocial != null ? " — " + l.razaoSocial : "");
            case NFSE -> "NFS-e" + (l.numeroDocumento != null ? " nº " + l.numeroDocumento : "") + (l.razaoSocial != null ? " — " + l.razaoSocial : "");
            case EXTRATO_BANCARIO -> "Extrato bancário" + (l.competencia != null ? " — " + l.competencia : "");
            case CERTIDAO -> "Certidão" + (l.validade != null ? " — válida até " + String.format("%02d/%02d/%d",
                    l.validade.getDayOfMonth(), l.validade.getMonthValue(), l.validade.getYear()) : "");
            case BALANCETE -> "Balancete" + (l.competencia != null ? " — " + l.competencia : "");
            case FOLHA -> "Folha de pagamento" + comp;
            case CONTRATO -> "Contrato / alteração contratual";
            case OUTRO -> null;
        };
    }

    // ------------------------------------------------------------------ apoio

    /** Minúsculo e sem acentos, mantendo o mesmo comprimento para textos com caracteres pré-compostos. */
    public static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private static String janela(String s, int inicio, int tamanho) {
        return s.substring(inicio, Math.min(s.length(), inicio + tamanho));
    }

    private static List<Pattern> rotulos(String... r) {
        List<Pattern> l = new ArrayList<>();
        for (String s : r) l.add(Pattern.compile(s));
        return l;
    }

    private static void palavras(TipoDocumento tipo, Object... pares) {
        Map<Pattern, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put(Pattern.compile((String) pares[i]), (Integer) pares[i + 1]);
        PALAVRAS.put(tipo, m);
    }
}
