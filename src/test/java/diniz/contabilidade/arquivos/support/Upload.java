package diniz.contabilidade.arquivos.support;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/** Envio de arquivo (multipart/form-data) igual ao feito pela tela de upload. */
public final class Upload {

    private Upload() {}

    public static ValidatableResponse enviar(String token, Long idEmpresa, Long idUsuario, Long idPasta,
                                             Long idObrigacaoPendente, String nomeArquivo) {
        RequestSpecification req = given().auth().oauth2(token)
                .multiPart("arquivo", nomeArquivo, "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8),
                        "application/pdf")
                .multiPart("idEmpresa", String.valueOf(idEmpresa))
                .multiPart("idUsuario", String.valueOf(idUsuario))
                .multiPart("idPasta", String.valueOf(idPasta));
        if (idObrigacaoPendente != null) {
            req = req.multiPart("idObrigacaoPendente", String.valueOf(idObrigacaoPendente));
        }
        return req.when().post("/arquivos").then();
    }

    public static ValidatableResponse enviar(String token, Long idEmpresa, Long idUsuario, Long idPasta) {
        return enviar(token, idEmpresa, idUsuario, idPasta, null, "documento.pdf");
    }
}
