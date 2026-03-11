package diniz.contabilidade.arquivos.dto.response;

public record EmpresaResponseDTO(

    Long id,
    String nomeFantasia,
    String razaoSocial,
    String cnpj,
    String telefone,
    String email

) {}