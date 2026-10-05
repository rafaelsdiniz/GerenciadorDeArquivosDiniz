package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.service.ObrigacaoPendenteService;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Regras de geração das obrigações a partir do calendário (recorrentes) e do
 * cálculo da competência exibida ao cliente.
 */
@QuarkusTest
@DisplayName("Obrigações: competência e geração automática")
class ObrigacaoPendenteServiceTest {

    @Inject
    ObrigacaoPendenteService service;

    @Inject
    DadosTeste dados;

    Empresa empresa;
    String tokenAdmin;

    @BeforeEach
    void cenario() {
        empresa = dados.empresa("Construtora Alfa");
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    private String competencia(Periodicidade periodicidade, LocalDate vencimento) {
        ObrigacaoRecorrente rec = dados.recorrente(empresa, periodicidade, vencimento.getDayOfMonth(),
                ResponsavelObrigacao.ESCRITORIO);
        ObrigacaoPendente p = dados.pendente(rec, vencimento, StatusObrigacao.PENDENTE);
        return service.buscarPorId(p.getId()).competencia();
    }

    /** Próximo vencimento mensal estritamente depois de hoje (dia limitado ao tamanho do mês). */
    private static LocalDate proximoVencimentoMensal(int dia) {
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.from(hoje);
        LocalDate candidato = mes.atDay(Math.min(dia, mes.lengthOfMonth()));
        if (!candidato.isAfter(hoje)) {
            mes = mes.plusMonths(1);
            candidato = mes.atDay(Math.min(dia, mes.lengthOfMonth()));
        }
        return candidato;
    }

    // ------------------------------------------------------------------ competência

    @Test
    @DisplayName("Mensal: competência é o mês anterior ao vencimento (vence 20/10/2026 → 09/2026)")
    void competenciaMensal() {
        assertEquals("09/2026", competencia(Periodicidade.MENSAL, LocalDate.of(2026, 10, 20)));
    }

    @Test
    @DisplayName("Mensal com vencimento em janeiro: competência é dezembro do ano anterior")
    void competenciaMensalViraOAno() {
        assertEquals("12/2026", competencia(Periodicidade.MENSAL, LocalDate.of(2027, 1, 20)));
    }

    @Test
    @DisplayName("Anual: competência é o ano anterior ao vencimento (vence 30/04/2027 → 2026)")
    void competenciaAnual() {
        assertEquals("2026", competencia(Periodicidade.ANUAL, LocalDate.of(2027, 4, 30)));
    }

    // ------------------------------------------------------------------ geração

    @Test
    @DisplayName("gerarProxima é idempotente: chamar duas vezes cria uma única ocorrência")
    void gerarProximaEhIdempotente() {
        ObrigacaoRecorrente rec = dados.recorrente(empresa, Periodicidade.MENSAL, 15, ResponsavelObrigacao.ESCRITORIO);

        assertTrue(service.gerarProxima(rec), "a primeira chamada deve criar a ocorrência");
        assertFalse(service.gerarProxima(rec), "a segunda chamada não deve duplicar");
        assertEquals(1, dados.contarPendentes(rec));
    }

    @Test
    @DisplayName("Geração em lote (POST /obrigacoes-pendentes/gerar) não duplica ao rodar de novo")
    void geracaoEmLoteEhIdempotente() {
        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).when().post("/obrigacoes-pendentes/gerar")
                .then().statusCode(200);

        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).when().post("/obrigacoes-pendentes/gerar")
                .then().statusCode(200)
                .body("criadas", equalTo(0));
    }

    @Test
    @DisplayName("Cadastrar recorrente ativa já gera a próxima pendência, com responsável ESCRITORIO por padrão")
    void cadastrarRecorrenteAtivaGeraProximaPendencia() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("idEmpresa", empresa.getId());
        corpo.put("nome", "FGTS");
        corpo.put("periodicidade", "MENSAL");
        corpo.put("diaVencimento", 20);

        int idRecorrente = given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(corpo)
                .when().post("/obrigacoes-recorrentes")
                .then().statusCode(201)
                .body("ativo", equalTo(true))
                .body("responsavel", equalTo("ESCRITORIO"))
                .extract().path("id");

        given().auth().oauth2(tokenAdmin)
                .when().get("/obrigacoes-pendentes/empresa/" + empresa.getId())
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].idObrigacaoRecorrente", equalTo(idRecorrente))
                .body("[0].nomeObrigacao", equalTo("FGTS"))
                .body("[0].status", equalTo("PENDENTE"))
                .body("[0].responsavel", equalTo("ESCRITORIO"))
                .body("[0].dataVencimento", equalTo(proximoVencimentoMensal(20).toString()));
    }

    @Test
    @DisplayName("Cadastrar recorrente inativa não gera pendência")
    void cadastrarRecorrenteInativaNaoGera() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("idEmpresa", empresa.getId());
        corpo.put("nome", "DEFIS");
        corpo.put("periodicidade", "ANUAL");
        corpo.put("diaVencimento", 31);
        corpo.put("ativo", false);

        given().auth().oauth2(tokenAdmin).contentType(ContentType.JSON).body(corpo)
                .when().post("/obrigacoes-recorrentes")
                .then().statusCode(201);

        given().auth().oauth2(tokenAdmin)
                .when().get("/obrigacoes-pendentes/empresa/" + empresa.getId())
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Funcionário não altera o calendário de obrigações (403)")
    void funcionarioNaoCadastraRecorrente() {
        String tokenFuncionario = TokenTeste.de(dados.funcionario(empresa));
        Map<String, Object> corpo = Map.of("idEmpresa", empresa.getId(), "nome", "DAS",
                "periodicidade", "MENSAL", "diaVencimento", 20);

        given().auth().oauth2(tokenFuncionario).contentType(ContentType.JSON).body(corpo)
                .when().post("/obrigacoes-recorrentes")
                .then().statusCode(403);
    }
}
