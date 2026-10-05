package diniz.contabilidade.arquivos.assistente;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import diniz.contabilidade.arquivos.assistente.AssistenteDto.Acao;

/**
 * Detecção de assunto por palavras-chave (sem rede) e botões de navegação sugeridos.
 * Usada tanto com a IA (para os botões) quanto nas respostas automáticas.
 */
public final class AssistenteIntencoes {

    private AssistenteIntencoes() {
    }

    private static final Map<AssistenteIntencao, Pattern> PADROES = new LinkedHashMap<>();

    static {
        PADROES.put(AssistenteIntencao.AGRADECIMENTO, p("^(muito )?(obrigad[oa]|valeu|agradeco|grat[oa])\\b"));
        PADROES.put(AssistenteIntencao.CERTIDOES, p("certid|\\bcnds?\\b|\\bcrf\\b|\\bcndt\\b|regularidade"));
        PADROES.put(AssistenteIntencao.DEC, p("\\bdec\\b|sefaz|comunicac|intimac|notificac|cienci|domicilio eletronico|fiscaliza"));
        PADROES.put(AssistenteIntencao.VENCIDAS, p("vencid|atrasad|em atraso|\\batraso|passou do prazo|perdi o prazo"));
        PADROES.put(AssistenteIntencao.ANEXAR_GUIA, p("anex\\w* (a |uma |as )?guia|(subir|lancar|emitir|entregar) (a |uma |as )?guia|prorrog|adiar (o )?vencimento|mudar (o )?vencimento"));
        PADROES.put(AssistenteIntencao.RESUMO, p("resum|situacao|carteira|panorama|visao geral|como (esta|estao|estou|anda)|status geral|balanco|prioridade|urgente"));
        PADROES.put(AssistenteIntencao.PAGAMENTO, p("\\bpag(o|os|a|as|ar|ou|uei|amos|ando|ue|amentos?)\\b|boleto|comprovante|quitar|quitad|\\bpix\\b"));
        PADROES.put(AssistenteIntencao.ENVIAR, p("enviar|envio|\\bmandar|documento|extrato|nota[s]? fisca|\\bnf|preciso entregar|pedid[oa]s? pelo escritorio|solicitad"));
        PADROES.put(AssistenteIntencao.PRAZOS, p("venc|prazo|semana|proxim|\\bhoje\\b|amanha|\\bmes\\b|quando|\\bguias?\\b|obrigac|pendenc|agenda"));
        PADROES.put(AssistenteIntencao.ARQUIVOS, p("arquivo|pasta|drive|upload|lixeira|baixar|download"));
        PADROES.put(AssistenteIntencao.CALENDARIO, p("calendario"));
        PADROES.put(AssistenteIntencao.RELATORIOS, p("relatorio|exportar|imprimir|planilha"));
        PADROES.put(AssistenteIntencao.MENSAGENS, p("mensage|conversa|falar com|chat|contato|contatar|atendimento"));
        PADROES.put(AssistenteIntencao.CONTA, p("senha|minha conta|meu perfil|meus dados|trocar e-?mail|alterar e-?mail"));
        PADROES.put(AssistenteIntencao.AJUDA, p("ajuda|o que voce (faz|sabe)|como usar|como funciona|para que serve|tutorial|\\bhelp\\b|duvida"));
        PADROES.put(AssistenteIntencao.SAUDACAO, p("^(oi|ola|bom dia|boa tarde|boa noite|e ai|hey|opa|tudo bem)\\b"));
    }

    private static final Pattern COMO_FAZER = p("\\bcomo\\b|\\bonde\\b|passo a passo|de que forma|consigo|posso|tenho que fazer|o que faco|faco para");

    private static Pattern p(String regex) {
        return Pattern.compile(regex);
    }

    /** minúsculas, sem acentos e espaços repetidos. */
    public static String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /** Assuntos citados, em ordem de prioridade (EnumSet itera na ordem do enum). */
    public static Set<AssistenteIntencao> detectar(String mensagem) {
        String t = normalizar(mensagem);
        Set<AssistenteIntencao> achadas = EnumSet.noneOf(AssistenteIntencao.class);
        for (Map.Entry<AssistenteIntencao, Pattern> e : PADROES.entrySet()) {
            if (e.getValue().matcher(t).find()) achadas.add(e.getKey());
        }
        // "como funciona o calendário" é pergunta sobre o calendário, não ajuda genérica
        if (achadas.size() > 1) achadas.remove(AssistenteIntencao.SAUDACAO);
        boolean temAssunto = achadas.stream().anyMatch(i -> i != AssistenteIntencao.AJUDA && i != AssistenteIntencao.SAUDACAO);
        if (temAssunto) achadas.remove(AssistenteIntencao.AJUDA);
        // "obrigado, e as guias?" → segue o assunto
        if (achadas.size() > 1) achadas.remove(AssistenteIntencao.AGRADECIMENTO);
        return achadas;
    }

    /** Assunto principal (ou null se nenhum foi reconhecido). */
    public static AssistenteIntencao principal(Set<AssistenteIntencao> intencoes) {
        return intencoes.isEmpty() ? null : intencoes.iterator().next();
    }

    /** A pergunta pede um passo a passo ("como...", "onde...")? */
    public static boolean pedeComoFazer(String mensagem) {
        return COMO_FAZER.matcher(normalizar(mensagem)).find();
    }

    /** Até 3 botões de navegação coerentes com o assunto e o perfil. */
    public static List<Acao> acoes(String mensagem, Set<AssistenteIntencao> intencoes, boolean escritorio) {
        String t = normalizar(mensagem);
        AssistenteIntencao principal = principal(intencoes);
        List<Acao> lista = new ArrayList<>();
        for (AssistenteIntencao i : intencoes) {
            // "vence"/"prazo" junto de outro assunto (certidões, DEC...) não gera botões de prazos
            if (i == AssistenteIntencao.PRAZOS && principal != AssistenteIntencao.PRAZOS) continue;
            switch (i) {
                case VENCIDAS -> lista.add(pend("Ver pendências vencidas", "vencidas"));
                case ANEXAR_GUIA -> lista.add(pend(escritorio ? "Abrir Pendências" : "Guias a pagar", escritorio ? "pendentes" : "pagar"));
                case ENVIAR -> lista.add(pend(escritorio ? "Documentos aguardando clientes" : "Ver o que enviar", "enviar"));
                case PAGAMENTO -> lista.add(pend(escritorio ? "Guias aguardando pagamento" : "Guias a pagar", "pagar"));
                case PRAZOS -> {
                    if (!escritorio && t.matches(".*\\bguias?\\b.*")) lista.add(pend("Guias a pagar", "pagar"));
                    lista.add(pend("Vencimentos da semana", "semana"));
                    lista.add(new Acao("Abrir calendário", "/calendario"));
                }
                case DEC -> lista.add(new Acao("Comunicações que exigem atenção", "/dec", Map.of("filtro", "atencao")));
                case CERTIDOES -> {
                    boolean vencidas = t.contains("vencid");
                    lista.add(new Acao(vencidas ? "Certidões vencidas" : "Certidões vencendo", "/certidoes",
                            Map.of("status", vencidas ? "VENCIDA" : "VENCENDO")));
                }
                case ARQUIVOS -> lista.add(new Acao("Abrir Arquivos", "/arquivos"));
                case CALENDARIO -> lista.add(new Acao("Abrir calendário", "/calendario"));
                case RELATORIOS -> lista.add(new Acao("Abrir Relatórios", "/relatorios"));
                case MENSAGENS -> lista.add(pend("Pendências com mensagens", "mensagens"));
                case CONTA -> lista.add(new Acao("Minha conta", "/conta"));
                case RESUMO -> {
                    lista.add(new Acao("Ir para o Painel", "/dashboard"));
                    if (escritorio) lista.add(pend("Ver pendências vencidas", "vencidas"));
                }
                case AJUDA -> lista.add(new Acao("Abrir o guia Como usar", "/como-usar"));
                default -> {
                }
            }
        }
        // sem duplicar rota+filtro, no máximo 3
        List<Acao> unicas = new ArrayList<>();
        for (Acao a : lista) {
            boolean repetida = unicas.stream().anyMatch(u -> u.rota().equals(a.rota())
                    && java.util.Objects.equals(u.queryParams(), a.queryParams()));
            if (!repetida) unicas.add(a);
            if (unicas.size() == 3) break;
        }
        return unicas;
    }

    private static Acao pend(String rotulo, String filtro) {
        return new Acao(rotulo, "/obrigacoes-pendentes", Map.of("filtro", filtro));
    }
}
