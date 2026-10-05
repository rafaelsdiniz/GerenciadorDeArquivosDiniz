package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

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

@QuarkusTest
@DisplayName("Conta do usuário: login, perfil e troca de senha")
class ContaUsuarioTest {

    @Inject
    DadosTeste dados;

    Usuario usuario;
    String token;

    @BeforeEach
    void cenario() {
        Empresa empresa = dados.empresa("Padaria Pão Quente");
        usuario = dados.funcionario(empresa);
        token = TokenTeste.de(usuario);
    }

    private String email() {
        return usuario.getEmail().getEndereco();
    }

    @Test
    @DisplayName("Login com e-mail e senha corretos devolve um token JWT")
    void loginComSucesso() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email(), "senha", DadosTeste.SENHA_PADRAO))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("Login com senha errada responde 401")
    void loginComSenhaErrada() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email(), "senha", "outra-senha"))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("Requisição sem token responde 401")
    void semToken() {
        given().when().get("/usuarios/me").then().statusCode(401);
    }

    @Test
    @DisplayName("GET /usuarios/me devolve o próprio usuário logado")
    void meuPerfil() {
        given().auth().oauth2(token)
                .when().get("/usuarios/me")
                .then().statusCode(200)
                .body("id", equalTo(usuario.getId().intValue()))
                .body("email", equalTo(email()))
                .body("perfilUsuario", equalTo("FUNCIONARIO"))
                .body("idEmpresa", equalTo(usuario.getEmpresa().getId().intValue()));
    }

    @Test
    @DisplayName("Trocar a senha informando a senha atual errada responde 400 com mensagem clara")
    void trocarSenhaComSenhaAtualErrada() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("senhaAtual", "nao-e-essa", "novaSenha", "nova-senha-forte"))
                .when().put("/usuarios/me/senha")
                .then().statusCode(400)
                .body("mensagem", equalTo("A senha atual está incorreta."));
    }

    @Test
    @DisplayName("Nova senha com menos de 8 caracteres é recusada (400)")
    void trocarSenhaCurta() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("senhaAtual", DadosTeste.SENHA_PADRAO, "novaSenha", "123"))
                .when().put("/usuarios/me/senha")
                .then().statusCode(400)
                .body(containsString("mínimo 8 caracteres"));
    }

    @Test
    @DisplayName("Troca de senha correta responde 204 e o login passa a aceitar só a nova senha")
    void trocarSenhaComSucesso() {
        String novaSenha = "nova-senha-forte";

        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("senhaAtual", DadosTeste.SENHA_PADRAO, "novaSenha", novaSenha))
                .when().put("/usuarios/me/senha")
                .then().statusCode(204);

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email(), "senha", novaSenha))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", not(emptyOrNullString()));

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email(), "senha", DadosTeste.SENHA_PADRAO))
                .when().post("/auth/login")
                .then().statusCode(401);
    }
}
