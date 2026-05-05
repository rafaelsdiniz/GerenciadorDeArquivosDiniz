package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.service.ObrigacaoPendenteService;
import java.time.LocalDate;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/obrigacoes-pendentes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ObrigacaoPendenteResource {

    @Inject
    ObrigacaoPendenteService service;

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(service.listar()).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(service.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(service.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}/pendentes")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response pendentesPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(service.buscarPendentesPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/vencidas")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response vencidas() {
        return Response.ok(service.buscarVencidas()).build();
    }

    @GET
    @Path("/vencendo")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response vencendo(@QueryParam("dias") @DefaultValue("7") int dias) {
        return Response.ok(service.buscarVencendoEm(dias)).build();
    }

    @POST
    @Path("/gerar")
    @RolesAllowed({"ADMIN"})
    public Response gerar() {
        int criadas = service.gerarPendentesParaTodasRecorrentes();
        return Response.ok("{\"criadas\": " + criadas + "}").build();
    }

    @PATCH
    @Path("/{id}/entregar")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response marcarEntregue(@PathParam("id") Long id) {
        return Response.ok(service.marcarComoEntregue(id)).build();
    }

    @PATCH
    @Path("/{id}/vencimento")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizarVencimento(
            @PathParam("id") Long id,
            @QueryParam("dataVencimento") LocalDate dataVencimento) {
        return Response.ok(service.atualizarVencimento(id, dataVencimento)).build();
    }
}
