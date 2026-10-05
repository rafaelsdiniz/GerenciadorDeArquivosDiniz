package diniz.contabilidade.arquivos.config;

import java.util.TimeZone;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Fixa o fuso da aplicação no horário do Tocantins. Sem isso, em servidores em UTC
 * (como o Render) as datas de envio, criação e auditoria ficariam 3 horas adiantadas.
 */
@Startup
@ApplicationScoped
public class FusoHorario {

    public static final String FUSO = "America/Araguaina";

    @PostConstruct
    void configurar() {
        TimeZone.setDefault(TimeZone.getTimeZone(FUSO));
    }
}
