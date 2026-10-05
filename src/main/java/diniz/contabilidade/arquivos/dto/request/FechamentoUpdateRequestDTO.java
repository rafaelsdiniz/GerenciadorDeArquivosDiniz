package diniz.contabilidade.arquivos.dto.request;

import diniz.contabilidade.arquivos.model.enums.EtapaFechamento;

/**
 * Alteração parcial de um fechamento: campos nulos ficam como estão.
 * Observação vazia ("") apaga a observação; removerResponsavel=true deixa o card sem responsável.
 */
public record FechamentoUpdateRequestDTO(
    EtapaFechamento etapa,
    Long idResponsavel,
    Boolean removerResponsavel,
    String observacao
) {}
