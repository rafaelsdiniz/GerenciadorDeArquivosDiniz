package diniz.contabilidade.arquivos.dto.response;

import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;

public record ObrigacaoRecorrenteResponseDTO(

    Long id,
    Long idEmpresa,
    String nome,
    String descricao,
    Periodicidade periodicidade,
    Integer diaVencimento,
    TipoArquivo tipoArquivoEsperado,
    Boolean ativo

) {}
