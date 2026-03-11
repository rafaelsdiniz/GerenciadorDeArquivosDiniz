package diniz.contabilidade.arquivos.dto.response;

import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;

public record UsuarioResponseDTO(
    Long id,
    String nome,
    String email,
    PerfilUsuario perfilUsuario,
    Long idEmpresa
) {
}