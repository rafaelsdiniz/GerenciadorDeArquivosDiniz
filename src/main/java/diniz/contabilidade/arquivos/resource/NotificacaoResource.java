package diniz.contabilidade.arquivos.resource;

import diniz.contabilidade.arquivos.service.NotificacaoService;
import diniz.contabilidade.arquivos.service.ObrigacaoPendenteService;
import diniz.contabilidade.arquivos.service.SchedulerService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/notificacoes")
@Produces(MediaType.APPLICATION_JSON)
public class NotificacaoResource {

    @Inject
    NotificacaoService notificacaoService;

    @Inject
    SchedulerService schedulerService;

    @Inject
    ObrigacaoPendenteService obrigacaoPendenteService;

    @POST
    @Path("/disparar-avisos")
    @RolesAllowed("ADMIN")
    public Response dispararAvisos() {
        int total = notificacaoService.dispararAvisos();
        return Response.ok("{\"empresasNotificadas\": " + total + "}").build();
    }

    @POST
    @Path("/marcar-vencidos")
    @RolesAllowed("ADMIN")
    public Response marcarVencidos() {
        int arquivos = schedulerService.marcarArquivosVencidos();
        int obrigacoes = obrigacaoPendenteService.marcarVencidas();
        return Response.ok(
                "{\"arquivosVencidos\": " + arquivos
                        + ", \"obrigacoesVencidas\": " + obrigacoes + "}"
        ).build();
    }
}
