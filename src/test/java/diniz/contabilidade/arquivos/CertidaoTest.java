package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Certidões negativas: validade calculada (VALIDA / VENCENDO ≤ 15 dias / VENCIDA),
 * escrita só pelo escritório, leitura do cliente restrita à própria empresa e
 * anexo do PDF no Drive (pasta "Certidões").
 */
@QuarkusTest
@DisplayName("Certidões negativas")
class CertidaoTest {

    @Inject
    DadosTeste dados;

    Empresa empresaA;
    Empresa empresaB;
    String tokenAdmin;
    String tokenFuncionarioA;

    @BeforeEach
    void cenario() {
        empresaA = dados.empresa("Indústria A");
        empresaB = dados.empresa("Indústria B");
        Usuario funcionarioA = dados.funcionario(empresaA);
        tokenFuncionarioA = TokenTeste.de(funcionarioA);
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    private Map<String, Object> certidao(Empresa e, String tipo, LocalDate emissao, LocalDate validade) {
        Map<String, Object> m = new HashMap<>();
        m.put("idEmpresa", e.getId());
        m.put("tipo", tipo);
        m.put("situacao", "NEGATIVA");
        m.put("numero", "TESTE-" + System.nanoTime());
        m.put("dataEmissao", emissao != null ? emissao.toString() : null);
        m.put("dataValidade", validade.toString());
        return m;
    }

    private int criar(Map<String, Object> body) {
        return given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(body)
                .when().post("/certidoes")
                .then().statusCode(201).extract().path("id");
    }

    @Test
    @DisplayName("Calcula a situação da validade e os dias para vencer")
    void calculaStatusDeValidade() {
        LocalDate hoje = LocalDate.now();
        int valida = criar(certidao(empresaA, "FEDERAL", hoje.minusDays(10), hoje.plusDays(40)));
        int vencendo = criar(certidao(empresaA, "FGTS", hoje.minusDays(20), hoje.plusDays(15)));
        int vencida = criar(certidao(empresaA, "ESTADUAL", hoje.minusDays(70), hoje.minusDays(1)));

        given().auth().oauth2(tokenAdmin).when().get("/certidoes/" + valida)
                .then().statusCode(200).body("statusValidade", equalTo("VALIDA")).body("diasParaVencer", equalTo(40));
        given().auth().oauth2(tokenAdmin).when().get("/certidoes/" + vencendo)
                .then().statusCode(200).body("statusValidade", equalTo("VENCENDO")).body("diasParaVencer", equalTo(15));
        given().auth().oauth2(tokenAdmin).when().get("/certidoes/" + vencida)
                .then().statusCode(200).body("statusValidade", equalTo("VENCIDA")).body("nomeEmpresa", equalTo("Indústria A"));

        given().auth().oauth2(tokenAdmin).when().get("/certidoes/resumo?empresa=" + empresaA.getId())
                .then().statusCode(200)
                .body("total", equalTo(3)).body("validas", equalTo(1))
                .body("vencendo", equalTo(1)).body("vencidas", equalTo(1));
    }

    @Test
    @DisplayName("Cliente só vê as certidões da própria empresa e não altera nada")
    void clienteSoLeAsDaPropriaEmpresa() {
        LocalDate hoje = LocalDate.now();
        criar(certidao(empresaA, "FEDERAL", hoje, hoje.plusDays(180)));
        int daB = criar(certidao(empresaB, "FEDERAL", hoje, hoje.plusDays(180)));

        given().auth().oauth2(tokenFuncionarioA).when().get("/certidoes")
                .then().statusCode(200).body("$", hasSize(1)).body("idEmpresa", everyItem(equalTo(empresaA.getId().intValue())));
        given().auth().oauth2(tokenFuncionarioA).when().get("/certidoes/" + daB).then().statusCode(404);
        given().auth().oauth2(tokenFuncionarioA).when().get("/certidoes/empresa/" + empresaB.getId()).then().statusCode(404);
        given().auth().oauth2(tokenFuncionarioA).when().get("/certidoes/resumo")
                .then().statusCode(200).body("total", equalTo(1));

        given().auth().oauth2(tokenFuncionarioA).contentType(ContentType.JSON)
                .body(certidao(empresaA, "FGTS", hoje, hoje.plusDays(30)))
                .when().post("/certidoes").then().statusCode(403);
        given().auth().oauth2(tokenFuncionarioA).when().delete("/certidoes/" + daB).then().statusCode(403);
    }

    @Test
    @DisplayName("Rejeita validade anterior à emissão")
    void validaDatas() {
        LocalDate hoje = LocalDate.now();
        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON)
                .body(certidao(empresaA, "MUNICIPAL", hoje, hoje.minusDays(5)))
                .when().post("/certidoes").then().statusCode(400);
    }

    @Test
    @DisplayName("Anexa o PDF no Drive da empresa, na pasta Certidões")
    void anexaPdfNoDrive() {
        LocalDate hoje = LocalDate.now();
        int id = criar(certidao(empresaA, "TRABALHISTA", hoje, hoje.plusDays(180)));

        given().auth().oauth2(tokenAdmin)
                .multiPart("arquivo", "cndt.pdf", "%PDF-1.4 certidao".getBytes(StandardCharsets.UTF_8), "application/pdf")
                .when().post("/certidoes/" + id + "/anexo")
                .then().statusCode(200)
                .body("idArquivo", notNullValue())
                .body("nomeArquivo", equalTo("cndt.pdf"));

        given().auth().oauth2(tokenAdmin).when().get("/pastas/empresa/" + empresaA.getId())
                .then().statusCode(200).body("nome", org.hamcrest.Matchers.hasItem("Certidões"));

        given().auth().oauth2(tokenAdmin)
                .multiPart("arquivo", "planilha.xlsx", new byte[] { 1, 2, 3 }, "application/octet-stream")
                .when().post("/certidoes/" + id + "/anexo")
                .then().statusCode(400);
    }
}
