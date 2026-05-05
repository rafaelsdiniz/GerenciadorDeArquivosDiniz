package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.AcaoLog;

public record LogAcessoResponseDTO(

    Long id,
    LocalDateTime dataCriacao,
    Long idUsuario,
    String nomeUsuario,
    AcaoLog acao,
    String entidade,
    Long idEntidade,
    String detalhes

) {}
