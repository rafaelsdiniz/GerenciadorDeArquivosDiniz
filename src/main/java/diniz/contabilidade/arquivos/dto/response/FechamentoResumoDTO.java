package diniz.contabilidade.arquivos.dto.response;

import java.util.Map;

import diniz.contabilidade.arquivos.model.enums.EtapaFechamento;

public record FechamentoResumoDTO(
    String competencia,
    long total,
    /** Quantidade de empresas em cada etapa (todas as etapas presentes, mesmo com zero). */
    Map<EtapaFechamento, Long> porEtapa,
    /** Percentual de empresas concluídas (0–100, arredondado). */
    int percentualConcluido,
    /** Não concluídos com o prazo já vencido. */
    long atrasados,
    /** Não concluídos sem responsável definido. */
    long semResponsavel
) {}
