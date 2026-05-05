package diniz.contabilidade.arquivos.dto.request;

import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ObrigacaoRecorrenteRequestDTO(

    @NotNull(message = "O id da empresa é obrigatório.")
    Long idEmpresa,

    @NotBlank(message = "O nome da obrigação é obrigatório.")
    String nome,

    String descricao,

    @NotNull(message = "A periodicidade é obrigatória.")
    Periodicidade periodicidade,

    @NotNull(message = "O dia de vencimento é obrigatório.")
    @Min(value = 1, message = "Dia mínimo é 1.")
    @Max(value = 31, message = "Dia máximo é 31.")
    Integer diaVencimento,

    TipoArquivo tipoArquivoEsperado,

    Boolean ativo

) {}
