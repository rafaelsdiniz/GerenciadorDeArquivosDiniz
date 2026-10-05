package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Conversa escritório x cliente dentro da obrigação: envio, leitura, contadores e isolamento. */
@QuarkusTest
@DisplayName("Mensagens por obrigação")
class MensagemObrigacaoTest {

    @Inject
    DadosTeste dados;

    Empresa empresa;
    ObrigacaoPendente obrigacao;
    String tokenCliente;
    String tokenAdmin;
    String tokenOutroCliente;

    @BeforeEach
    void cenario() {
        empresa = dados.empresa("Padaria Teste");
        obrigacao = dados.pendenteDoEscritorio(empresa, LocalDate.now().plusDays(10), StatusObrigacao.PENDENTE);
        tokenCliente = TokenTeste.de(dados.funcionario(empresa));
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
        tokenOutroCliente = TokenTeste.de(dados.funcionario(dados.empresa("Outra Empresa")));
    }

    private String url() {
        return "/mensagens/obrigacao/" + obrigacao.getId();
    }

    private String chave() {
        return "porObrigacao.'" + obrigacao.getId() + "'";
    }

    @Test
    @DisplayName("Mensagem do cliente fica não lida para o escritório até um ADMIN abrir a conversa")
    void clienteEnviaEscritorioLe() {
        given().auth().oauth2(tokenCliente).contentType(ContentType.JSON)
                .body(Map.of("texto", "  A guia do DAS deste mês já está disponível?  "))
                .when().post(url())
                .then().statusCode(201)
                .body("texto", equalTo("A guia do DAS deste mês já está disponível?"))
                .body("minha", equalTo(true))
                .body("autorPerfil", equalTo("FUNCIONARIO"))
                .body("lidaPeloDestinatario", equalTo(false));

        given().auth().oauth2(tokenAdmin).when().get("/mensagens/nao-lidas")
                .then().statusCode(200).body(chave(), equalTo(1));
        // o próprio autor não tem nada a ler
        given().auth().oauth2(tokenCliente).when().get("/mensagens/nao-lidas")
                .then().statusCode(200).body("total", equalTo(0));

        given().auth().oauth2(tokenAdmin).when().get(url())
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].minha", equalTo(false))
                .body("[0].doEscritorio", equalTo(false));

        given().auth().oauth2(tokenAdmin).when().get("/mensagens/nao-lidas")
                .then().statusCode(200).body(chave(), nullValue());
    }

    @Test
    @DisplayName("Resposta do escritório conta como não lida para o cliente e aparece nas recentes")
    void escritorioRespondeClienteRecebe() {
        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON)
                .body(Map.of("texto", "Sim, já está anexada."))
                .when().post(url())
                .then().statusCode(201).body("autorPerfil", equalTo("ADMIN"));

        given().auth().oauth2(tokenCliente).when().get("/mensagens/nao-lidas")
                .then().statusCode(200).body("total", equalTo(1)).body(chave(), equalTo(1));

        given().auth().oauth2(tokenCliente).when().get("/mensagens/recentes?limite=5")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].idObrigacao", equalTo(obrigacao.getId().intValue()))
                .body("[0].naoLidas", equalTo(1))
                .body("[0].previa", equalTo("Sim, já está anexada."));

        given().auth().oauth2(tokenCliente).when().get(url()).then().statusCode(200);
        given().auth().oauth2(tokenCliente).when().get("/mensagens/nao-lidas")
                .then().statusCode(200).body("total", equalTo(0));
        // o escritório vê o "lida" na própria mensagem
        given().auth().oauth2(tokenAdmin).when().get(url())
                .then().statusCode(200).body("[0].lidaPeloDestinatario", equalTo(true));
    }

    @Test
    @DisplayName("Cliente de outra empresa recebe 404 e não vê a conversa nas recentes")
    void isolamentoPorEmpresa() {
        given().auth().oauth2(tokenCliente).contentType(ContentType.JSON)
                .body(Map.of("texto", "Mensagem interna")).when().post(url()).then().statusCode(201);

        given().auth().oauth2(tokenOutroCliente).when().get(url()).then().statusCode(404);
        given().auth().oauth2(tokenOutroCliente).contentType(ContentType.JSON)
                .body(Map.of("texto", "Olá")).when().post(url()).then().statusCode(404);
        given().auth().oauth2(tokenOutroCliente).when().get("/mensagens/recentes")
                .then().statusCode(200)
                .body("idObrigacao", everyItem(org.hamcrest.Matchers.not(obrigacao.getId().intValue())));
    }

    @Test
    @DisplayName("Texto vazio ou acima de 2000 caracteres é recusado com 400")
    void validacaoDoTexto() {
        given().auth().oauth2(tokenCliente).contentType(ContentType.JSON)
                .body(Map.of("texto", "   ")).when().post(url()).then().statusCode(400);
        given().auth().oauth2(tokenCliente).contentType(ContentType.JSON)
                .body(Map.of("texto", "x".repeat(2001))).when().post(url()).then().statusCode(400);
        given().auth().oauth2(tokenCliente).when().get(url()).then().statusCode(200).body("$", hasSize(0));
    }
}
