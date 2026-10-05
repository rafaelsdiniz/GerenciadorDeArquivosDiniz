package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.ObrigacaoRecorrenteRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoRecorrenteResponseDTO;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.ObrigacaoRecorrenteService;
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

@Path("/obrigacoes-recorrentes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ObrigacaoRecorrenteResource {

    @Inject
    ObrigacaoRecorrenteService service;

    @Inject
    UsuarioLogado usuario;

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(usuario.filtrar(service.listar(), ObrigacaoRecorrenteResponseDTO::idEmpresa)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(usuario.verificar(service.buscarPorId(id), ObrigacaoRecorrenteResponseDTO::idEmpresa)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return Response.ok(service.buscarPorEmpresa(idEmpresa)).build();
    }

    /** O calendário de obrigações é definido pelo escritório. */
    @POST
    @RolesAllowed({"ADMIN"})
    public Response salvar(@Valid ObrigacaoRecorrenteRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(service.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response atualizar(@PathParam("id") Long id, @Valid ObrigacaoRecorrenteRequestDTO dto) {
        return Response.ok(service.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response deletar(@PathParam("id") Long id) {
        service.deletar(id);
        return Response.noContent().build();
    }
}
