package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.MensagemRequestDTO;
import diniz.contabilidade.arquivos.service.MensagemObrigacaoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Mensagens entre escritório e cliente dentro de cada obrigação. */
@Path("/mensagens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "FUNCIONARIO"})
public class MensagemObrigacaoResource {

    @Inject
    MensagemObrigacaoService service;

    /** Conversa da obrigação (mais antiga primeiro). Abrir marca como lidas as mensagens do outro lado. */
    @GET
    @Path("/obrigacao/{idObrigacao}")
    public Response conversa(@PathParam("idObrigacao") Long idObrigacao) {
        return Response.ok(service.abrirConversa(idObrigacao)).build();
    }

    @POST
    @Path("/obrigacao/{idObrigacao}")
    public Response enviar(@PathParam("idObrigacao") Long idObrigacao, MensagemRequestDTO dto) {
        return Response.status(Response.Status.CREATED).entity(service.enviar(idObrigacao, dto)).build();
    }

    /** {total, porObrigacao: {idObrigacao: quantidade}} das mensagens não lidas pelo usuário logado. */
    @GET
    @Path("/nao-lidas")
    public Response naoLidas() {
        return Response.ok(service.naoLidas()).build();
    }

    /** Conversas com atividade mais recente, com prévia da última mensagem. */
    @GET
    @Path("/recentes")
    public Response recentes(@QueryParam("limite") @DefaultValue("10") int limite) {
        return Response.ok(service.recentes(limite)).build();
    }
}
