package diniz.contabilidade.arquivos.dto.request;


import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(

    @NotBlank(message = "O nome do usuário é obrigatório.")
    String nome,

    @NotBlank(message = "O email do usuário é obrigatório.")
    @Email(message = "O email informado é inválido.")
    String email,

    // obrigatória ao criar (validada no service); em branco na edição mantém a atual
    String senha,

    @NotNull(message = "O id da empresa é obrigatório.")
    Long idEmpresa,

    @NotNull(message = "O perfil do usuário é obrigatório.")
    PerfilUsuario perfilUsuario
) {
}