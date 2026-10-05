package diniz.contabilidade.arquivos.dto.response;

public record CompetenciaDTO(
    /** "MM/aaaa" */
    String competencia,
    /** "Setembro de 2026" */
    String rotulo,
    /** Competência sugerida ao abrir a tela (mês anterior ao atual). */
    boolean padrao
) {}
