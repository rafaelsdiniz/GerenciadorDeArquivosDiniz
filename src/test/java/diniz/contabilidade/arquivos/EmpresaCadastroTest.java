package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Cadastro e exclusão de empresas")
class EmpresaCadastroTest {

    @Inject
    DadosTeste dados;

    String tokenAdmin;

    @BeforeEach
    void cenario() {
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    private Map<String, Object> empresa(String cnpj) {
        return Map.of(
                "nomeFantasia", "Loja Centro",
                "razaoSocial", "Loja Centro Comércio LTDA",
                "cnpj", cnpj,
                "telefone", "(63) 98888-7777",
                "email", "loja" + System.nanoTime() + "@teste.com");
    }

    @Test
    @DisplayName("CNPJ digitado com máscara é gravado só com os dígitos")
    void cnpjComMascaraEhNormalizado() {
        String digitos = DadosTeste.novoCnpj();
        String formatado = digitos.substring(0, 2) + "." + digitos.substring(2, 5) + "." + digitos.substring(5, 8)
                + "/" + digitos.substring(8, 12) + "-" + digitos.substring(12);

        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(empresa(formatado))
                .when().post("/empresas")
                .then().statusCode(201)
                .body("cnpj", equalTo(digitos))
                .body("telefone", equalTo("63988887777"));
    }

    @Test
    @DisplayName("CNPJ inválido é recusado com 400 e mensagem clara")
    void cnpjInvalidoEhRecusado() {
        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(empresa("123"))
                .when().post("/empresas")
                .then().statusCode(400)
                .body("mensagem", containsString("CNPJ inválido"));
    }

    @Test
    @DisplayName("Excluir empresa com dados vinculados responde 400 explicando o que impede")
    void exclusaoComDependentesEhBloqueada() {
        Empresa empresa = dados.empresa("Empresa Com Dados");
        dados.funcionario(empresa);
        dados.pasta(empresa);

        given().auth().oauth2(tokenAdmin)
                .when().delete("/empresas/" + empresa.getId())
                .then().statusCode(400)
                .body("mensagem", equalTo("Não é possível excluir: a empresa ainda tem 1 pasta(s), 1 usuário(s). "
                        + "Remova ou transfira esses dados antes."));

        given().auth().oauth2(tokenAdmin)
                .when().get("/empresas/" + empresa.getId())
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Excluir empresa sem dados vinculados responde 204 e ela deixa de existir")
    void exclusaoSemDependentes() {
        Empresa empresa = dados.empresa("Empresa Vazia");

        given().auth().oauth2(tokenAdmin)
                .when().delete("/empresas/" + empresa.getId())
                .then().statusCode(204);

        given().auth().oauth2(tokenAdmin)
                .when().get("/empresas/" + empresa.getId())
                .then().statusCode(404);
    }
}
