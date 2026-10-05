package diniz.contabilidade.arquivos.dto.response;

import java.util.Map;

/** Mensagens não lidas pelo usuário logado: total e contagem por obrigação (id -> quantidade). */
public record MensagensNaoLidasDTO(long total, Map<Long, Long> porObrigacao) {}
