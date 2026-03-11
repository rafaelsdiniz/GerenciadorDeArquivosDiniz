package diniz.contabilidade.arquivos.dto.request;

import jakarta.validation.constraints.NotNull;

public record ArquivoRequestDTO(

    @NotNull(message = "O id da empresa é obrigatório.")
    Long idEmpresa,

    @NotNull(message = "O id do usuário é obrigatório.")
    Long idUsuario,

    @NotNull(message = "O id da pasta é obrigatório.")
    Long idPasta

) {}