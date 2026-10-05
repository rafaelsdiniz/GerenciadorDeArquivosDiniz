package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.EtapaFechamento;

public record FechamentoMensalResponseDTO(
    Long id,
    Long idEmpresa,
    String nomeEmpresa,
    String cnpj,
    String regimeTributario,
    String competencia,
    EtapaFechamento etapa,
    Long idResponsavel,
    String nomeResponsavel,
    LocalDate prazo,
    /** Dias até o prazo (negativo = atrasado). */
    Long diasParaPrazo,
    /** Prazo vencido sem concluir. */
    boolean atrasado,
    String observacao,
    LocalDateTime concluidoEm,
    LocalDateTime dataAtualizacao,
    Indicadores indicadores
) {

    /** Progresso calculado a partir das obrigações da competência. */
    public record Indicadores(
        /** Documentos que o cliente precisa enviar (obrigações de responsabilidade do CLIENTE). */
        int documentosClienteTotal,
        int documentosClientePendentes,
        /** Obrigações do escritório (guias e declarações). */
        int obrigacoesEscritorioTotal,
        int obrigacoesEscritorioEntregues,
        int obrigacoesEscritorioPendentes,
        /** Pendências (de qualquer responsável) com vencimento já passado. */
        int obrigacoesVencidas,
        /** Guias entregues aguardando o pagamento do cliente (inclui as atrasadas). */
        int guiasAguardandoPagamento,
        int guiasPagamentoAtrasado,
        int guiasPagas
    ) {}
}
