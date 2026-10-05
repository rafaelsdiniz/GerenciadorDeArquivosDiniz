package diniz.contabilidade.arquivos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.support.DadosTeste;
import diniz.contabilidade.arquivos.support.TokenTeste;
import diniz.contabilidade.arquivos.support.Upload;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;

/**
 * Ciclo de vida de uma obrigação: entrega (por upload da guia), pagamento,
 * reabertura e as regras de quem pode fazer o quê.
 */
@QuarkusTest
@DisplayName("Fluxo de obrigações: entrega, pagamento e reabertura")
class ObrigacaoFluxoTest {

    LocalDate hoje;

    @Inject
    DadosTeste dados;

    Empresa empresa;
    Usuario funcionario;
    Pasta pasta;
    String tokenFuncionario;
    String tokenAdmin;

    @BeforeEach
    void cenario() {
        hoje = LocalDate.now();
        empresa = dados.empresa("Farmácia Saúde");
        funcionario = dados.funcionario(empresa);
        pasta = dados.pasta(empresa);
        tokenFuncionario = TokenTeste.de(funcionario);
        tokenAdmin = TokenTeste.de(dados.admin(dados.empresa("Escritório Diniz")));
    }

    private ObrigacaoPendente obrigacao(LocalDate vencimento, StatusObrigacao status) {
        return dados.pendenteDoEscritorio(empresa, vencimento, status);
    }

    private ValidatableResponse consultar(ObrigacaoPendente o) {
        return given().auth().oauth2(tokenFuncionario)
                .when().get("/obrigacoes-pendentes/" + o.getId())
                .then().statusCode(200);
    }

    private ValidatableResponse enviarGuia(ObrigacaoPendente o, String nome) {
        return Upload.enviar(tokenFuncionario, empresa.getId(), funcionario.getId(), pasta.getId(), o.getId(), nome);
    }

    // ------------------------------------------------------------------ entrega por upload

    @Test
    @DisplayName("Upload vinculado à obrigação marca ENTREGUE com a data de hoje e conta 1 arquivo")
    void uploadMarcaObrigacaoComoEntregue() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(10), StatusObrigacao.PENDENTE);

        enviarGuia(o, "guia-das.pdf")
                .statusCode(201)
                .body("idObrigacaoPendente", equalTo(o.getId().intValue()))
                .body("status", equalTo("ENTREGUE"))
                .body("dataVencimento", equalTo(o.getDataVencimento().toString()));

        consultar(o)
                .body("status", equalTo("ENTREGUE"))
                .body("dataEntrega", equalTo(hoje.toString()))
                .body("totalArquivos", equalTo(1));

        given().auth().oauth2(tokenFuncionario)
                .when().get("/arquivos/obrigacao/" + o.getId())
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nomeOriginal", equalTo("guia-das.pdf"));
    }

    @Test
    @DisplayName("Novo anexo em obrigação já entregue mantém a data de entrega original")
    void segundoUploadMantemDataDeEntrega() {
        // a fixture registra a entrega como feita ontem
        ObrigacaoPendente o = obrigacao(hoje.plusDays(10), StatusObrigacao.ENTREGUE);
        LocalDate entregaOriginal = hoje.minusDays(1);

        enviarGuia(o, "guia.pdf").statusCode(201);
        enviarGuia(o, "comprovante.pdf").statusCode(201);

        consultar(o)
                .body("status", equalTo("ENTREGUE"))
                .body("dataEntrega", equalTo(entregaOriginal.toString()))
                .body("totalArquivos", equalTo(2));
    }

    // ------------------------------------------------------------------ reabertura

    @Test
    @DisplayName("ADMIN reabre obrigação no prazo: volta a PENDENTE e limpa entrega e pagamento")
    void adminReabreObrigacaoNoPrazo() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);
        given().auth().oauth2(tokenFuncionario).when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(200);

        given().auth().oauth2(tokenAdmin)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/reabrir")
                .then().statusCode(200)
                .body("status", equalTo("PENDENTE"))
                .body("dataEntrega", nullValue())
                .body("dataPagamento", nullValue())
                .body("situacaoPagamento", equalTo("NAO_SE_APLICA"));
    }

    @Test
    @DisplayName("ADMIN reabre obrigação com prazo vencido: volta como VENCIDA")
    void adminReabreObrigacaoVencida() {
        ObrigacaoPendente o = obrigacao(hoje.minusDays(3), StatusObrigacao.ENTREGUE);

        given().auth().oauth2(tokenAdmin)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/reabrir")
                .then().statusCode(200)
                .body("status", equalTo("VENCIDA"));
    }

    @Test
    @DisplayName("Funcionário não pode reabrir obrigação (403)")
    void funcionarioNaoReabre() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);

        given().auth().oauth2(tokenFuncionario)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/reabrir")
                .then().statusCode(403);

        consultar(o).body("status", equalTo("ENTREGUE"));
    }

    // ------------------------------------------------------------------ pagamento

    @Test
    @DisplayName("Funcionário da empresa confirma o pagamento da guia: situação PAGO")
    void funcionarioConfirmaPagamento() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);

        given().auth().oauth2(tokenFuncionario)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(200)
                .body("situacaoPagamento", equalTo("PAGO"))
                .body("dataPagamento", equalTo(hoje.toString()));
    }

    @Test
    @DisplayName("Pagamento pode ser confirmado com uma data passada informada")
    void pagamentoComDataInformada() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);
        LocalDate ontem = hoje.minusDays(1);

        given().auth().oauth2(tokenFuncionario).queryParam("data", ontem.toString())
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(200)
                .body("dataPagamento", equalTo(ontem.toString()));
    }

    @Test
    @DisplayName("Funcionário de outra empresa não confirma pagamento (404)")
    void pagamentoPorOutraEmpresaEhNegado() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);
        String tokenOutroCliente = TokenTeste.de(dados.funcionario(dados.empresa("Outro Cliente")));

        given().auth().oauth2(tokenOutroCliente)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Pagamento antes da entrega da guia é recusado (400)")
    void pagamentoAntesDaEntrega() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.PENDENTE);

        given().auth().oauth2(tokenFuncionario)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(400)
                .body("mensagem", equalTo("Só é possível confirmar o pagamento depois que a guia foi entregue."));
    }

    @Test
    @DisplayName("Pagamento com data no futuro é recusado (400)")
    void pagamentoComDataFutura() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);

        given().auth().oauth2(tokenFuncionario).queryParam("data", hoje.plusDays(1).toString())
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(400)
                .body("mensagem", equalTo("A data de pagamento não pode ser no futuro."));
    }

    @Test
    @DisplayName("Obrigação de responsabilidade do cliente não tem guia para pagar (400)")
    void pagamentoEmObrigacaoDoCliente() {
        var recorrente = dados.recorrente(empresa, Periodicidade.MENSAL, 10, ResponsavelObrigacao.CLIENTE);
        ObrigacaoPendente o = dados.pendente(recorrente, hoje.plusDays(5), StatusObrigacao.ENTREGUE);

        consultar(o).body("situacaoPagamento", equalTo("NAO_SE_APLICA"));

        given().auth().oauth2(tokenFuncionario)
                .when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(400)
                .body("mensagem", equalTo("Esta obrigação é de envio de documentos pelo cliente; não tem guia para pagar."));
    }

    @Test
    @DisplayName("Desfazer pagamento é exclusivo do ADMIN; depois volta a AGUARDANDO")
    void desfazerPagamentoSoAdmin() {
        ObrigacaoPendente o = obrigacao(hoje.plusDays(5), StatusObrigacao.ENTREGUE);
        given().auth().oauth2(tokenFuncionario).when().patch("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(200);

        given().auth().oauth2(tokenFuncionario)
                .when().delete("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(403);

        given().auth().oauth2(tokenAdmin)
                .when().delete("/obrigacoes-pendentes/" + o.getId() + "/pagamento")
                .then().statusCode(200)
                .body("dataPagamento", nullValue())
                .body("situacaoPagamento", equalTo("AGUARDANDO"));
    }

    @Test
    @DisplayName("Guia entregue e não paga: AGUARDANDO dentro do prazo, ATRASADO depois do vencimento")
    void situacaoPagamentoConformeVencimento() {
        consultar(obrigacao(hoje.plusDays(3), StatusObrigacao.ENTREGUE))
                .body("situacaoPagamento", equalTo("AGUARDANDO"));

        consultar(obrigacao(hoje.minusDays(3), StatusObrigacao.ENTREGUE))
                .body("situacaoPagamento", equalTo("ATRASADO"));

        consultar(obrigacao(hoje.plusDays(3), StatusObrigacao.PENDENTE))
                .body("situacaoPagamento", equalTo("NAO_SE_APLICA"));
    }
}
