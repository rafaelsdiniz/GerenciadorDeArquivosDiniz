package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.UsuarioRequestDTO;
import diniz.contabilidade.arquivos.service.UsuarioService;
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

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService usuarioService;

    @GET
    public Response listar() {
        return Response.ok(usuarioService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(usuarioService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(usuarioService.buscarPorEmpresa(idEmpresa)).build();
    }

    @POST
    public Response salvar(@Valid UsuarioRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(usuarioService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(@PathParam("id") Long id, @Valid UsuarioRequestDTO dto) {
        return Response.ok(usuarioService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletar(@PathParam("id") Long id) {
        usuarioService.deletar(id);
        return Response.noContent().build();
    }
}