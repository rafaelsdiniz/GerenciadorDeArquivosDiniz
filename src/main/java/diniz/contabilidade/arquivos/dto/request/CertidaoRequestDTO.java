package diniz.contabilidade.arquivos.dto.request;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.model.enums.SituacaoCertidao;
import diniz.contabilidade.arquivos.model.enums.TipoCertidao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertidaoRequestDTO(

    @NotNull(message = "A empresa é obrigatória.")
    Long idEmpresa,

    @NotNull(message = "O tipo da certidão é obrigatório.")
    TipoCertidao tipo,

    @NotNull(message = "A situação da certidão é obrigatória.")
    SituacaoCertidao situacao,

    @Size(max = 120, message = "O número pode ter no máximo 120 caracteres.")
    String numero,

    LocalDate dataEmissao,

    @NotNull(message = "A data de validade é obrigatória.")
    LocalDate dataValidade,

    @Size(max = 160, message = "O órgão emissor pode ter no máximo 160 caracteres.")
    String orgaoEmissor,

    @Size(max = 1000, message = "A observação pode ter no máximo 1000 caracteres.")
    String observacao,

    /** arquivo já existente no Drive da empresa (opcional) */
    Long idArquivo
) {}
