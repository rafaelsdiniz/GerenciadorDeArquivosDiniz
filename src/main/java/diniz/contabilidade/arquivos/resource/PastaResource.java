package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.PastaRequestDTO;
import diniz.contabilidade.arquivos.service.PastaService;
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

@Path("/pastas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PastaResource {

    @Inject
    PastaService pastaService;

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(pastaService.listar()).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(pastaService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(pastaService.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}/raiz")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPastasRaiz(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(pastaService.buscarPastasRaiz(idEmpresa)).build();
    }

    @GET
    @Path("/{idPastaPai}/subpastas")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarSubpastas(@PathParam("idPastaPai") Long idPastaPai) {
        return Response.ok(pastaService.buscarSubpastas(idPastaPai)).build();
    }

    @POST
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response salvar(@Valid PastaRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(pastaService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizar(@PathParam("id") Long id, @Valid PastaRequestDTO dto) {
        return Response.ok(pastaService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response deletar(@PathParam("id") Long id) {
        pastaService.deletar(id);
        return Response.noContent().build();
    }
}