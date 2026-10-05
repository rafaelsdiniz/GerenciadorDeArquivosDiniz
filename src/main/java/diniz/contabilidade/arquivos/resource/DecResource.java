package diniz.contabilidade.arquivos.resource;

import java.util.Map;

import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.exception.ErroPayload;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.DecIntegracaoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Comunicações do DEC (SEFAZ-TO) sincronizadas do DEC Monitor. Somente leitura.
 * Funcionário de cliente vê só as da própria empresa; o escritório vê todas,
 * inclusive as de CNPJ ainda não cadastrado no Gerenciador.
 */
@Path("/dec")
@Produces(MediaType.APPLICATION_JSON)
public class DecResource {

    @Inject
    DecIntegracaoService service;

    @Inject
    UsuarioLogado usuario;

    @GET
    @Path("/status")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response status() {
        DecIntegracaoService.StatusIntegracao s = service.status();
        if (!usuario.isAdmin()) {
            // cliente só precisa saber se está ligada e quando atualizou
            return Response.ok(Map.of(
                    "configurada", s.configurada(),
                    "ultimaSincronizacao", s.ultimaSincronizacao() == null ? "" : s.ultimaSincronizacao().toString()
            )).build();
        }
        return Response.ok(s).build();
    }

    @GET
    @Path("/comunicacoes")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response listar() {
        if (usuario.isAdmin()) {
            return Response.ok(service.listar()).build();
        }
        return Response.ok(usuario.filtrar(service.listar(), ComunicacaoDecResponseDTO::idEmpresa)).build();
    }

    @GET
    @Path("/comunicacoes/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response porEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return Response.ok(service.listarPorEmpresa(idEmpresa)).build();
    }

    /** Buscar agora (botão "Sincronizar"). Só o escritório. */
    @POST
    @Path("/sincronizar")
    @RolesAllowed({"ADMIN"})
    public Response sincronizar() {
        try {
            return Response.ok(service.sincronizar()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErroPayload("DEC", e.getMessage()))
                    .build();
        }
    }
}
