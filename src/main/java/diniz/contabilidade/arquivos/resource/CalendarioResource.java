package diniz.contabilidade.arquivos.resource;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.service.CalendarioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/calendario")
@Produces(MediaType.APPLICATION_JSON)
public class CalendarioResource {

    @Inject
    CalendarioService service;

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response porMes(
            @PathParam("idEmpresa") Long idEmpresa,
            @QueryParam("ano") @DefaultValue("0") int ano,
            @QueryParam("mes") @DefaultValue("0") int mes) {

        LocalDate hoje = LocalDate.now();
        if (ano <= 0) ano = hoje.getYear();
        if (mes <= 0) mes = hoje.getMonthValue();

        return Response.ok(service.eventosDoMes(idEmpresa, ano, mes)).build();
    }
}
