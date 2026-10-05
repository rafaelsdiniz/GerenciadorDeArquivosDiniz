package diniz.contabilidade.arquivos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/**
 * Resultado da leitura inteligente de um documento (guia, nota, extrato, certidão...).
 *
 * fonte: "IA" (DeepSeek + validação) ou "PADROES" (leitura determinística por expressões regulares).
 * Campos não encontrados ficam nulos.
 */
public record DocumentoAnalisado(
    TipoDocumento tipoDocumento,
    String tipoDocumentoRotulo,
    CategoriaFiscal categoriaFiscalSugerida,
    String descricaoSugerida,
    /** CNPJ (14 dígitos) do contribuinte/emitente encontrado no documento. */
    String cnpj,
    /** true = confere com a empresa; false = diverge; null = sem empresa para comparar ou CNPJ não encontrado. */
    Boolean cnpjConfere,
    String razaoSocial,
    /** "MM/aaaa" */
    String competencia,
    LocalDate vencimento,
    BigDecimal valor,
    /** Só dígitos (47 = boleto bancário, 48 = arrecadação/tributos). */
    String linhaDigitavel,
    String codigoBarras,
    String numeroDocumento,
    /** Validade (certidões). */
    LocalDate validade,
    String fonte,
    /** Modelo de IA usado (null na leitura por padrões). */
    String modelo,
    /** 0 a 1 */
    double confianca,
    /** false = documento sem texto (imagem/escaneado) ou formato não suportado. */
    boolean textoLegivel,
    List<Alerta> alertas,
    String textoExtraidoResumo
) {

    /** nivel: "danger" (divergência grave), "warning" (atenção) ou "info". */
    public record Alerta(String codigo, String nivel, String mensagem) {}
}
