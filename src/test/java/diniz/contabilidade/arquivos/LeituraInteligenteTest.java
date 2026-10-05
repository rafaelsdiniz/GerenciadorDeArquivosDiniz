package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.service.ia.GeradorExemplos;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.restassured.path.json.JsonPath;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Leitura inteligente de documentos pela API (sem IA nos testes: leitura por padrões) e o isolamento
 * por empresa nos endpoints /ia.
 */
@QuarkusTest
@DisplayName("Leitura inteligente de documentos (/ia)")
class LeituraInteligenteTest {

    @Inject
    DadosTeste dados;

    Empresa cliente;
    Usuario funcionario;
    Pasta pasta;
    ObrigacaoPendente das;
    String tokenCliente;
    String tokenOutroCliente;
    LocalDate vencimento;
    byte[] guiaDas;

    @BeforeEach
    void cenario() throws Exception {
        cliente = dados.empresa("Padaria Teste");
        funcionario = dados.funcionario(cliente);
        pasta = dados.pasta(cliente);
        vencimento = LocalDate.now().plusDays(10);
        das = dados.pendenteDoEscritorio(cliente, vencimento, StatusObrigacao.PENDENTE);
        tokenCliente = TokenTeste.de(funcionario);
        tokenOutroCliente = TokenTeste.de(dados.funcionario(dados.empresa("Outra Empresa")));
        guiaDas = new GeradorExemplos().das(new GeradorExemplos.Dados(cliente.getRazaoSocial(), cliente.getCnpj().getNumero(),
                YearMonth.from(vencimento).minusMonths(1), vencimento, new BigDecimal("1847.32")));
    }

    @Test
    @DisplayName("Status: sem chave configurada usa a leitura por padrões")
    void status() {
        given().auth().oauth2(tokenCliente).when().get("/ia/status").then()
                .statusCode(200)
                .body("configurada", equalTo(false))
                .body("leituraPorPadroes", equalTo(true));
    }

    @Test
    @DisplayName("Analisar antes do envio: lê a guia, confere com a obrigação e não grava nada")
    void analisarSemGravar() {
        JsonPath r = given().auth().oauth2(tokenCliente)
                .multiPart("arquivo", "guia-das.pdf", guiaDas, "application/pdf")
                .multiPart("idObrigacaoPendente", String.valueOf(das.getId()))
                .when().post("/ia/analisar").then()
                .statusCode(200)
                .body("tipoDocumento", equalTo("DAS"))
                .body("categoriaFiscalSugerida", equalTo("DAS"))
                .body("valor", equalTo(1847.32f))
                .body("cnpjConfere", equalTo(true))
                .body("fonte", equalTo("PADROES"))
                .body("textoLegivel", equalTo(true))
                .extract().jsonPath();
        assertEquals(vencimento.toString(), r.getString("vencimento"));
        assertEquals(48, r.getString("linhaDigitavel").length());
        assertTrue(r.getList("alertas").isEmpty(), "sem divergências: " + r.getList("alertas"));

        given().auth().oauth2(tokenCliente).when().get("/arquivos/obrigacao/" + das.getId()).then().body("$", hasSize(0));
    }

    @Test
    @DisplayName("Isolamento: cliente não lê documentos no contexto de outra empresa (404)")
    void isolamento() {
        given().auth().oauth2(tokenOutroCliente)
                .multiPart("arquivo", "guia.pdf", guiaDas, "application/pdf")
                .multiPart("idObrigacaoPendente", String.valueOf(das.getId()))
                .when().post("/ia/analisar").then().statusCode(404);
        given().auth().oauth2(tokenOutroCliente)
                .multiPart("arquivo", "guia.pdf", guiaDas, "application/pdf")
                .multiPart("idEmpresa", String.valueOf(cliente.getId()))
                .when().post("/ia/analisar").then().statusCode(404);

        Arquivo arquivo = dados.arquivo(cliente, funcionario, pasta);
        given().auth().oauth2(tokenOutroCliente).when().post("/ia/arquivos/" + arquivo.getId() + "/analisar").then().statusCode(404);
        given().auth().oauth2(tokenOutroCliente).queryParam("idEmpresa", cliente.getId())
                .when().get("/ia/exemplos/" + GeradorExemplos.DAS).then().statusCode(404);
        given().when().get("/ia/status").then().statusCode(401);
    }

    @Test
    @DisplayName("Arquivo armazenado sem texto: responde com o aviso, sem erro")
    void arquivoSemTexto() {
        Arquivo arquivo = dados.arquivo(cliente, funcionario, pasta);
        given().auth().oauth2(tokenCliente).when().post("/ia/arquivos/" + arquivo.getId() + "/analisar").then()
                .statusCode(200)
                .body("analise.textoLegivel", equalTo(false))
                .body("analise.alertas[0].codigo", equalTo("SEM_TEXTO"))
                .body("arquivo.analisadoEm", notNullValue());
    }

    @Test
    @DisplayName("Guia anexada à obrigação é lida automaticamente: valor e linha digitável no arquivo e na obrigação")
    void leituraAutomaticaNoUpload() throws Exception {
        int idArquivo = given().auth().oauth2(tokenCliente)
                .multiPart("arquivo", "guia-das.pdf", guiaDas, "application/pdf")
                .multiPart("idEmpresa", String.valueOf(cliente.getId()))
                .multiPart("idUsuario", String.valueOf(funcionario.getId()))
                .multiPart("idPasta", String.valueOf(pasta.getId()))
                .multiPart("idObrigacaoPendente", String.valueOf(das.getId()))
                .when().post("/arquivos").then().statusCode(201)
                .extract().path("id");

        // a leitura roda em segundo plano depois do commit
        JsonPath arquivo = null;
        for (int i = 0; i < 50; i++) {
            arquivo = given().auth().oauth2(tokenCliente).when().get("/arquivos/" + idArquivo).then().statusCode(200).extract().jsonPath();
            if (arquivo.get("analisadoEm") != null) break;
            Thread.sleep(100);
        }
        assertEquals(1847.32f, arquivo.getFloat("valor"));
        assertEquals("DAS", arquivo.getString("tipoDocumento"));
        assertEquals(48, arquivo.getString("linhaDigitavel").length());

        given().auth().oauth2(tokenCliente).when().get("/obrigacoes-pendentes/" + das.getId()).then()
                .statusCode(200).body("valorGuia", equalTo(1847.32f));
    }

    @Test
    @DisplayName("Guia de exemplo: PDF fictício gerado para a empresa do usuário")
    void exemplo() {
        byte[] pdf = given().auth().oauth2(tokenCliente).when().get("/ia/exemplos/" + GeradorExemplos.FGTS).then()
                .statusCode(200).extract().asByteArray();
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF"));
        given().auth().oauth2(tokenCliente).when().get("/ia/exemplos/qualquer.pdf").then().statusCode(404);
    }
}
