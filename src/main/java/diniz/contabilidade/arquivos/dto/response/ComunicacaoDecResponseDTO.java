package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComunicacaoDecResponseDTO(
    Long id,
    String idExterno,
    Long idEmpresa,
    String nomeEmpresa,
    String cnpj,
    String razaoSocial,
    String numero,
    String tipo,
    String assunto,
    String corpo,
    String remetente,
    LocalDate disponibilizadaEm,
    LocalDate cienteEm,
    LocalDate prazoCienciaEm,
    Integer diasParaResposta,
    LocalDateTime coletadaEm,
    String status,
    String urgencia,
    String motivo,
    /** dias até a ciência tácita (negativo = já passou), recalculado hoje */
    Long diasRestantes,
    LocalDate cienciaTacitaEm,
    LocalDate prazoRespostaEm,
    Boolean encerrada,
    Boolean temInteiroTeor,
    String link,
    LocalDateTime sincronizadaEm
) {
}
