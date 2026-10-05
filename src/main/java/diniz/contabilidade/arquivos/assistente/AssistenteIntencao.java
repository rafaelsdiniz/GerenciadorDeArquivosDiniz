package diniz.contabilidade.arquivos.assistente;

/**
 * Assuntos reconhecidos na pergunta do usuário. A ordem da declaração é a prioridade:
 * quando a frase cita vários assuntos, o primeiro da lista é o principal
 * (ex.: "Quais certidões vencem nos próximos 15 dias?" → CERTIDOES, não PRAZOS).
 */
public enum AssistenteIntencao {
    AGRADECIMENTO,
    CERTIDOES,
    DEC,
    VENCIDAS,
    /** anexar guia / prorrogar vencimento (rotina do escritório) */
    ANEXAR_GUIA,
    PAGAMENTO,
    ENVIAR,
    RESUMO,
    PRAZOS,
    ARQUIVOS,
    CALENDARIO,
    RELATORIOS,
    MENSAGENS,
    CONTA,
    AJUDA,
    SAUDACAO
}
