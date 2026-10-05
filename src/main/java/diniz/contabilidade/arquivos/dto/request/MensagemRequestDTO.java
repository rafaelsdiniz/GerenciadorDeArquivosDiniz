package diniz.contabilidade.arquivos.dto.request;

/** Nova mensagem na conversa de uma obrigação. idArquivo é opcional (arquivo da mesma empresa). */
public record MensagemRequestDTO(String texto, Long idArquivo) {}
