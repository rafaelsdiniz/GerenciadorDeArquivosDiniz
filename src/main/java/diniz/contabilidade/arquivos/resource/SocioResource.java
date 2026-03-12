package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.SocioRequestDTO;
import diniz.contabilidade.arquivos.service.SocioService;
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

@Path("/socios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SocioResource {

    @Inject
    SocioService socioService;

    @GET
    public Response listar() {
        return Response.ok(socioService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(socioService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(socioService.buscarPorEmpresa(idEmpresa)).build();
    }

    @POST
    public Response salvar(@Valid SocioRequestDTO dto) {
        return Response.status(Response.Status.CREATED)
                .entity(socioService.salvar(dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(@PathParam("id") Long id, @Valid SocioRequestDTO dto) {
        return Response.ok(socioService.atualizar(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletar(@PathParam("id") Long id) {
        socioService.deletar(id);
        return Response.noContent().build();
    }
}