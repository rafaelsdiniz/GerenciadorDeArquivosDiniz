package diniz.contabilidade.arquivos.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmpresaRequestDTO(
    
    @NotBlank(message = "O nome fantasia é obrigatório.")
    String nomeFantasia,

    @NotBlank(message = "A razão social é obrigatória.")
    String razaoSocial,

    @NotBlank(message = "O CNPJ é obrigatório.")
    String cnpj,

    @NotBlank(message = "O telefone é obrigatório.")
    String telefone,

    @NotBlank(message = "O email é obrigatório.")
    String email

) {}