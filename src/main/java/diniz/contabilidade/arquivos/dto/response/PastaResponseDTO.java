package diniz.contabilidade.arquivos.dto.response;

public record PastaResponseDTO(

    Long id,
    String nome,
    String descricao,
    Long idEmpresa,
    Long idPastaPai

) {}