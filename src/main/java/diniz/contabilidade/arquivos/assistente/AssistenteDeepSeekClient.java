package diniz.contabilidade.arquivos.assistente;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cliente de chat da API do DeepSeek (compatível com a da OpenAI): POST {url}/chat/completions.
 * Usa as mesmas propriedades ia.deepseek.* da leitura de documentos. A chave nunca vai para o log.
 */
@ApplicationScoped
public class AssistenteDeepSeekClient {

    /** Tempo máximo de espera pela resposta; depois disso o assistente usa a resposta automática. */
    static final Duration TIMEOUT = Duration.ofSeconds(25);

    /** vazio = IA desligada (Optional: o Quarkus trata "" como ausente) */
    @ConfigProperty(name = "ia.deepseek.api-key")
    Optional<String> chaveConfig;

    @ConfigProperty(name = "ia.deepseek.url", defaultValue = "https://api.deepseek.com")
    String url;

    @ConfigProperty(name = "ia.deepseek.modelo", defaultValue = "deepseek-chat")
    String modelo;

    @Inject
    ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** Uma mensagem da conversa. role = system | user | assistant. */
    public record Mensagem(String role, String content) {
    }

    /** Falha da chamada (rede, tempo esgotado, HTTP != 200, resposta vazia). Mensagem sem dados sensíveis. */
    public static class FalhaAssistente extends Exception {
        public FalhaAssistente(String motivo) {
            super(motivo);
        }
    }

    public boolean configurado() {
        return chaveConfig != null && chaveConfig.filter(c -> !c.isBlank()).isPresent();
    }

    public String conversar(List<Mensagem> mensagens) throws FalhaAssistente {
        if (!configurado()) throw new FalhaAssistente("chave não configurada");
        String chave = chaveConfig.get();

        ObjectNode corpo = mapper.createObjectNode();
        corpo.put("model", modelo);
        corpo.put("temperature", 0.3);
        corpo.put("max_tokens", 700);
        corpo.put("stream", false);
        ArrayNode msgs = corpo.putArray("messages");
        for (Mensagem m : mensagens) {
            msgs.addObject().put("role", m.role()).put("content", m.content());
        }

        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        HttpResponse<String> resp;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/chat/completions"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + chave.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo), StandardCharsets.UTF_8))
                    .build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new FalhaAssistente("tempo esgotado");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaAssistente("interrompido");
        } catch (Exception e) {
            throw new FalhaAssistente("falha de conexão (" + e.getClass().getSimpleName() + ")");
        }

        int status = resp.statusCode();
        if (status == 401 || status == 403) throw new FalhaAssistente("chave recusada (HTTP " + status + ")");
        if (status == 402) throw new FalhaAssistente("saldo insuficiente (HTTP 402)");
        if (status == 429) throw new FalhaAssistente("limite de requisições (HTTP 429)");
        if (status != 200) throw new FalhaAssistente("HTTP " + status);

        try {
            JsonNode raiz = mapper.readTree(resp.body());
            String conteudo = raiz.path("choices").path(0).path("message").path("content").asText("").trim();
            if (conteudo.isEmpty()) throw new FalhaAssistente("resposta vazia");
            return conteudo;
        } catch (FalhaAssistente e) {
            throw e;
        } catch (Exception e) {
            throw new FalhaAssistente("resposta em formato inesperado");
        }
    }
}
