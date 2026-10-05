package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.repository.ComunicacaoDecRepository;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.DecTestProfile;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;

/**
 * Integração (somente leitura) com o DEC Monitor, usando um servidor DEC falso
 * (FakeDecServer) que exige "Authorization: Bearer" como o real.
 */
@QuarkusTest
@TestProfile(DecTestProfile.class)
@DisplayName("Integração com o DEC (Domicílio Eletrônico do Contribuinte)")
class DecIntegracaoTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @ConfigProperty(name = "dec.url")
    String decUrl;

    @Inject
    ComunicacaoDecRepository comunicacoes;

    @Inject
    DadosTeste dados;

    @Inject
    ObjectMapper json;

    Empresa cliente;
    Usuario funcionarioCliente;
    String tokenAdmin;

    @BeforeEach
    void cenario() throws Exception {
        controlarDec("/teste/reset", "");
        QuarkusTransaction.requiringNew().run(() -> comunicacoes.deleteAll());

        cliente = dados.empresa("Distribuidora Tocantins");
        funcionarioCliente = dados.funcionario(cliente);
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    // ------------------------------------------------------------------ apoio

    /** Comunicação no formato da API de integração do DEC Monitor. */
    private Map<String, Object> item(String id, String cnpj, String status) {
        LocalDate disponibilizada = LocalDate.now().minusDays(3);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("cnpj", cnpj);
        m.put("razaoSocial", "Contribuinte " + cnpj);
        m.put("numero", "360137");
        m.put("tipo", "NOTIFICACAO");
        m.put("assunto", "Notificação de divergência na EFD");
        m.put("corpo", "Texto integral da notificação.");
        m.put("remetente", "SEFAZ-TO");
        m.put("disponibilizadaEm", disponibilizada.toString());
        m.put("cienteEm", null);
        m.put("prazoCienciaEm", disponibilizada.plusDays(10).toString());
        m.put("diasParaResposta", 30);
        m.put("coletadaEm", "2026-10-01T15:00:30Z");
        m.put("status", status);
        m.put("urgencia", "ALTA");
        m.put("motivo", "Notificação sem ciência.");
        m.put("diasRestantes", 7);
        m.put("cienciaTacitaEm", disponibilizada.plusDays(10).toString());
        m.put("prazoRespostaEm", disponibilizada.plusDays(40).toString());
        m.put("encerrada", false);
        m.put("temInteiroTeor", true);
        m.put("link", "https://dec.exemplo/comunicacao/" + id);
        return m;
    }

    private void decDevolve(List<Map<String, Object>> itens) throws Exception {
        controlarDec("/teste/itens", json.writeValueAsString(itens));
    }

    private void controlarDec(String rota, String corpo) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(decUrl + rota))
                .POST(HttpRequest.BodyPublishers.ofString(corpo)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, r.statusCode(), "falha ao controlar o DEC falso");
    }

    private JsonPath sincronizar() {
        return given().auth().oauth2(tokenAdmin)
                .when().post("/dec/sincronizar")
                .then().statusCode(200)
                .extract().jsonPath();
    }

    private Map<String, Object> comunicacao(String idExterno) {
        List<Map<String, Object>> todas = given().auth().oauth2(tokenAdmin)
                .when().get("/dec/comunicacoes")
                .then().statusCode(200)
                .extract().jsonPath().getList("findAll { it.idExterno == '" + idExterno + "' }");
        assertEquals(1, todas.size(), "deve existir exatamente uma comunicação " + idExterno);
        return todas.get(0);
    }

    private static String formatar(String cnpj) {
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }

    // ------------------------------------------------------------------ testes

    @Test
    @DisplayName("Primeira sincronização grava as comunicações recebidas como novas")
    void primeiraSincronizacao() throws Exception {
        decDevolve(List.of(
                item("dec-1", cliente.getCnpj().getNumero(), "NOVA"),
                item("dec-2", DadosTeste.novoCnpj(), "NOVA")));

        JsonPath r = sincronizar();

        assertEquals(2, r.getInt("recebidas"));
        assertEquals(2, r.getInt("novas"));
        assertEquals(0, r.getInt("atualizadas"));
        assertEquals(1, r.getInt("semEmpresa"));
    }

    @Test
    @DisplayName("Sincronizar de novo atualiza por id externo (upsert), sem duplicar")
    void segundaSincronizacaoAtualizaSemDuplicar() throws Exception {
        String cnpj = cliente.getCnpj().getNumero();
        decDevolve(List.of(item("dec-1", cnpj, "NOVA"), item("dec-2", cnpj, "NOVA")));
        sincronizar();

        decDevolve(List.of(item("dec-1", cnpj, "RESOLVIDA"), item("dec-2", cnpj, "NOVA")));
        JsonPath r = sincronizar();

        assertEquals(0, r.getInt("novas"));
        assertEquals(2, r.getInt("atualizadas"));
        assertEquals(2, comunicacoes.count());
        assertEquals("RESOLVIDA", comunicacao("dec-1").get("status"));
    }

    @Test
    @DisplayName("Comunicação é ligada à empresa pelos dígitos do CNPJ; sem cadastro fica sem empresa")
    void vinculoPorCnpj() throws Exception {
        // o DEC pode mandar o CNPJ com máscara; a empresa guarda só os dígitos
        decDevolve(List.of(
                item("dec-cliente", formatar(cliente.getCnpj().getNumero()), "NOVA"),
                item("dec-desconhecida", DadosTeste.novoCnpj(), "NOVA")));
        sincronizar();

        Map<String, Object> doCliente = comunicacao("dec-cliente");
        assertEquals(cliente.getId().intValue(), doCliente.get("idEmpresa"));
        assertEquals(cliente.getCnpj().getNumero(), doCliente.get("cnpj"));
        assertEquals("2026-10-01T12:00:30", doCliente.get("coletadaEm"), "instante convertido para o fuso de Tocantins");

        assertNull(comunicacao("dec-desconhecida").get("idEmpresa"));

        given().auth().oauth2(tokenAdmin)
                .when().get("/dec/status")
                .then().statusCode(200)
                .body("configurada", equalTo(true))
                .body("semEmpresa", equalTo(1))
                .body("escritorio", equalTo("Diniz Assessoria Contábil"));
    }

    @Test
    @DisplayName("Funcionário vê só as comunicações da própria empresa e não pode sincronizar (403)")
    void funcionarioVeSoAsSuas() throws Exception {
        Empresa outra = dados.empresa("Outra Empresa");
        decDevolve(List.of(
                item("dec-minha", cliente.getCnpj().getNumero(), "NOVA"),
                item("dec-outra", outra.getCnpj().getNumero(), "NOVA"),
                item("dec-sem-cadastro", DadosTeste.novoCnpj(), "NOVA")));
        sincronizar();
        String token = TokenTeste.de(funcionarioCliente);

        given().auth().oauth2(token)
                .when().get("/dec/comunicacoes")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("idExterno", everyItem(equalTo("dec-minha")));

        given().auth().oauth2(token)
                .when().get("/dec/comunicacoes/empresa/" + outra.getId())
                .then().statusCode(404);

        given().auth().oauth2(token)
                .when().post("/dec/sincronizar")
                .then().statusCode(403);

        // o cliente vê apenas se a integração está ligada, sem detalhes do escritório
        given().auth().oauth2(token)
                .when().get("/dec/status")
                .then().statusCode(200)
                .body("configurada", equalTo(true))
                .body("$", not(hasKey("semEmpresa")));
    }

    @Test
    @DisplayName("Se o DEC recusa a chave, a sincronização responde 502 com mensagem amigável")
    void tokenRecusadoPeloDec() throws Exception {
        controlarDec("/teste/token", "outro-token");

        given().auth().oauth2(tokenAdmin)
                .when().post("/dec/sincronizar")
                .then().statusCode(502)
                .body("mensagem", equalTo("O DEC recusou a chave (401). Confira DEC_TOKEN e INTEGRACAO_TOKEN."));

        given().auth().oauth2(tokenAdmin)
                .when().get("/dec/status")
                .then().statusCode(200)
                .body("ultimoErro", equalTo("O DEC recusou a chave (401). Confira DEC_TOKEN e INTEGRACAO_TOKEN."));
    }

    @Test
    @DisplayName("Cadastrar empresa com o CNPJ liga as comunicações já recebidas; excluí-la desfaz o vínculo")
    void cadastroEExclusaoDeEmpresaAtualizamVinculo() throws Exception {
        String cnpj = DadosTeste.novoCnpj();
        decDevolve(List.of(item("dec-futuro-cliente", cnpj, "NOVA")));
        sincronizar();
        assertNull(comunicacao("dec-futuro-cliente").get("idEmpresa"));

        Map<String, Object> novaEmpresa = Map.of(
                "nomeFantasia", "Novo Cliente", "razaoSocial", "Novo Cliente LTDA",
                "cnpj", formatar(cnpj), "telefone", "63999998888", "email", "novo" + cnpj + "@teste.com");
        int idEmpresa = given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(novaEmpresa)
                .when().post("/empresas")
                .then().statusCode(201)
                .extract().path("id");

        given().auth().oauth2(tokenAdmin)
                .when().get("/dec/comunicacoes/empresa/" + idEmpresa)
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].idExterno", equalTo("dec-futuro-cliente"));

        given().auth().oauth2(tokenAdmin)
                .when().delete("/empresas/" + idEmpresa)
                .then().statusCode(204);

        assertNull(comunicacao("dec-futuro-cliente").get("idEmpresa"));
        given().auth().oauth2(tokenAdmin)
                .when().get("/dec/status")
                .then().body("semEmpresa", equalTo(1));
    }

    @Test
    @DisplayName("Empresa com usuários não é excluída (400), mesmo tendo comunicações do DEC")
    void exclusaoBloqueadaPorDependentes() throws Exception {
        decDevolve(List.of(item("dec-1", cliente.getCnpj().getNumero(), "NOVA")));
        sincronizar();

        given().auth().oauth2(tokenAdmin)
                .when().delete("/empresas/" + cliente.getId())
                .then().statusCode(400)
                .body("mensagem", equalTo("Não é possível excluir: a empresa ainda tem 1 usuário(s). "
                        + "Remova ou transfira esses dados antes."));

        assertEquals(cliente.getId().intValue(), comunicacao("dec-1").get("idEmpresa"));
    }
}
