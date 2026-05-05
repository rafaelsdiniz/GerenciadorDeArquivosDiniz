package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;

public record ObrigacaoPendenteResponseDTO(

    Long id,
    Long idEmpresa,
    Long idObrigacaoRecorrente,
    String nomeObrigacao,
    LocalDate dataVencimento,
    LocalDate dataEntrega,
    StatusObrigacao status,
    Long diasParaVencer

) {}
