package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDateTime;

/** Conversa com atividade recente (para o painel de notificações). */
public record ConversaRecenteDTO(
    Long idObrigacao,
    String nomeObrigacao,
    /** Mês de referência "MM/aaaa" da obrigação. */
    String competencia,
    Long idEmpresa,
    String nomeEmpresa,
    Long idMensagem,
    String autorNome,
    String autorPerfil,
    boolean doEscritorio,
    boolean minha,
    /** Início do texto da última mensagem. */
    String previa,
    LocalDateTime dataUltimaMensagem,
    /** Mensagens desta conversa ainda não lidas pelo usuário logado. */
    long naoLidas
) {}
