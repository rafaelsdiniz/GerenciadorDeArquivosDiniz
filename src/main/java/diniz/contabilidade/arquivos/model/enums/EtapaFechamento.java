package diniz.contabilidade.arquivos.model.enums;

/**
 * Etapas do fechamento mensal de uma empresa no escritório (colunas do quadro kanban).
 */
public enum EtapaFechamento {
    /** Esperando extratos, notas e demais documentos do cliente. */
    AGUARDANDO_DOCUMENTOS,
    /** Documentos recebidos: escrituração e apuração dos impostos em andamento. */
    EM_APURACAO,
    /** Guias e declarações emitidas e enviadas ao cliente. */
    GUIAS_EMITIDAS,
    /** Competência encerrada. */
    CONCLUIDO
}
