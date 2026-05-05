package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;

public record ArquivoResponseDTO(

    Long id,
    String nome,
    String nomeOriginal,
    Long tamanho,
    TipoArquivo tipoArquivo,
    CategoriaFiscal categoriaFiscal,
    String caminho,
    String descricao,
    LocalDate dataVencimento,
    StatusArquivo status,
    Long diasParaVencer,
    LocalDateTime excluidoEm,
    Long idEmpresa,
    Long idUsuario,
    Long idPasta,
    Long idObrigacaoPendente

) {}
