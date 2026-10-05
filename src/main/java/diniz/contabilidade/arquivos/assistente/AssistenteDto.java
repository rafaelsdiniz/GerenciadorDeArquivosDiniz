package diniz.contabilidade.arquivos.assistente;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Contratos JSON do Assistente Diniz (chat com IA sobre os dados da conta). */
public final class AssistenteDto {

    private AssistenteDto() {
    }

    /** Uma fala anterior da conversa. papel = "usuario" | "assistente". */
    public record ItemHistorico(String papel, String texto) {
    }

    /** POST /assistente/mensagem */
    public record MensagemRequest(String mensagem, List<ItemHistorico> historico) {
    }

    /** Botão de navegação sugerido abaixo da resposta. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Acao(String rotulo, String rota, Map<String, String> queryParams) {

        public Acao(String rotulo, String rota) {
            this(rotulo, rota, null);
        }
    }

    /** Resposta do assistente. fonte = "IA" | "AUTOMATICA". */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resposta(String resposta, String fonte, List<Acao> acoes, Map<String, Object> dados) {
    }

    /** GET /assistente/status */
    public record Status(boolean iaConfigurada, String nome, boolean escritorio, List<String> sugestoes) {
    }
}
