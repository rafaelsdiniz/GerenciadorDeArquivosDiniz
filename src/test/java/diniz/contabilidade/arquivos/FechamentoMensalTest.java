package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.service.FechamentoMensalService;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Quadro do fechamento mensal: criação automática por competência, indicadores,
 * mudança de etapa/responsável e acesso restrito ao escritório.
 */
@QuarkusTest
@DisplayName("Fechamento mensal")
class FechamentoMensalTest {

    @Inject
    DadosTeste dados;

    Empresa empresa;
    Usuario admin;
    Usuario funcionario;
    String tokenAdmin;
    String tokenFuncionario;
    YearMonth competencia;
    String comp;

    @BeforeEach
    void cenario() {
        empresa = dados.empresa("Padaria Fechamento");
        funcionario = dados.funcionario(empresa);
        admin = dados.admin(dados.empresa("Escritório Fechamento"));
        tokenAdmin = TokenTeste.de(admin);
        tokenFuncionario = TokenTeste.de(funcionario);
        competencia = YearMonth.now().minusMonths(1);
        comp = FechamentoMensalService.formatar(competencia);
    }

    private Map<String, Object> linhaDaEmpresa(String competencia) {
        List<Map<String, Object>> lista = given().auth().oauth2(tokenAdmin)
                .queryParam("competencia", competencia)
                .when().get("/fechamentos")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");
        List<Map<String, Object>> daEmpresa = lista.stream()
                .filter(m -> ((Number) m.get("idEmpresa")).longValue() == empresa.getId())
                .toList();
        assertEquals(1, daEmpresa.size(), "deve existir exatamente uma linha por empresa e competência");
        return daEmpresa.get(0);
    }

    @Test
    @DisplayName("GET cria a linha da empresa em 'Aguardando documentos' com prazo no dia 20 do mês seguinte, sem duplicar")
    void criaLinhasAutomaticamente() {
        Map<String, Object> linha = linhaDaEmpresa(comp);
        assertEquals("AGUARDANDO_DOCUMENTOS", linha.get("etapa"));
        assertEquals(comp, linha.get("competencia"));
        assertEquals(competencia.plusMonths(1).atDay(20).toString(), linha.get("prazo"));

        // segunda consulta não duplica
        Map<String, Object> deNovo = linhaDaEmpresa(comp);
        assertEquals(linha.get("id"), deNovo.get("id"));
    }

    @Test
    @DisplayName("Indicadores contam documentos do cliente e guias do escritório da competência")
    void indicadoresDaCompetencia() {
        ObrigacaoRecorrente docs = dados.recorrente(empresa, Periodicidade.MENSAL, 5, ResponsavelObrigacao.CLIENTE);
        // competência = mês anterior ao vencimento
        dados.pendente(docs, competencia.plusMonths(1).atDay(5), StatusObrigacao.PENDENTE);
        dados.pendenteDoEscritorio(empresa, competencia.plusMonths(1).atDay(20), StatusObrigacao.ENTREGUE);
        // outra competência: não conta
        dados.pendente(docs, competencia.atDay(5), StatusObrigacao.PENDENTE);

        @SuppressWarnings("unchecked")
        Map<String, Object> ind = (Map<String, Object>) linhaDaEmpresa(comp).get("indicadores");
        assertEquals(1, ind.get("documentosClienteTotal"));
        assertEquals(1, ind.get("documentosClientePendentes"));
        assertEquals(1, ind.get("obrigacoesEscritorioTotal"));
        assertEquals(1, ind.get("obrigacoesEscritorioEntregues"));
        assertEquals(1, ind.get("guiasAguardandoPagamento"));
    }

    @Test
    @DisplayName("PATCH muda etapa (concluidoEm só em CONCLUIDO), responsável e observação")
    void atualizaEtapaResponsavelEObservacao() {
        Number id = (Number) linhaDaEmpresa(comp).get("id");

        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON)
                .body(Map.of("etapa", "CONCLUIDO", "idResponsavel", admin.getId(), "observacao", "  Tudo certo  "))
                .when().patch("/fechamentos/" + id)
                .then().statusCode(200)
                .body("etapa", equalTo("CONCLUIDO"))
                .body("concluidoEm", notNullValue())
                .body("idResponsavel", equalTo(admin.getId().intValue()))
                .body("observacao", equalTo("Tudo certo"))
                .body("atrasado", equalTo(false));

        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON)
                .body(Map.of("etapa", "EM_APURACAO", "removerResponsavel", true, "observacao", ""))
                .when().patch("/fechamentos/" + id)
                .then().statusCode(200)
                .body("etapa", equalTo("EM_APURACAO"))
                .body("concluidoEm", nullValue())
                .body("idResponsavel", nullValue())
                .body("observacao", nullValue());
    }

    @Test
    @DisplayName("Responsável precisa ser do escritório (ADMIN)")
    void responsavelPrecisaSerAdmin() {
        Number id = (Number) linhaDaEmpresa(comp).get("id");
        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON)
                .body(Map.of("idResponsavel", funcionario.getId()))
                .when().patch("/fechamentos/" + id)
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Resumo traz contagem por etapa e competências oferecem a padrão")
    void resumoECompetencias() {
        linhaDaEmpresa(comp);
        Map<String, Object> resumo = given().auth().oauth2(tokenAdmin)
                .queryParam("competencia", comp)
                .when().get("/fechamentos/resumo")
                .then().statusCode(200)
                .body("competencia", equalTo(comp))
                .extract().jsonPath().getMap("$");
        assertNotNull(resumo.get("porEtapa"));
        assertTrue(((Number) resumo.get("total")).intValue() >= 1);

        given().auth().oauth2(tokenAdmin)
                .when().get("/fechamentos/competencias")
                .then().statusCode(200)
                .body("find { it.padrao == true }.competencia", equalTo(comp));
    }

    @Test
    @DisplayName("Competência inválida responde 400; cliente (FUNCIONARIO) não acessa")
    void validacaoEAcesso() {
        given().auth().oauth2(tokenAdmin).queryParam("competencia", "13/2026")
                .when().get("/fechamentos")
                .then().statusCode(400);

        given().auth().oauth2(tokenFuncionario)
                .when().get("/fechamentos")
                .then().statusCode(403);
    }
}
