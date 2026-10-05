package diniz.contabilidade.arquivos.resource;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.ObrigacaoPendenteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

    @Inject
    UsuarioLogado usuario;

    private ObrigacaoPendenteResponseDTO visivel(Long id) {
        return usuario.verificar(service.buscarPorId(id), ObrigacaoPendenteResponseDTO::idEmpresa);
    }

    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        return Response.ok(usuario.filtrar(service.listar(), ObrigacaoPendenteResponseDTO::idEmpresa)).build();
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
        return Response.ok(service.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}/pendentes")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response pendentesPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return Response.ok(service.buscarPendentesPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/vencidas")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response vencidas() {
        return Response.ok(usuario.filtrar(service.buscarVencidas(), ObrigacaoPendenteResponseDTO::idEmpresa)).build();
    }

    @GET
    @Path("/vencendo")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response vencendo(@QueryParam("dias") @DefaultValue("7") int dias) {
        return Response.ok(usuario.filtrar(service.buscarVencendoEm(dias), ObrigacaoPendenteResponseDTO::idEmpresa)).build();
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
        visivel(id);
        return Response.ok(service.marcarComoEntregue(id)).build();
    }

    /** Confirma o pagamento da guia (cliente da própria empresa ou escritório). Data opcional (padrão: hoje). */
    @PATCH
    @Path("/{id}/pagamento")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response confirmarPagamento(@PathParam("id") Long id, @QueryParam("data") LocalDate data) {
        visivel(id);
        return Response.ok(service.confirmarPagamento(id, data)).build();
    }

    /** Desfaz a confirmação de pagamento. Só o escritório. */
    @DELETE
    @Path("/{id}/pagamento")
    @RolesAllowed({"ADMIN"})
    public Response desfazerPagamento(@PathParam("id") Long id) {
        return Response.ok(service.desfazerPagamento(id)).build();
    }

    /** Desfaz a entrega (erro de marcação ou guia substituída). Só o escritório. */
    @PATCH
    @Path("/{id}/reabrir")
    @RolesAllowed({"ADMIN"})
    public Response reabrir(@PathParam("id") Long id) {
        return Response.ok(service.reabrir(id)).build();
    }

    @PATCH
    @Path("/{id}/vencimento")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response atualizarVencimento(
            @PathParam("id") Long id,
            @QueryParam("dataVencimento") LocalDate dataVencimento) {
        visivel(id);
        return Response.ok(service.atualizarVencimento(id, dataVencimento)).build();
    }
}
