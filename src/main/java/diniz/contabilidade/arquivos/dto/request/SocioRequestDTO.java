package diniz.contabilidade.arquivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocioRequestDTO(

    @NotBlank(message = "O nome do sócio é obrigatório")
    String nome,

    @NotBlank(message = "O CPF do sócio é obrigatório")
    String cpf,

    @NotNull(message = "O id da empresa é obrigatório")
    Long idEmpresa
) {
}