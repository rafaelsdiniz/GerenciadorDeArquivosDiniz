package diniz.contabilidade.arquivos.service.ia;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extrai o texto de um arquivo para a leitura inteligente.
 *
 * PDF (PDFBox, até 15 páginas), XML (vira linhas "tag: valor"), TXT/CSV (UTF-8 ou Latin-1),
 * DOCX/XLSX/ODT (texto do XML interno). Imagens e PDFs escaneados não têm texto: devolve vazio.
 */
public final class ExtratorTexto {

    static final int MAX_PAGINAS = 15;
    static final int MAX_CARACTERES = 200_000;

    private static final Pattern TAG_COM_VALOR = Pattern.compile("<(?:[\\w.-]+:)?([\\w.-]+)(?:\\s[^>]*)?>([^<>]+)</");
    private static final Pattern RAIZ_XML = Pattern.compile("<((?:[\\w.-]+:)?[\\w.-]+)[\\s>]");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private ExtratorTexto() {}

    /** Resultado: texto (pode ser vazio) e o formato reconhecido. */
    public record Texto(String conteudo, String formato) {
        public boolean vazio() {
            return conteudo == null || conteudo.isBlank() || conteudo.replaceAll("\\s", "").length() < 20;
        }
    }

    public static Texto extrair(byte[] conteudo, String nomeArquivo) {
        if (conteudo == null || conteudo.length == 0) return new Texto("", "VAZIO");
        String ext = extensao(nomeArquivo);
        try {
            if (ext.equals("pdf") || comecaCom(conteudo, "%PDF")) return new Texto(limitar(pdf(conteudo)), "PDF");
            if (ext.equals("xml") || comecaCom(conteudo, "<?xml")) return new Texto(limitar(xml(decodificar(conteudo))), "XML");
            if (ext.equals("txt") || ext.equals("csv") || ext.equals("ofx") || ext.equals("html") || ext.equals("htm")) {
                String t = decodificar(conteudo);
                if (ext.startsWith("htm")) t = TAGS.matcher(t).replaceAll(" ");
                return new Texto(limitar(t), "TEXTO");
            }
            if (ext.equals("docx")) return new Texto(limitar(zip(conteudo, "word/document.xml")), "DOCX");
            if (ext.equals("xlsx")) return new Texto(limitar(zip(conteudo, "xl/sharedStrings.xml")), "XLSX");
            if (ext.equals("odt") || ext.equals("ods")) return new Texto(limitar(zip(conteudo, "content.xml")), "ODF");
            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp")) {
                return new Texto("", "IMAGEM");
            }
        } catch (Exception | LinkageError e) {
            return new Texto("", "ILEGIVEL");
        }
        return new Texto("", "NAO_SUPORTADO");
    }

    private static String pdf(byte[] conteudo) throws IOException {
        try (PDDocument doc = Loader.loadPDF(conteudo)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(doc.getNumberOfPages(), MAX_PAGINAS));
            return stripper.getText(doc);
        }
    }

    /** XML → "tag: valor" por linha (compacto e legível para os padrões e para a IA). */
    static String xml(String xml) {
        StringBuilder sb = new StringBuilder();
        String inicio = xml.length() > 600 ? xml.substring(0, 600) : xml;
        Matcher m0 = RAIZ_XML.matcher(inicio.replaceFirst("<\\?xml[^>]*>", ""));
        if (m0.find()) sb.append("documento-xml: ").append(m0.group(1)).append('\n');
        Matcher m = TAG_COM_VALOR.matcher(xml);
        while (m.find() && sb.length() < MAX_CARACTERES) {
            String valor = m.group(2).trim();
            if (valor.isEmpty()) continue;
            sb.append(m.group(1)).append(": ").append(valor).append('\n');
        }
        if (sb.length() < 30) return TAGS.matcher(xml).replaceAll(" ");
        return sb.toString();
    }

    private static String zip(byte[] conteudo, String entrada) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(conteudo))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entrada)) {
                    String xml = new String(zis.readNBytes(MAX_CARACTERES * 4), StandardCharsets.UTF_8);
                    xml = xml.replaceAll("</w:p>|</text:p>|</si>|</row>", "\n").replaceAll("<w:tab/>|</c>", " ");
                    return TAGS.matcher(xml).replaceAll("");
                }
            }
        }
        return "";
    }

    static String decodificar(byte[] conteudo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo)).toString();
        } catch (CharacterCodingException e) {
            return new String(conteudo, StandardCharsets.ISO_8859_1);
        }
    }

    private static String limitar(String t) {
        if (t == null) return "";
        t = t.replace(' ', ' ').replace("\r", "");
        return t.length() > MAX_CARACTERES ? t.substring(0, MAX_CARACTERES) : t;
    }

    private static boolean comecaCom(byte[] b, String prefixo) {
        byte[] p = prefixo.getBytes(StandardCharsets.US_ASCII);
        int inicio = 0;
        // ignora BOM / espaços iniciais
        while (inicio < b.length && inicio < 8 && (b[inicio] == (byte) 0xEF || b[inicio] == (byte) 0xBB || b[inicio] == (byte) 0xBF
                || b[inicio] == ' ' || b[inicio] == '\n' || b[inicio] == '\r' || b[inicio] == '\t')) inicio++;
        if (b.length - inicio < p.length) return false;
        for (int i = 0; i < p.length; i++) if (b[inicio + i] != p[i]) return false;
        return true;
    }

    static String extensao(String nome) {
        if (nome == null) return "";
        int i = nome.lastIndexOf('.');
        return i < 0 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT).trim();
    }
}
