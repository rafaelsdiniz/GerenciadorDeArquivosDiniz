package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.SituacaoCertidao;
import diniz.contabilidade.arquivos.model.enums.TipoCertidao;

/**
 * Certidão com a validade já calculada:
 * statusValidade = VALIDA | VENCENDO (vence em até 15 dias) | VENCIDA.
 */
public record CertidaoResponseDTO(
    Long id,
    Long idEmpresa,
    String nomeEmpresa,
    TipoCertidao tipo,
    String tipoRotulo,
    SituacaoCertidao situacao,
    String numero,
    LocalDate dataEmissao,
    LocalDate dataValidade,
    String orgaoEmissor,
    String observacao,
    Long idArquivo,
    String nomeArquivo,
    String statusValidade,
    Long diasParaVencer,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao
) {}
