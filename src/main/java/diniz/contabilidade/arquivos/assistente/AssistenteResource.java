package diniz.contabilidade.arquivos.assistente;

import diniz.contabilidade.arquivos.assistente.AssistenteDto.MensagemRequest;
import diniz.contabilidade.arquivos.exception.ErroPayload;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Assistente Diniz (chat com IA). Responde só com dados que o usuário logado pode ver:
 * o escritório vê a carteira; o cliente, apenas a própria empresa.
 */
@Path("/assistente")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "FUNCIONARIO"})
public class AssistenteResource {

    @Inject
    AssistenteService service;

    @Inject
    AssistenteLimitador limitador;

    @Inject
    UsuarioLogado usuario;

    @GET
    @Path("/status")
    public Response status() {
        return Response.ok(service.status()).build();
    }

    @POST
    @Path("/mensagem")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mensagem(MensagemRequest req) {
        String chave = usuario.usuarioId() != null ? "u" + usuario.usuarioId() : usuario.email();
        if (!limitador.permitir(chave)) {
            return Response.status(429)
                    .entity(new ErroPayload("LIMITE", "Muitas perguntas em pouco tempo. Aguarde um minuto e tente de novo."))
                    .build();
        }
        return Response.ok(service.responder(req)).build();
    }
}
