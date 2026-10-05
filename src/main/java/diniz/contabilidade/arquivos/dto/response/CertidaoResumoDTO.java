package diniz.contabilidade.arquivos.dto.response;

/** Contagem das certidões visíveis ao usuário por situação de validade. */
public record CertidaoResumoDTO(
    long total,
    long validas,
    long vencendo,
    long vencidas,
    /** empresas com ao menos uma certidão vencida ou vencendo */
    long empresasComAlerta,
    /** menor prazo (em dias) entre as certidões ainda não vencidas; null se não houver */
    Long proximoVencimentoDias
) {}
