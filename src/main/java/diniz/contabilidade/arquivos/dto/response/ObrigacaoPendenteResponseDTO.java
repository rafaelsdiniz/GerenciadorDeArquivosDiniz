package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;

import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;

public record ObrigacaoPendenteResponseDTO(

    Long id,
    Long idEmpresa,
    Long idObrigacaoRecorrente,
    String nomeObrigacao,
    LocalDate dataVencimento,
    LocalDate dataEntrega,
    StatusObrigacao status,
    Long diasParaVencer,
    String nomeEmpresa,
    /** Mês de referência "MM/aaaa" (vencimento menos 1 mês; anual = ano anterior). */
    String competencia,
    ResponsavelObrigacao responsavel,
    /** Arquivos anexados (guia, comprovante, declaração). */
    long totalArquivos,
    /** Data do pagamento da guia (null = não paga). */
    LocalDate dataPagamento,
    /** NAO_SE_APLICA (documentos do cliente / não entregue) | AGUARDANDO | ATRASADO | PAGO */
    String situacaoPagamento
) {}
