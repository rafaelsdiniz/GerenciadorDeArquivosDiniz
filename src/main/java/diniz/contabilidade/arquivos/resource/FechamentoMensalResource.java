package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.dto.request.FechamentoUpdateRequestDTO;
import diniz.contabilidade.arquivos.service.FechamentoMensalService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Fechamento mensal (quadro kanban do escritório). Trabalho interno: só ADMIN.
 */
@Path("/fechamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN"})
public class FechamentoMensalResource {

    @Inject
    FechamentoMensalService service;

    /** Fechamentos de todas as empresas na competência "MM/aaaa" (padrão: mês anterior). Cria as linhas que faltarem. */
    @GET
    public Response listar(@QueryParam("competencia") String competencia) {
        return Response.ok(service.listar(competencia)).build();
    }

    /** Competências disponíveis no seletor (mais recente primeiro). */
    @GET
    @Path("/competencias")
    public Response competencias() {
        return Response.ok(service.competencias()).build();
    }

    /** Contagem por etapa e percentual concluído da competência. */
    @GET
    @Path("/resumo")
    public Response resumo(@QueryParam("competencia") String competencia) {
        return Response.ok(service.resumo(competencia)).build();
    }

    /** Altera etapa, responsável e/ou observação. */
    @PATCH
    @Path("/{id}")
    public Response atualizar(@PathParam("id") Long id, FechamentoUpdateRequestDTO dto) {
        return Response.ok(service.atualizar(id, dto)).build();
    }
}
