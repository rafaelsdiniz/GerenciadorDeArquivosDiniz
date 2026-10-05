package diniz.contabilidade.arquivos.assistente;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import diniz.contabilidade.arquivos.dto.response.CertidaoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;

/**
 * Retrato dos dados que o usuário logado pode ver, já filtrado por empresa
 * (montado por {@link AssistenteContextoService}). Serve de base tanto para o prompt
 * da IA ({@link #texto()}) quanto para as respostas automáticas.
 *
 * Classe sem CDI nem banco: pode ser montada à mão nos testes.
 */
public class AssistenteContexto {

    /** Limite do texto enviado à IA. */
    public static final int LIMITE_TEXTO = 8000;

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT = Locale.forLanguageTag("pt-BR");

    /** Linha do resumo por empresa (só para o escritório). */
    public record ResumoEmpresa(Long idEmpresa, String nome, int vencidas, int semana, int aEnviar,
                                int aPagar, int decAtencao, int certidoesAlerta) {

        /** quanto maior, mais urgente */
        public int peso() {
            return vencidas * 100 + decAtencao * 60 + certidoesAlerta * 25 + aPagar * 15 + semana * 10 + aEnviar * 3;
        }

        public boolean temAlerta() {
            return peso() > 0;
        }
    }

    boolean escritorio;
    String nomeUsuario = "usuário";
    String nomeEmpresa;
    Long idEmpresa;
    LocalDate hoje = LocalDate.now();
    /** Obrigações não entregues (pendentes e vencidas), por vencimento. */
    List<ObrigacaoPendenteResponseDTO> abertas = new ArrayList<>();
    /** Guias entregues pelo escritório aguardando a confirmação de pagamento. */
    List<ObrigacaoPendenteResponseDTO> aPagar = new ArrayList<>();
    /** Comunicações DEC abertas e sem ciência, das mais urgentes para as menos. */
    List<ComunicacaoDecResponseDTO> decSemCiencia = new ArrayList<>();
    /** Certidões vencendo (≤ 15 dias) ou vencidas. */
    List<CertidaoResponseDTO> certidoesAlerta = new ArrayList<>();
    long totalArquivos;
    long totalEmpresas;
    /** Escritório: empresas ordenadas por urgência. */
    List<ResumoEmpresa> empresas = new ArrayList<>();
    /** Escritório: nome de todas as empresas (para reconhecer "como está a Padaria?"). */
    Map<Long, String> nomesEmpresas = new LinkedHashMap<>();

    private static final Set<String> PALAVRAS_GENERICAS = Set.of(
            "ltda", "eireli", "comercio", "industria", "servicos", "empresa", "empresas", "tocantins", "palmas",
            "brasil", "distribuidora", "comercial", "obrigacoes", "obrigacao", "situacao", "quais", "certidoes",
            "comunicacoes", "pendencias", "documentos", "guias", "vencidas", "semana", "resumo", "carteira");

    /** Escritório: empresa citada pelo nome na pergunta (ou null). */
    public Map.Entry<Long, String> empresaCitada(String mensagem) {
        if (!escritorio || nomesEmpresas.isEmpty()) return null;
        String t = " " + AssistenteIntencoes.normalizar(mensagem).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ") + " ";
        Map.Entry<Long, String> melhor = null;
        int melhorPontos = 0;
        for (Map.Entry<Long, String> e : nomesEmpresas.entrySet()) {
            String nome = AssistenteIntencoes.normalizar(e.getValue()).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
            if (nome.isEmpty()) continue;
            int pontos = t.contains(" " + nome + " ") ? 100 : 0;
            for (String palavra : nome.split(" ")) {
                if (palavra.length() >= 5 && !PALAVRAS_GENERICAS.contains(palavra) && t.contains(" " + palavra + " ")) pontos++;
            }
            if (pontos > melhorPontos) {
                melhorPontos = pontos;
                melhor = e;
            }
        }
        return melhor;
    }

    /** Recorte do contexto só com uma empresa (respostas sobre uma empresa citada). */
    public AssistenteContexto daEmpresa(Long id, String nome) {
        AssistenteContexto c = new AssistenteContexto();
        c.escritorio = true;
        c.nomeUsuario = nomeUsuario;
        c.nomeEmpresa = nome;
        c.idEmpresa = id;
        c.hoje = hoje;
        c.abertas = abertas.stream().filter(o -> id.equals(o.idEmpresa())).toList();
        c.aPagar = aPagar.stream().filter(o -> id.equals(o.idEmpresa())).toList();
        c.decSemCiencia = decSemCiencia.stream().filter(o -> id.equals(o.idEmpresa())).toList();
        c.certidoesAlerta = certidoesAlerta.stream().filter(o -> id.equals(o.idEmpresa())).toList();
        c.totalEmpresas = 1;
        return c;
    }

    // ------------------------------------------------------------------ getters (para quem está fora do pacote)

    public boolean isEscritorio() { return escritorio; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public LocalDate getHoje() { return hoje; }

    /** Primeiro nome, para cumprimentar. */
    public String primeiroNome() {
        if (nomeUsuario == null || nomeUsuario.isBlank()) return "";
        return nomeUsuario.trim().split("\\s+")[0];
    }

    // ------------------------------------------------------------------ recortes

    public static boolean vencida(ObrigacaoPendenteResponseDTO o) {
        return o.status() == StatusObrigacao.VENCIDA
                || (o.status() == StatusObrigacao.PENDENTE && o.diasParaVencer() != null && o.diasParaVencer() < 0);
    }

    public static boolean doCliente(ObrigacaoPendenteResponseDTO o) {
        return o.responsavel() == ResponsavelObrigacao.CLIENTE;
    }

    public static boolean decAtencao(ComunicacaoDecResponseDTO c) {
        boolean urgente = "CRITICA".equals(c.urgencia()) || "ALTA".equals(c.urgencia());
        boolean tacitaProxima = c.diasRestantes() != null && c.diasRestantes() <= 3;
        return urgente || tacitaProxima;
    }

    public List<ObrigacaoPendenteResponseDTO> vencidas() {
        return abertas.stream().filter(AssistenteContexto::vencida).toList();
    }

    /** Pendentes que vencem de hoje até daqui a {@code dias} dias. */
    public List<ObrigacaoPendenteResponseDTO> proximas(int dias) {
        return abertas.stream()
                .filter(o -> !vencida(o) && o.diasParaVencer() != null && o.diasParaVencer() >= 0 && o.diasParaVencer() <= dias)
                .toList();
    }

    public List<ObrigacaoPendenteResponseDTO> aEnviar() {
        return abertas.stream().filter(AssistenteContexto::doCliente).toList();
    }

    public List<ComunicacaoDecResponseDTO> decComAtencao() {
        return decSemCiencia.stream().filter(AssistenteContexto::decAtencao).toList();
    }

    public List<ResumoEmpresa> empresasComAlerta() {
        return empresas.stream().filter(ResumoEmpresa::temAlerta).toList();
    }

    /** Ordena as listas (chamado por quem monta o contexto). */
    void ordenar() {
        Comparator<ObrigacaoPendenteResponseDTO> porData = Comparator.comparing(
                ObrigacaoPendenteResponseDTO::dataVencimento, Comparator.nullsLast(Comparator.naturalOrder()));
        abertas = new ArrayList<>(abertas.stream().sorted(porData).toList());
        aPagar = new ArrayList<>(aPagar.stream().sorted(porData).toList());
        decSemCiencia = new ArrayList<>(decSemCiencia.stream().sorted(
                Comparator.comparingInt((ComunicacaoDecResponseDTO c) -> pesoUrgencia(c.urgencia()))
                        .thenComparing(ComunicacaoDecResponseDTO::diasRestantes, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        certidoesAlerta = new ArrayList<>(certidoesAlerta.stream().sorted(Comparator.comparing(
                CertidaoResponseDTO::dataValidade, Comparator.nullsLast(Comparator.naturalOrder()))).toList());
        empresas = new ArrayList<>(empresas.stream()
                .sorted(Comparator.comparingInt(ResumoEmpresa::peso).reversed().thenComparing(ResumoEmpresa::nome,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList());
    }

    static int pesoUrgencia(String u) {
        if (u == null) return 5;
        return switch (u) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            case "BAIXA" -> 3;
            case "SEM_RISCO" -> 4;
            default -> 5;
        };
    }

    // ------------------------------------------------------------------ formatação (compartilhada com as respostas automáticas)

    public static String data(LocalDate d) {
        return d == null ? "sem data" : d.format(BR);
    }

    /** "hoje", "amanhã", "em 5 dias", "há 3 dias". */
    public static String relativo(Long dias) {
        if (dias == null) return "";
        if (dias == 0) return "hoje";
        if (dias == 1) return "amanhã";
        if (dias == -1) return "ontem";
        return dias > 0 ? "em " + dias + " dias" : "há " + (-dias) + " dias";
    }

    public static String moeda(java.math.BigDecimal v) {
        java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(PT);
        return f.format(v).replace(' ', ' ');
    }

    public static String nomeObrigacao(ObrigacaoPendenteResponseDTO o) {
        return o.nomeObrigacao() == null || o.nomeObrigacao().isBlank() ? "Obrigação" : o.nomeObrigacao();
    }

    public static String rotuloUrgencia(String u) {
        if (u == null) return "sem classificação";
        return switch (u) {
            case "CRITICA" -> "crítica";
            case "ALTA" -> "alta";
            case "MEDIA" -> "média";
            case "BAIXA" -> "baixa";
            case "SEM_RISCO" -> "sem risco";
            default -> u.toLowerCase();
        };
    }

    public static String rotuloTipoDec(String t) {
        if (t == null || t.isBlank()) return "Comunicação";
        switch (t) {
            case "NOTIFICACAO": return "Notificação";
            case "INTIMACAO": return "Intimação";
            case "ALERTA": return "Alerta";
            case "COMUNICADO": return "Comunicado";
            case "INFORMATIVO": return "Informativo";
            case "AVISO": return "Aviso";
            case "DOCUMENTO_ADMINISTRATIVO": return "Documento administrativo";
            default: break;
        }
        String s = t.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String corta(String s, int max) {
        if (s == null) return "";
        String limpo = s.replaceAll("\\s+", " ").trim();
        return limpo.length() <= max ? limpo : limpo.substring(0, max - 1).trim() + "…";
    }

    static String nomeEmpresaDec(ComunicacaoDecResponseDTO c) {
        if (c.nomeEmpresa() != null && !c.nomeEmpresa().isBlank()) return c.nomeEmpresa();
        if (c.razaoSocial() != null && !c.razaoSocial().isBlank()) return c.razaoSocial() + " (CNPJ não cadastrado)";
        return "CNPJ " + c.cnpj();
    }

    // ------------------------------------------------------------------ texto para a IA

    /** Retrato compacto em texto (≤ {@value #LIMITE_TEXTO} caracteres). Sem senhas nem tokens. */
    public String texto() {
        Texto t = new Texto(LIMITE_TEXTO);
        t.linha("USUÁRIO: " + nomeUsuario + (escritorio
                ? " — perfil ESCRITÓRIO (administrador da Diniz Assessoria Contábil; vê todas as " + totalEmpresas + " empresas clientes)"
                : " — perfil CLIENTE (funcionário da empresa " + (nomeEmpresa == null ? "não informada" : nomeEmpresa) + "; vê só os dados dela)"));
        t.linha("HOJE: " + data(hoje) + " (" + hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, PT) + ")");

        List<ObrigacaoPendenteResponseDTO> vencidas = vencidas();
        List<ObrigacaoPendenteResponseDTO> semana = proximas(7);
        List<ObrigacaoPendenteResponseDTO> quinze = proximas(15);
        List<ObrigacaoPendenteResponseDTO> enviar = aEnviar();
        List<ComunicacaoDecResponseDTO> atencao = decComAtencao();
        t.linha("RESUMO: " + vencidas.size() + " obrigação(ões) vencida(s) · " + semana.size() + " vencem em até 7 dias · "
                + quinze.size() + " em até 15 dias · " + enviar.size() + " documento(s) que o cliente precisa enviar · "
                + aPagar.size() + " guia(s) aguardando pagamento · " + decSemCiencia.size() + " comunicação(ões) DEC sem ciência ("
                + atencao.size() + " exigem atenção) · " + certidoesAlerta.size() + " certidão(ões) vencendo ou vencida(s) · "
                + totalArquivos + " arquivo(s) no Drive");

        if (escritorio) {
            List<ResumoEmpresa> alerta = empresasComAlerta();
            t.secao("EMPRESAS QUE EXIGEM ATENÇÃO (" + alerta.size() + " de " + totalEmpresas + "; top 10 por urgência)");
            if (alerta.isEmpty()) t.linha("- nenhuma empresa com pendência urgente");
            alerta.stream().limit(10).forEach(e -> t.linha("- " + e.nome() + " | vencidas: " + e.vencidas()
                    + " | vencem em 7 dias: " + e.semana() + " | docs a enviar: " + e.aEnviar() + " | guias a pagar: " + e.aPagar()
                    + " | DEC atenção: " + e.decAtencao() + " | certidões em alerta: " + e.certidoesAlerta()));
        }

        int maxObr = escritorio ? 15 : 25;
        t.secao("OBRIGAÇÕES VENCIDAS (" + vencidas.size() + ")");
        if (vencidas.isEmpty()) t.linha("- nenhuma");
        vencidas.stream().limit(maxObr).forEach(o -> t.linha(linhaObrigacao(o)));
        if (vencidas.size() > maxObr) t.linha("- … e mais " + (vencidas.size() - maxObr));

        t.secao("PRÓXIMOS VENCIMENTOS — até 15 dias (" + quinze.size() + ")");
        if (quinze.isEmpty()) t.linha("- nenhum");
        quinze.stream().limit(maxObr).forEach(o -> t.linha(linhaObrigacao(o)));
        if (quinze.size() > maxObr) t.linha("- … e mais " + (quinze.size() - maxObr));

        t.secao("DOCUMENTOS QUE O CLIENTE PRECISA ENVIAR AO ESCRITÓRIO (" + enviar.size() + ")");
        if (enviar.isEmpty()) t.linha("- nenhum");
        enviar.stream().limit(escritorio ? 10 : 20).forEach(o -> t.linha(linhaObrigacao(o)));

        t.secao("GUIAS ENTREGUES AGUARDANDO CONFIRMAÇÃO DE PAGAMENTO (" + aPagar.size() + ")");
        if (aPagar.isEmpty()) t.linha("- nenhuma");
        aPagar.stream().limit(escritorio ? 10 : 20).forEach(o -> t.linha(linhaObrigacao(o)));

        t.secao("COMUNICAÇÕES DO DEC/SEFAZ-TO SEM CIÊNCIA (" + decSemCiencia.size() + ")");
        if (decSemCiencia.isEmpty()) t.linha("- nenhuma");
        decSemCiencia.stream().limit(escritorio ? 10 : 15).forEach(c -> t.linha("- " + rotuloTipoDec(c.tipo())
                + (c.numero() != null ? " nº " + c.numero() : "") + " | assunto: " + corta(c.assunto(), 140)
                + " | urgência: " + rotuloUrgencia(c.urgencia())
                + " | disponibilizada em " + data(c.disponibilizadaEm())
                + (c.cienciaTacitaEm() != null ? " | ciência tácita em " + data(c.cienciaTacitaEm()) + " (" + relativo(c.diasRestantes()) + ")" : "")
                + (c.prazoRespostaEm() != null ? " | prazo de resposta " + data(c.prazoRespostaEm()) : "")
                + (escritorio ? " | empresa: " + nomeEmpresaDec(c) : "")));

        t.secao("CERTIDÕES VENCENDO (≤ 15 dias) OU VENCIDAS (" + certidoesAlerta.size() + ")");
        if (certidoesAlerta.isEmpty()) t.linha("- nenhuma");
        certidoesAlerta.stream().limit(10).forEach(c -> t.linha("- " + c.tipoRotulo() + " | validade " + data(c.dataValidade())
                + " (" + relativo(c.diasParaVencer()) + ") | " + ("VENCIDA".equals(c.statusValidade()) ? "VENCIDA" : "vencendo")
                + (escritorio ? " | empresa: " + c.nomeEmpresa() : "")));

        return t.toString();
    }

    private String linhaObrigacao(ObrigacaoPendenteResponseDTO o) {
        StringBuilder sb = new StringBuilder("- ").append(nomeObrigacao(o));
        if (o.competencia() != null) sb.append(" | competência ").append(o.competencia());
        sb.append(" | vence ").append(data(o.dataVencimento()));
        if (o.diasParaVencer() != null) sb.append(" (").append(relativo(o.diasParaVencer())).append(")");
        sb.append(" | responsável: ").append(doCliente(o) ? "cliente (enviar documento)" : "escritório");
        sb.append(" | situação: ").append(situacao(o));
        if (o.valorGuia() != null) sb.append(" | valor da guia: ").append(moeda(o.valorGuia()));
        if (escritorio) sb.append(" | empresa: ").append(o.nomeEmpresa());
        return sb.toString();
    }

    static String situacao(ObrigacaoPendenteResponseDTO o) {
        if (o.status() == StatusObrigacao.ENTREGUE) {
            return switch (o.situacaoPagamento() == null ? "" : o.situacaoPagamento()) {
                case "PAGO" -> "entregue, guia paga em " + data(o.dataPagamento());
                case "ATRASADO" -> "entregue, pagamento ATRASADO";
                case "AGUARDANDO" -> "entregue, aguardando pagamento";
                default -> "entregue";
            };
        }
        return vencida(o) ? "VENCIDA" : "pendente";
    }

    /** Acumula linhas sem passar do limite. */
    private static final class Texto {
        private final StringBuilder sb = new StringBuilder();
        private final int limite;
        private boolean cortado;

        Texto(int limite) {
            this.limite = limite;
        }

        void secao(String titulo) {
            linha("");
            linha(titulo + ":");
        }

        void linha(String s) {
            if (cortado) return;
            if (sb.length() + s.length() + 1 > limite - 40) {
                sb.append("(… dados truncados)\n");
                cortado = true;
                return;
            }
            sb.append(s).append('\n');
        }

        @Override
        public String toString() {
            return sb.toString().trim();
        }
    }
}
