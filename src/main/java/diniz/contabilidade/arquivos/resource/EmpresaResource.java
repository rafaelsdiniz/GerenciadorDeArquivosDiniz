package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.EmpresaRequestDTO;
import diniz.contabilidade.arquivos.service.EmpresaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpresaResource {

    @Inject
    EmpresaService empresaService;

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(empresaService.listar()).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(empresaService.buscarPorId(id)).build();
    }

    @POST
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response salvar(@Valid EmpresaRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(empresaService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizar(@PathParam("id") Long id, @Valid EmpresaRequestDTO dto) {
        return Response.ok(empresaService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response deletar(@PathParam("id") Long id) {
        empresaService.deletar(id);
        return Response.noContent().build();
    }
}