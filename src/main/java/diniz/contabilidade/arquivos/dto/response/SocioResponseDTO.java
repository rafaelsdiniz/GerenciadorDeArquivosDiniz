package diniz.contabilidade.arquivos.dto.response;

public record SocioResponseDTO(

    Long id,
    String nome,
    String cpf,
    Long idEmpresa

) {}