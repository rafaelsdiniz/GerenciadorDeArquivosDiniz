package diniz.contabilidade.arquivos.resource;

import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import diniz.contabilidade.arquivos.service.LogAcessoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogAcessoResource {

    @Inject
    LogAcessoService service;

    @GET
    @RolesAllowed("ADMIN")
    public Response listarRecentes(@QueryParam("limite") @DefaultValue("100") int limite) {
        return Response.ok(service.listarRecentes(limite)).build();
    }

    @GET
    @Path("/entidade/{tipo}/{id}")
    @RolesAllowed("ADMIN")
    public Response porEntidade(@PathParam("tipo") String tipo, @PathParam("id") Long id) {
        return Response.ok(service.buscarPorEntidade(tipo, id)).build();
    }

    @GET
    @Path("/acao/{acao}")
    @RolesAllowed("ADMIN")
    public Response porAcao(@PathParam("acao") AcaoLog acao) {
        return Response.ok(service.buscarPorAcao(acao)).build();
    }

    @GET
    @Path("/periodo")
    @RolesAllowed("ADMIN")
    public Response porPeriodo(
            @QueryParam("inicio") LocalDateTime inicio,
            @QueryParam("fim") LocalDateTime fim) {
        return Response.ok(service.buscarPorPeriodo(inicio, fim)).build();
    }
}
