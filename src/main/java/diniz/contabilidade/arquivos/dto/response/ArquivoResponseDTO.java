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
    Long idObrigacaoPendente,
    /** Data do envio. */
    LocalDateTime dataCriacao,
    /** Nome de quem enviou. */
    String nomeUsuario,
    // ---- leitura inteligente (null enquanto o documento não foi lido)
    /** Valor do documento (ex.: total da guia). */
    java.math.BigDecimal valor,
    /** Linha digitável (só dígitos). */
    String linhaDigitavel,
    /** "MM/aaaa" */
    String competenciaDocumento,
    String cnpjDocumento,
    diniz.contabilidade.arquivos.model.enums.TipoDocumento tipoDocumento,
    /** "IA" ou "PADROES" */
    String fonteLeitura,
    java.util.List<String> alertasLeitura,
    LocalDateTime analisadoEm,
    /** false = registro sem o arquivo físico (dados de demonstração). */
    boolean possuiConteudo
) {}
