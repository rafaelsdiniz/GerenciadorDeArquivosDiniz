package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;

public record EventoCalendarioDTO(

    LocalDate data,
    String tipo,
    Long idReferencia,
    String titulo,
    String descricao,
    String status,
    String categoria

) {}
