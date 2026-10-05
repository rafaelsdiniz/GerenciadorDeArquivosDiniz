package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Socio;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import diniz.contabilidade.arquivos.support.Upload;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Regra central de segurança: o funcionário de um cliente só enxerga e altera dados
 * da própria empresa; registros de outra empresa respondem 404 (para não revelar que
 * existem) e ações do escritório respondem 403. O ADMIN (escritório) vê tudo.
 */
@QuarkusTest
@DisplayName("Isolamento de dados por empresa")
class IsolamentoPorEmpresaTest {

    @Inject
    DadosTeste dados;

    Empresa empresaA;
    Empresa empresaB;
    Usuario funcionarioA;
    Usuario funcionarioB;
    Pasta pastaA;
    Pasta pastaB;
    Arquivo arquivoA;
    Arquivo arquivoB;
    Socio socioA;
    Socio socioB;
    ObrigacaoPendente obrigacaoA;
    ObrigacaoPendente obrigacaoB;

    String tokenFuncionarioA;
    String tokenAdmin;

    @BeforeEach
    void cenario() {
        empresaA = dados.empresa("Mercado A");
        empresaB = dados.empresa("Oficina B");
        funcionarioA = dados.funcionario(empresaA);
        funcionarioB = dados.funcionario(empresaB);
        pastaA = dados.pasta(empresaA);
        pastaB = dados.pasta(empresaB);
        arquivoA = dados.arquivo(empresaA, funcionarioA, pastaA);
        arquivoB = dados.arquivo(empresaB, funcionarioB, pastaB);
        socioA = dados.socio(empresaA);
        socioB = dados.socio(empresaB);
        obrigacaoA = dados.pendenteDoEscritorio(empresaA, java.time.LocalDate.now().plusDays(10), StatusObrigacao.PENDENTE);
        obrigacaoB = dados.pendenteDoEscritorio(empresaB, java.time.LocalDate.now().plusDays(10), StatusObrigacao.PENDENTE);

        tokenFuncionarioA = TokenTeste.de(funcionarioA);
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    /** O JSON devolve ids pequenos como int. */
    private static int id(Long entidadeId) {
        return entidadeId.intValue();
    }

    private Map<String, Object> dadosEmpresa(Empresa e, String nome) {
        return Map.of(
                "nomeFantasia", nome,
                "razaoSocial", nome + " LTDA",
                "cnpj", e != null ? e.getCnpj().getNumero() : DadosTeste.novoCnpj(),
                "telefone", "(63) 3215-0000",
                "email", "contato" + System.nanoTime() + "@teste.com");
    }

    // ------------------------------------------------------------------ empresas

    @Test
    @DisplayName("Funcionário lista apenas a própria empresa")
    void funcionarioListaSoAPropriaEmpresa() {
        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/empresas")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(id(empresaA.getId())));
    }

    @Test
    @DisplayName("Funcionário recebe 404 ao abrir empresa de outro cliente (e 200 na própria)")
    void funcionarioNaoAbreEmpresaDeOutroCliente() {
        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/empresas/" + empresaB.getId())
                .then().statusCode(404);

        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/empresas/" + empresaA.getId())
                .then().statusCode(200)
                .body("nomeFantasia", equalTo("Mercado A"));
    }

    @Test
    @DisplayName("Funcionário não cadastra empresas (403) e não altera empresa de outro cliente (404)")
    void funcionarioNaoCadastraNemAlteraOutraEmpresa() {
        given().auth().oauth2(tokenFuncionarioA).contentType(ContentType.JSON)
                .body(dadosEmpresa(null, "Empresa Nova"))
                .when().post("/empresas")
                .then().statusCode(403);

        given().auth().oauth2(tokenFuncionarioA).contentType(ContentType.JSON)
                .body(dadosEmpresa(empresaB, "Nome Trocado"))
                .when().put("/empresas/" + empresaB.getId())
                .then().statusCode(404);

        given().auth().oauth2(tokenAdmin)
                .when().get("/empresas/" + empresaB.getId())
                .then().statusCode(200)
                .body("nomeFantasia", equalTo("Oficina B"));
    }

    @Test
    @DisplayName("Funcionário pode atualizar os dados cadastrais da própria empresa")
    void funcionarioAtualizaAPropriaEmpresa() {
        given().auth().oauth2(tokenFuncionarioA).contentType(ContentType.JSON)
                .body(dadosEmpresa(empresaA, "Mercado A Renovado"))
                .when().put("/empresas/" + empresaA.getId())
                .then().statusCode(200)
                .body("nomeFantasia", equalTo("Mercado A Renovado"));
    }

    @Test
    @DisplayName("ADMIN (escritório) enxerga todas as empresas")
    void adminVeTodasAsEmpresas() {
        given().auth().oauth2(tokenAdmin)
                .when().get("/empresas")
                .then().statusCode(200)
                .body("id", hasItems(id(empresaA.getId()), id(empresaB.getId())));
    }

    // ------------------------------------------------------------------ listas filtradas

    @Test
    @DisplayName("Lista de arquivos do funcionário traz só arquivos da própria empresa")
    void arquivosFiltradosPorEmpresa() {
        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/arquivos")
                .then().statusCode(200)
                .body("idEmpresa", everyItem(equalTo(id(empresaA.getId()))))
                .body("id", hasItem(id(arquivoA.getId())))
                .body("id", not(hasItem(id(arquivoB.getId()))));

        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/arquivos/" + arquivoB.getId())
                .then().statusCode(404);

        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/arquivos/" + arquivoB.getId() + "/download")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Lista de obrigações do funcionário traz só obrigações da própria empresa")
    void obrigacoesFiltradasPorEmpresa() {
        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/obrigacoes-pendentes")
                .then().statusCode(200)
                .body("idEmpresa", everyItem(equalTo(id(empresaA.getId()))))
                .body("id", hasItem(id(obrigacaoA.getId())))
                .body("id", not(hasItem(id(obrigacaoB.getId()))));

        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/obrigacoes-pendentes/" + obrigacaoB.getId())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Lista de sócios do funcionário traz só o quadro societário da própria empresa")
    void sociosFiltradosPorEmpresa() {
        given().auth().oauth2(tokenFuncionarioA)
                .when().get("/socios")
                .then().statusCode(200)
                .body("idEmpresa", everyItem(equalTo(id(empresaA.getId()))))
                .body("id", hasItem(id(socioA.getId())))
                .body("id", not(hasItem(id(socioB.getId()))));
    }

    // ------------------------------------------------------------------ upload

    @Test
    @DisplayName("Funcionário não consegue enviar arquivo para outra empresa (404)")
    void uploadParaOutraEmpresaEhNegado() {
        Upload.enviar(tokenFuncionarioA, empresaB.getId(), funcionarioA.getId(), pastaB.getId())
                .statusCode(404);

        given().auth().oauth2(tokenAdmin)
                .when().get("/arquivos/empresa/" + empresaB.getId())
                .then().statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    @DisplayName("Upload ignora o idUsuario enviado no formulário: o autor é sempre quem está logado")
    void uploadIgnoraIdUsuarioForjado() {
        Upload.enviar(tokenFuncionarioA, empresaA.getId(), funcionarioB.getId(), pastaA.getId())
                .statusCode(201)
                .body("idEmpresa", equalTo(id(empresaA.getId())))
                .body("idUsuario", equalTo(id(funcionarioA.getId())));
    }
}
