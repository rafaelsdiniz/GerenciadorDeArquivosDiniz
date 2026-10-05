package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.PastaRequestDTO;
import diniz.contabilidade.arquivos.dto.response.PastaResponseDTO;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
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

    @Inject
    UsuarioLogado usuario;

    private PastaResponseDTO visivel(Long id) {
        return usuario.verificar(pastaService.buscarPorId(id), PastaResponseDTO::idEmpresa);
    }

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(usuario.filtrar(pastaService.listar(), PastaResponseDTO::idEmpresa)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(visivel(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return Response.ok(pastaService.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}/raiz")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPastasRaiz(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return Response.ok(pastaService.buscarPastasRaiz(idEmpresa)).build();
    }

    @GET
    @Path("/{idPastaPai}/subpastas")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarSubpastas(@PathParam("idPastaPai") Long idPastaPai) {
        visivel(idPastaPai);
        return Response.ok(pastaService.buscarSubpastas(idPastaPai)).build();
    }

    @POST
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response salvar(@Valid PastaRequestDTO dto) {
        usuario.exigirEmpresa(dto.idEmpresa());
        return Response.status(Response.Status.CREATED)
                .entity(pastaService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizar(@PathParam("id") Long id, @Valid PastaRequestDTO dto) {
        visivel(id);
        usuario.exigirEmpresa(dto.idEmpresa());
        return Response.ok(pastaService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response deletar(@PathParam("id") Long id) {
        visivel(id);
        pastaService.deletar(id);
        return Response.noContent().build();
    }
}
