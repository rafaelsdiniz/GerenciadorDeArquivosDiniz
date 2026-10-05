package diniz.contabilidade.arquivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequestDTO(
    @NotBlank(message = "Informe a senha atual.")
    String senhaAtual,

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres.")
    String novaSenha
) {
}
