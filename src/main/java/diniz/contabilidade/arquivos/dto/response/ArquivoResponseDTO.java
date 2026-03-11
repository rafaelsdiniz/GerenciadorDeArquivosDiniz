package diniz.contabilidade.arquivos.dto.response;

import diniz.contabilidade.arquivos.model.enums.TipoArquivo;

public record ArquivoResponseDTO(

    Long id,
    String nome,
    String nomeOriginal,
    Long tamanho,
    TipoArquivo tipoArquivo,
    String caminho,
    Long idEmpresa,
    Long idUsuario,
    Long idPasta

) {}