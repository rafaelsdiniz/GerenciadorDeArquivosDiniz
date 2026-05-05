package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.service.DashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    DashboardService service;

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response porEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(service.porEmpresa(idEmpresa)).build();
    }
}
