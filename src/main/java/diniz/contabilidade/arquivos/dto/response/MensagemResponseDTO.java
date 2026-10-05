package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDateTime;

/** Mensagem da conversa de uma obrigação, já do ponto de vista de quem consulta. */
public record MensagemResponseDTO(
    Long id,
    Long idObrigacao,
    String texto,
    LocalDateTime dataCriacao,
    Long idAutor,
    String autorNome,
    /** ADMIN (escritório) | FUNCIONARIO (cliente) */
    String autorPerfil,
    /** true = enviada pelo escritório. */
    boolean doEscritorio,
    /** true = enviada pelo usuário logado. */
    boolean minha,
    Long idArquivo,
    String nomeArquivo,
    LocalDateTime lidaPeloEscritorioEm,
    LocalDateTime lidaPeloClienteEm,
    /** O outro lado já leu (útil para o "visto" das minhas mensagens). */
    boolean lidaPeloDestinatario
) {}
