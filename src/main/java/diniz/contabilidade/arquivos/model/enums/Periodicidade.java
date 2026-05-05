package diniz.contabilidade.arquivos.model.enums;

import java.time.LocalDate;

public enum Periodicidade {
    MENSAL,
    TRIMESTRAL,
    ANUAL;

    public LocalDate proximaOcorrencia(LocalDate referencia) {
        return switch (this) {
            case MENSAL -> referencia.plusMonths(1);
            case TRIMESTRAL -> referencia.plusMonths(3);
            case ANUAL -> referencia.plusYears(1);
        };
    }
}
