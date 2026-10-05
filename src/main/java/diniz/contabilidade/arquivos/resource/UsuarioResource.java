package diniz.contabilidade.arquivos.resource;

import java.util.Map;

import diniz.contabilidade.arquivos.dto.request.AlterarSenhaRequestDTO;
import diniz.contabilidade.arquivos.dto.request.UsuarioRequestDTO;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.UsuarioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotAuthorizedException;
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

    @Inject
    UsuarioLogado logado;

    private Long meuId() {
        Long id = logado.usuarioId();
        if (id == null) throw new NotAuthorizedException("Sessão inválida.");
        return id;
    }

    /** Perfil do usuário logado (qualquer perfil). */
    @GET
    @Path("/me")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response meuPerfil() {
        return Response.ok(usuarioService.buscarPorId(meuId())).build();
    }

    @PUT
    @Path("/me")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizarMeuPerfil(Map<String, String> corpo) {
        return Response.ok(usuarioService.atualizarMeuNome(meuId(), corpo != null ? corpo.get("nome") : null)).build();
    }

    @PUT
    @Path("/me/senha")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response alterarMinhaSenha(@Valid AlterarSenhaRequestDTO dto) {
        usuarioService.alterarSenha(meuId(), dto.senhaAtual(), dto.novaSenha());
        return Response.noContent().build();
    }

    @GET
    @RolesAllowed({"ADMIN"})
    public Response listar() {
        return Response.ok(usuarioService.listar()).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(usuarioService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(usuarioService.buscarPorEmpresa(idEmpresa)).build();
    }

    @POST
    @RolesAllowed({"ADMIN"})
    public Response salvar(@Valid UsuarioRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(usuarioService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response atualizar(@PathParam("id") Long id, @Valid UsuarioRequestDTO dto) {
        return Response.ok(usuarioService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response deletar(@PathParam("id") Long id) {
        usuarioService.deletar(id);
        return Response.noContent().build();
    }
}