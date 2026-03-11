package diniz.contabilidade.arquivos.dto.request;


import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(

    @NotBlank(message = "O nome do usuário é obrigatório.")
    String nome,

    @NotBlank(message = "O email do usuário é obrigatório.")
    String email,

    @NotBlank(message = "A senha do usuário é obrigatória.")
    @Size(min = 6, message = "A senha deve conter no mínimo 6 caracteres.")
    String senha,

    @NotNull(message = "O id da empresa é obrigatório.")
    Long idEmpresa,

    @NotBlank(message = "O perfil do usuário é obrigatório.")
    PerfilUsuario perfilUsuario
) {
}