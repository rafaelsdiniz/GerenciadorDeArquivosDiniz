package diniz.contabilidade.arquivos.service.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Linha digitável e código de barras (padrão FEBRABAN).
 *
 * 48 dígitos: documentos de arrecadação (DAS, DARF, GPS, FGTS, DARE, contas de consumo) — começam com 8.
 * 47 dígitos: boleto bancário.
 * Valida os dígitos verificadores e decodifica o valor (e, no boleto, o vencimento pelo fator).
 */
public final class CodigoBarras {

    private static final LocalDate BASE_FATOR = LocalDate.of(1997, 10, 7);
    /** Em 22/02/2025 o fator de vencimento chegou a 9999 e reiniciou em 1000. */
    private static final LocalDate BASE_FATOR_2025 = LocalDate.of(2025, 2, 22);

    private CodigoBarras() {}

    public record Info(String linhaDigitavel, String codigoBarras, boolean arrecadacao, boolean valido,
                       BigDecimal valor, LocalDate vencimento) {}

    /** Analisa uma sequência de dígitos (44, 47 ou 48). Devolve null se o tamanho não for reconhecido. */
    public static Info analisar(String entrada) {
        if (entrada == null) return null;
        String d = entrada.replaceAll("\\D", "");
        try {
            if (d.length() == 48 && d.charAt(0) == '8') return arrecadacao(d);
            if (d.length() == 47) return boleto(d);
            if (d.length() == 44) {
                return d.charAt(0) == '8' ? arrecadacao(linhaArrecadacao(d)) : boleto(linhaBoleto(d));
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    // ------------------------------------------------------------ arrecadação

    private static Info arrecadacao(String linha) {
        StringBuilder barras = new StringBuilder(44);
        boolean blocosOk = true;
        char idValor = linha.charAt(2);
        for (int b = 0; b < 4; b++) {
            String bloco = linha.substring(b * 12, b * 12 + 11);
            int dv = digito(linha.charAt(b * 12 + 11));
            barras.append(bloco);
            if (dvArrecadacao(idValor, bloco) != dv) blocosOk = false;
        }
        String cb = barras.toString();
        boolean geralOk = dvArrecadacao(idValor, cb.substring(0, 3) + cb.substring(4)) == digito(cb.charAt(3));
        BigDecimal valor = null;
        if (idValor == '6' || idValor == '8') {
            long centavos = Long.parseLong(cb.substring(4, 15));
            if (centavos > 0) valor = BigDecimal.valueOf(centavos).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
        }
        return new Info(linha, cb, true, blocosOk && geralOk, valor, null);
    }

    static int dvArrecadacao(char idValor, String numero) {
        return (idValor == '6' || idValor == '7') ? mod10(numero) : mod11Arrecadacao(numero);
    }

    /** Monta a linha digitável (48) a partir do código de barras de arrecadação (44). */
    public static String linhaArrecadacao(String barras44) {
        char idValor = barras44.charAt(2);
        StringBuilder sb = new StringBuilder(48);
        for (int b = 0; b < 4; b++) {
            String bloco = barras44.substring(b * 11, b * 11 + 11);
            sb.append(bloco).append(dvArrecadacao(idValor, bloco));
        }
        return sb.toString();
    }

    /**
     * Gera um código de barras de arrecadação (44) com DV geral correto.
     * segmento: 5 = órgãos governamentais; idValor: 6/7 (módulo 10) ou 8/9 (módulo 11).
     */
    public static String gerarArrecadacao(char segmento, char idValor, BigDecimal valor, String orgao4, String campoLivre25) {
        String v = String.format("%011d", valor.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact());
        String semDv = "8" + segmento + idValor + v + orgao4 + campoLivre25;
        if (semDv.length() != 43) throw new IllegalArgumentException("Código de arrecadação com tamanho inválido");
        int dv = dvArrecadacao(idValor, semDv);
        return semDv.substring(0, 3) + dv + semDv.substring(3);
    }

    // ------------------------------------------------------------ boleto bancário

    private static Info boleto(String linha) {
        boolean ok = mod10(linha.substring(0, 9)) == digito(linha.charAt(9))
                && mod10(linha.substring(10, 20)) == digito(linha.charAt(20))
                && mod10(linha.substring(21, 31)) == digito(linha.charAt(31));
        String cb = linha.substring(0, 4) + linha.charAt(32) + linha.substring(33, 47)
                + linha.substring(4, 9) + linha.substring(10, 20) + linha.substring(21, 31);
        ok = ok && mod11Boleto(cb.substring(0, 4) + cb.substring(5)) == digito(cb.charAt(4));
        int fator = Integer.parseInt(linha.substring(33, 37));
        long centavos = Long.parseLong(linha.substring(37, 47));
        BigDecimal valor = centavos > 0 ? BigDecimal.valueOf(centavos).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY) : null;
        return new Info(linha, cb, false, ok, valor, fator >= 1000 ? vencimentoPorFator(fator, LocalDate.now()) : null);
    }

    static String linhaBoleto(String cb) {
        String c1 = cb.substring(0, 4) + cb.substring(19, 24);
        String c2 = cb.substring(24, 34);
        String c3 = cb.substring(34, 44);
        return c1 + mod10(c1) + c2 + mod10(c2) + c3 + mod10(c3) + cb.charAt(4) + cb.substring(5, 19);
    }

    /** O fator reinicia a cada 9000 dias: escolhe a data mais próxima da referência. */
    static LocalDate vencimentoPorFator(int fator, LocalDate referencia) {
        LocalDate antiga = BASE_FATOR.plusDays(fator);
        LocalDate nova = BASE_FATOR_2025.plusDays(fator - 1000L);
        long da = Math.abs(ChronoUnit.DAYS.between(referencia, antiga));
        long dn = Math.abs(ChronoUnit.DAYS.between(referencia, nova));
        return dn <= da ? nova : antiga;
    }

    // ------------------------------------------------------------ módulos

    static int mod10(String numero) {
        int soma = 0;
        int peso = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int p = digito(numero.charAt(i)) * peso;
            soma += p > 9 ? p / 10 + p % 10 : p;
            peso = peso == 2 ? 1 : 2;
        }
        return (10 - soma % 10) % 10;
    }

    static int mod11Arrecadacao(String numero) {
        int resto = somaMod11(numero) % 11;
        if (resto == 0 || resto == 1) return 0;
        if (resto == 10) return 1;
        return 11 - resto;
    }

    static int mod11Boleto(String numero) {
        int dv = 11 - somaMod11(numero) % 11;
        return (dv == 0 || dv == 10 || dv == 11) ? 1 : dv;
    }

    private static int somaMod11(String numero) {
        int soma = 0;
        int peso = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            soma += digito(numero.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        return soma;
    }

    private static int digito(char c) {
        return Character.digit(c, 10);
    }

    /** Linha digitável formatada em blocos, para exibição em documentos de exemplo. */
    public static String formatar(String linha) {
        if (linha == null) return null;
        if (linha.length() == 48) {
            StringBuilder sb = new StringBuilder();
            for (int b = 0; b < 4; b++) {
                if (b > 0) sb.append("  ");
                sb.append(linha, b * 12, b * 12 + 11).append('-').append(linha.charAt(b * 12 + 11));
            }
            return sb.toString();
        }
        if (linha.length() == 47) {
            return linha.substring(0, 5) + "." + linha.substring(5, 10) + " " + linha.substring(10, 15) + "." + linha.substring(15, 21)
                    + " " + linha.substring(21, 26) + "." + linha.substring(26, 32) + " " + linha.charAt(32) + " " + linha.substring(33);
        }
        return linha;
    }
}
