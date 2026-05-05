package diniz.contabilidade.arquivos.dto.response;

public record EnderecoDTO(
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String uf,
    String cep
) {}
