package diniz.contabilidade.arquivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PastaRequestDTO(
    
    @NotBlank(message = "O nome da pasta é obrigatório.")
    String nome,

    String descricao,

    @NotNull(message = "O id da empresa é obrigatório.")
    Long idEmpresa,

    Long idPastaPai

) {
}