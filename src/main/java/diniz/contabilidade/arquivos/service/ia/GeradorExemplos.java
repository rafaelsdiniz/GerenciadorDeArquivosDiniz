package diniz.contabilidade.arquivos.service.ia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Gera guias FICTÍCIAS (DAS e FGTS Digital) em PDF com texto, para demonstrar a leitura inteligente.
 * Linha digitável com dígitos verificadores válidos e o valor codificado nela.
 * Todas trazem a tarja "EXEMPLO FICTÍCIO — NÃO PAGAR".
 */
public final class GeradorExemplos {

    public static final String DAS = "guia-das-exemplo.pdf";
    public static final String FGTS = "guia-fgts-exemplo.pdf";

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String[] MESES = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    private static final float W = PDRectangle.A4.getWidth();
    private static final float H = PDRectangle.A4.getHeight();
    private static final float M = 42;

    private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public record Dados(String razaoSocial, String cnpj14, YearMonth competencia, LocalDate vencimento, BigDecimal valor) {}

    // ------------------------------------------------------------------ DAS

    public byte[] das(Dados d) throws IOException {
        BigDecimal total = d.valor().setScale(2, RoundingMode.HALF_UP);
        String numero = String.format("07.20.%02d%03d.%07d-%d", d.competencia().getYear() % 100, d.competencia().getMonthValue() * 37 % 1000,
                Math.abs((d.cnpj14() + d.competencia()).hashCode()) % 10_000_000, d.competencia().getMonthValue() % 10);
        String barras = CodigoBarras.gerarArrecadacao('5', '8', total, "0328",
                String.format("%016d", Math.abs((long) numero.hashCode()) % 10_000_000_000_000_000L).substring(0, 16)
                        + d.vencimento().format(DateTimeFormatter.ofPattern("yyMMdd")) + "001");
        String linha = CodigoBarras.linhaArrecadacao(barras);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream c = new PDPageContentStream(doc, page)) {
                float y = H - M;
                tarja(c, y);
                y -= 34;
                caixa(c, M, y - 46, W - 2 * M, 46);
                texto(c, negrito, 13, M + 12, y - 20, "Documento de Arrecadação do Simples Nacional");
                texto(c, normal, 9, M + 12, y - 35, "Receita Federal do Brasil  ·  PGDAS-D  ·  Simples Nacional");
                y -= 62;

                y = linhaRotulos(c, y, new String[]{"CNPJ", "Razão Social"}, new float[]{0, 150});
                y = linhaValores(c, y, new String[]{formatarCnpj(d.cnpj14()), d.razaoSocial().toUpperCase(PT_BR)}, new float[]{0, 150});
                y -= 8;
                y = linhaRotulos(c, y, new String[]{"Período de Apuração", "Data de Vencimento", "Número do Documento"}, new float[]{0, 150, 300});
                y = linhaValores(c, y, new String[]{MESES[d.competencia().getMonthValue() - 1] + "/" + d.competencia().getYear(),
                        d.vencimento().format(BR), numero}, new float[]{0, 150, 300});
                y -= 10;

                caixa(c, M, y - 40, W - 2 * M, 40);
                texto(c, negrito, 11, M + 12, y - 16, "Pagar este documento até " + d.vencimento().format(BR));
                texto(c, normal, 9, M + 12, y - 31, "Observações: valor calculado conforme declaração transmitida (PGDAS-D).");
                texto(c, normal, 8, W - M - 150, y - 14, "Valor Total do Documento");
                texto(c, negrito, 15, W - M - 150, y - 32, moeda(total));
                y -= 60;

                texto(c, negrito, 10, M, y, "Composição do Documento de Arrecadação");
                y -= 16;
                String[] cab = {"Código", "Denominação", "Principal", "Multa", "Juros", "Total"};
                float[] xs = {0, 50, 270, 345, 400, 455};
                y = linhaRotulos(c, y, cab, xs);
                String[][] tributos = {
                        {"1001", "IRPJ - SIMPLES NACIONAL", "0.055"},
                        {"1002", "CSLL - SIMPLES NACIONAL", "0.035"},
                        {"1004", "COFINS - SIMPLES NACIONAL", "0.1274"},
                        {"1005", "PIS/PASEP - SIMPLES NACIONAL", "0.0276"},
                        {"1006", "INSS/CPP - SIMPLES NACIONAL", "0.415"},
                        {"1007", "ICMS - SIMPLES NACIONAL", "0.34"}};
                BigDecimal acumulado = BigDecimal.ZERO;
                for (int i = 0; i < tributos.length; i++) {
                    BigDecimal parte = i == tributos.length - 1 ? total.subtract(acumulado)
                            : total.multiply(new BigDecimal(tributos[i][2])).setScale(2, RoundingMode.HALF_UP);
                    acumulado = acumulado.add(parte);
                    y = linhaValores(c, y, new String[]{tributos[i][0], tributos[i][1], numeroBr(parte), "0,00", "0,00", numeroBr(parte)}, xs, 9);
                }
                y -= 4;
                c.moveTo(M, y + 8); c.lineTo(W - M, y + 8); c.stroke();
                y = linhaValores(c, y, new String[]{"", "Totais", numeroBr(total), "0,00", "0,00", numeroBr(total)}, xs, 9);
                y -= 30;

                rodapeBarras(c, y, linha, "Autenticação mecânica — DAS");
            }
            doc.getDocumentInformation().setTitle("DAS (exemplo fictício) — " + d.razaoSocial());
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------ FGTS Digital (GFD)

    public byte[] fgts(Dados d) throws IOException {
        BigDecimal total = d.valor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remuneracao = total.multiply(new BigDecimal("12.5")).setScale(2, RoundingMode.HALF_UP);
        String identificador = String.format("01%02d%02d%010d", d.competencia().getYear() % 100, d.competencia().getMonthValue(),
                Math.abs((long) d.cnpj14().hashCode()) % 10_000_000_000L);
        String barras = CodigoBarras.gerarArrecadacao('5', '6', total, "0179", identificador.substring(0, 16)
                + d.vencimento().format(DateTimeFormatter.ofPattern("yyMMdd")) + "002");
        String linha = CodigoBarras.linhaArrecadacao(barras);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream c = new PDPageContentStream(doc, page)) {
                float y = H - M;
                tarja(c, y);
                y -= 34;
                caixa(c, M, y - 46, W - 2 * M, 46);
                texto(c, negrito, 13, M + 12, y - 20, "GFD — Guia do FGTS Digital");
                texto(c, normal, 9, M + 12, y - 35, "Fundo de Garantia do Tempo de Serviço  ·  Ministério do Trabalho e Emprego");
                y -= 62;

                y = linhaRotulos(c, y, new String[]{"Nome do Empregador", "CNPJ"}, new float[]{0, 320});
                y = linhaValores(c, y, new String[]{d.razaoSocial().toUpperCase(PT_BR), formatarCnpj(d.cnpj14())}, new float[]{0, 320});
                y -= 8;
                y = linhaRotulos(c, y, new String[]{"Competência", "Data de Vencimento", "Identificador da Guia"}, new float[]{0, 150, 320});
                y = linhaValores(c, y, new String[]{String.format("%02d/%d", d.competencia().getMonthValue(), d.competencia().getYear()),
                        d.vencimento().format(BR), identificador}, new float[]{0, 150, 320});
                y -= 14;

                texto(c, negrito, 10, M, y, "Resumo do recolhimento");
                y -= 16;
                float[] xs = {0, 330};
                y = linhaValores(c, y, new String[]{"Quantidade de trabalhadores", "6"}, xs, 9);
                y = linhaValores(c, y, new String[]{"Remuneração (base de cálculo)", moeda(remuneracao)}, xs, 9);
                y = linhaValores(c, y, new String[]{"FGTS mensal (8%)", moeda(total)}, xs, 9);
                y = linhaValores(c, y, new String[]{"Encargos (multa e juros)", moeda(BigDecimal.ZERO.setScale(2))}, xs, 9);
                y -= 10;

                caixa(c, M, y - 40, W - 2 * M, 40);
                texto(c, negrito, 11, M + 12, y - 16, "Pagável até " + d.vencimento().format(BR) + " em qualquer banco ou via PIX");
                texto(c, normal, 9, M + 12, y - 31, "Guia emitida pelo empregador no FGTS Digital.");
                texto(c, normal, 8, W - M - 150, y - 14, "Total a recolher");
                texto(c, negrito, 15, W - M - 150, y - 32, moeda(total));
                y -= 70;

                rodapeBarras(c, y, linha, "Autenticação mecânica — FGTS Digital");
            }
            doc.getDocumentInformation().setTitle("GFD FGTS (exemplo fictício) — " + d.razaoSocial());
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------ desenho

    private void tarja(PDPageContentStream c, float y) throws IOException {
        c.setNonStrokingColor(0.98f, 0.93f, 0.80f);
        c.addRect(M, y - 22, W - 2 * M, 22);
        c.fill();
        c.setNonStrokingColor(0.45f, 0.30f, 0.05f);
        texto(c, negrito, 9, M + 10, y - 15, "EXEMPLO FICTÍCIO — gerado pelo Gerenciador Diniz para demonstrar a leitura inteligente. NÃO PAGAR.");
        c.setNonStrokingColor(0f, 0f, 0f);
    }

    private void rodapeBarras(PDPageContentStream c, float y, String linha, String rotulo) throws IOException {
        texto(c, normal, 8, M, y, "Linha digitável");
        texto(c, negrito, 12, M, y - 16, CodigoBarras.formatar(linha));
        // barras ilustrativas (não codificam o código real)
        float x = M;
        float base = y - 70;
        for (int i = 0; i < linha.length() * 3 && x < W - M; i++) {
            int dig = Character.digit(linha.charAt(i % linha.length()), 10);
            float largura = 0.8f + (dig % 3) * 0.9f;
            if (i % 2 == 0) {
                c.addRect(x, base, largura, 40);
            }
            x += largura + 0.9f;
        }
        c.fill();
        texto(c, normal, 7, M, base - 12, rotulo);
    }

    private float linhaRotulos(PDPageContentStream c, float y, String[] rotulos, float[] xs) throws IOException {
        c.setNonStrokingColor(0.35f, 0.38f, 0.45f);
        for (int i = 0; i < rotulos.length; i++) texto(c, normal, 8, M + xs[i], y, rotulos[i]);
        c.setNonStrokingColor(0f, 0f, 0f);
        return y - 13;
    }

    private float linhaValores(PDPageContentStream c, float y, String[] valores, float[] xs) throws IOException {
        return linhaValores(c, y, valores, xs, 11);
    }

    private float linhaValores(PDPageContentStream c, float y, String[] valores, float[] xs, float tamanho) throws IOException {
        for (int i = 0; i < valores.length; i++) texto(c, tamanho >= 11 ? negrito : normal, tamanho, M + xs[i], y, valores[i]);
        return y - (tamanho + 6);
    }

    private void caixa(PDPageContentStream c, float x, float y, float w, float h) throws IOException {
        c.setStrokingColor(0.6f, 0.62f, 0.68f);
        c.setLineWidth(0.7f);
        c.addRect(x, y, w, h);
        c.stroke();
    }

    private void texto(PDPageContentStream c, PDType1Font f, float tamanho, float x, float y, String s) throws IOException {
        c.beginText();
        c.setFont(f, tamanho);
        c.newLineAtOffset(x, y);
        c.showText(s);
        c.endText();
    }

    // ------------------------------------------------------------------ formatação

    static String moeda(BigDecimal v) {
        return "R$ " + numeroBr(v);
    }

    static String numeroBr(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return f.format(v);
    }

    static String formatarCnpj(String c) {
        String d = c.replaceAll("\\D", "");
        if (d.length() != 14) return c;
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    /** Gera os arquivos de exemplo estáticos (Padaria Pão Quente, competência 09/2026). */
    public static void main(String[] args) throws IOException {
        String pasta = args.length > 0 ? args[0] : ".";
        GeradorExemplos g = new GeradorExemplos();
        Dados das = new Dados("Pão Quente Comércio LTDA", "22222222000102", YearMonth.of(2026, 9), LocalDate.of(2026, 10, 20), new BigDecimal("1847.32"));
        Dados fgts = new Dados("Pão Quente Comércio LTDA", "22222222000102", YearMonth.of(2026, 9), LocalDate.of(2026, 10, 7), new BigDecimal("1076.00"));
        java.nio.file.Files.write(java.nio.file.Path.of(pasta, DAS), g.das(das));
        java.nio.file.Files.write(java.nio.file.Path.of(pasta, FGTS), g.fgts(fgts));
    }
}
