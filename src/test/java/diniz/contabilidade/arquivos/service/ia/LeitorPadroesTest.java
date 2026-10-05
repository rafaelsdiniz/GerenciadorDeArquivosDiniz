package diniz.contabilidade.arquivos.service.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/** Leitura por padrões (sem IA, sem Quarkus): textos de exemplo de guias e documentos. */
@DisplayName("Leitura inteligente — leitura por padrões")
class LeitorPadroesTest {

    /** CNPJ com dígitos verificadores válidos. */
    static final String CNPJ_VALIDO = "11222333000181";

    static final String DAS = """
            Documento de Arrecadação do Simples Nacional
            CNPJ Razão Social
            11.222.333/0001-81 PADARIA MODELO LTDA
            Período de Apuração Data de Vencimento Número do Documento
            Setembro/2026 20/10/2026 07.20.26278.4521739-6
            Pagar este documento até 20/10/2026
            Valor Total do Documento
            1.847,32
            1006 INSS/CPP - SIMPLES NACIONAL 766,64
            1007 ICMS - SIMPLES NACIONAL 628,08
            """;

    @Test
    @DisplayName("DAS: tipo, CNPJ, competência por extenso, vencimento, valor e razão social")
    void das() {
        Leitura l = LeitorPadroes.ler(DAS, null);
        assertEquals(TipoDocumento.DAS, l.tipo);
        assertEquals(CNPJ_VALIDO, l.cnpj);
        assertEquals("09/2026", l.competencia);
        assertEquals(LocalDate.of(2026, 10, 20), l.vencimento);
        assertEquals(new BigDecimal("1847.32"), l.valor);
        assertEquals("PADARIA MODELO LTDA", l.razaoSocial);
        assertEquals("07.20.26278.4521739-6", l.numeroDocumento);
        assertEquals("DAS — Simples Nacional — competência 09/2026", l.descricao);
    }

    @Test
    @DisplayName("DARF: período de apuração em data completa vira MM/aaaa")
    void darf() {
        String texto = """
                Ministério da Fazenda — Receita Federal
                Documento de Arrecadação de Receitas Federais DARF
                01 Nome/Telefone: TRANSPORTES EXEMPLO LTDA
                02 Período de Apuração 31/08/2026
                03 Número do CPF ou CNPJ 11.222.333/0001-81
                04 Código da Receita 2089
                06 Data de Vencimento 30/09/2026
                10 Valor Total R$ 3.250,10
                """;
        Leitura l = LeitorPadroes.ler(texto, null);
        assertEquals(TipoDocumento.DARF, l.tipo);
        assertEquals("08/2026", l.competencia);
        assertEquals(LocalDate.of(2026, 9, 30), l.vencimento);
        assertEquals(new BigDecimal("3250.10"), l.valor);
        assertEquals(CNPJ_VALIDO, l.cnpj);
    }

    @Test
    @DisplayName("FGTS: competência MM/aaaa, total a recolher e linha digitável válida")
    void fgtsComLinhaDigitavel() {
        String barras = CodigoBarras.gerarArrecadacao('5', '6', new BigDecimal("1076.00"), "0179", "0126090123456789261007002");
        String linha = CodigoBarras.formatar(CodigoBarras.linhaArrecadacao(barras));
        String texto = "GFD — Guia do FGTS Digital\nFundo de Garantia\nCompetência: 09/2026\nData de vencimento: 07/10/2026\n"
                + "Total a recolher R$ 1.076,00\nLinha digitável\n" + linha;
        Leitura l = LeitorPadroes.ler(texto, null);
        assertEquals(TipoDocumento.FGTS, l.tipo);
        assertEquals("09/2026", l.competencia);
        assertEquals(LocalDate.of(2026, 10, 7), l.vencimento);
        assertTrue(l.linhaValida, "DV da linha digitável deveria conferir");
        assertEquals(48, l.linhaDigitavel.length());
        assertEquals(new BigDecimal("1076.00"), l.valor);
    }

    @Test
    @DisplayName("Valor codificado na linha digitável prevalece sobre o texto")
    void valorDaLinhaPrevalece() {
        String barras = CodigoBarras.gerarArrecadacao('5', '8', new BigDecimal("999.99"), "0328", "0000000000000001261020001");
        String texto = "DAS Simples Nacional\nValor total do documento 1.000,00\n" + CodigoBarras.linhaArrecadacao(barras);
        assertEquals(new BigDecimal("999.99"), LeitorPadroes.ler(texto, null).valor);
    }

    @Test
    @DisplayName("Linha digitável com dígito errado é marcada como inválida")
    void linhaInvalida() {
        String barras = CodigoBarras.gerarArrecadacao('5', '8', new BigDecimal("10.00"), "0328", "0000000000000001261020001");
        String linha = CodigoBarras.linhaArrecadacao(barras);
        String errada = linha.substring(0, 20) + (linha.charAt(20) == '9' ? '0' : (char) (linha.charAt(20) + 1)) + linha.substring(21);
        Leitura l = LeitorPadroes.ler("Guia DARF\n" + errada, null);
        assertNotNull(l.linhaDigitavel);
        assertFalse(l.linhaValida);
    }

    @Test
    @DisplayName("NF-e em XML: tipo, valor com ponto decimal e CNPJs")
    void nfeXml() {
        String xml = "<?xml version=\"1.0\"?><nfeProc><NFe><infNFe Id=\"NFe1\"><ide><nNF>1234</nNF><dhEmi>2026-09-15T10:00:00-03:00</dhEmi></ide>"
                + "<emit><CNPJ>11222333000181</CNPJ><xNome>FORNECEDOR EXEMPLO LTDA</xNome></emit>"
                + "<dest><CNPJ>22222222000102</CNPJ></dest><total><ICMSTot><vNF>532.90</vNF></ICMSTot></total></infNFe></NFe></nfeProc>";
        ExtratorTexto.Texto t = ExtratorTexto.extrair(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8), "nota.xml");
        Leitura l = LeitorPadroes.ler(t.conteudo(), "22222222000102");
        assertEquals(TipoDocumento.NFE, l.tipo);
        assertEquals(new BigDecimal("532.90"), l.valor);
        assertTrue(l.cnpjs.contains("11222333000181"));
        assertEquals("22222222000102", l.cnpj, "deveria preferir o CNPJ da empresa");
        assertEquals("1234", l.numeroDocumento);
    }

    @Test
    @DisplayName("CNPJ: valida dígitos verificadores")
    void cnpjValido() {
        assertTrue(LeitorPadroes.cnpjValido("11.222.333/0001-81"));
        assertFalse(LeitorPadroes.cnpjValido("11.222.333/0001-82"));
        assertFalse(LeitorPadroes.cnpjValido("11111111111111"));
        assertFalse(LeitorPadroes.cnpjValido("123"));
    }

    @Test
    @DisplayName("CNPJ sem máscara e com DV inválido só é aceito se for o CNPJ cadastrado da empresa")
    void cnpjInvalidoDaEmpresa() {
        String texto = "Guia DAS Simples Nacional CNPJ 22222222000102 valor total R$ 10,00";
        assertNull(LeitorPadroes.ler(texto, null).cnpj);
        assertEquals("22222222000102", LeitorPadroes.ler(texto, "22222222000102").cnpj);
        // com a máscara completa a evidência é forte o bastante
        assertEquals("22222222000102", LeitorPadroes.ler("CNPJ 22.222.222/0001-02", null).cnpj);
    }

    @Test
    @DisplayName("Competência: normaliza formatos variados")
    void competencia() {
        assertEquals("09/2026", LeitorPadroes.normalizarCompetencia("9/2026"));
        assertEquals("09/2026", LeitorPadroes.normalizarCompetencia("2026-09"));
        assertEquals("09/2026", LeitorPadroes.normalizarCompetencia("Setembro/2026"));
        assertEquals("03/2026", LeitorPadroes.normalizarCompetencia("março de 2026"));
        assertNull(LeitorPadroes.normalizarCompetencia("13/2026"));
        assertNull(LeitorPadroes.normalizarCompetencia(null));
    }

    @Test
    @DisplayName("Tipo por palavras-chave: DAS cita INSS/ICMS mas continua DAS; certidão; extrato")
    void tipos() {
        assertEquals(TipoDocumento.DAS, LeitorPadroes.detectarTipo(LeitorPadroes.normalizar(DAS)));
        assertEquals(TipoDocumento.CERTIDAO, LeitorPadroes.detectarTipo("certidao negativa de debitos relativos aos tributos federais"));
        assertEquals(TipoDocumento.EXTRATO_BANCARIO, LeitorPadroes.detectarTipo("extrato de conta corrente saldo anterior 1.000,00"));
        assertEquals(TipoDocumento.OUTRO, LeitorPadroes.detectarTipo("lista de compras do mercado"));
    }

    @Test
    @DisplayName("Boleto bancário (47 dígitos): valida e decodifica valor e vencimento pelo fator")
    void boleto() {
        // código de barras de boleto montado com DV geral correto
        String semDv = "0019" + "1000" + "0000012345" + "0000002803601300000000017";
        String cb = semDv.substring(0, 4) + CodigoBarras.mod11Boleto(semDv) + semDv.substring(4);
        CodigoBarras.Info info = CodigoBarras.analisar(CodigoBarras.linhaBoleto(cb));
        assertNotNull(info);
        assertTrue(info.valido());
        assertEquals(new BigDecimal("123.45"), info.valor());
        assertEquals(LocalDate.of(2025, 2, 22), info.vencimento());
    }

    @Test
    @DisplayName("Texto vazio não quebra a leitura")
    void vazio() {
        Leitura l = LeitorPadroes.ler("  ", "22222222000102");
        assertEquals(TipoDocumento.OUTRO, l.tipo);
        assertNull(l.valor);
    }
}
