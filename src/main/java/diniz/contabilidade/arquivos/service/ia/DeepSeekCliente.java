package diniz.contabilidade.arquivos.service.ia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Cliente mínimo da API do DeepSeek (compatível com a da OpenAI): POST {url}/chat/completions,
 * resposta em JSON (response_format json_object), temperatura 0.
 *
 * Classe simples (sem CDI) para poder ser testada com um servidor falso. A chave nunca é registrada em log.
 */
public class DeepSeekCliente {

    private final String url;
    private final String chave;
    private final String modelo;
    private final Duration timeout;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public DeepSeekCliente(String url, String chave, String modelo, Duration timeout, ObjectMapper mapper) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.chave = chave;
        this.modelo = modelo;
        this.timeout = timeout;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public String modelo() {
        return modelo;
    }

    /** Falha da chamada (rede, tempo esgotado, HTTP != 200 ou resposta fora do formato). */
    public static class FalhaIa extends Exception {
        public FalhaIa(String mensagem) {
            super(mensagem);
        }
    }

    /** Envia as mensagens e devolve o objeto JSON produzido pelo modelo. */
    public JsonNode completarJson(String sistema, String usuario) throws FalhaIa {
        ObjectNode corpo = mapper.createObjectNode();
        corpo.put("model", modelo);
        corpo.put("temperature", 0);
        corpo.put("max_tokens", 900);
        corpo.putObject("response_format").put("type", "json_object");
        ArrayNode msgs = corpo.putArray("messages");
        msgs.addObject().put("role", "system").put("content", sistema);
        msgs.addObject().put("role", "user").put("content", usuario);

        HttpResponse<String> resp;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + chave)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo), StandardCharsets.UTF_8))
                    .build();
            resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new FalhaIa("tempo esgotado");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaIa("interrompido");
        } catch (Exception e) {
            throw new FalhaIa("falha de conexão (" + e.getClass().getSimpleName() + ")");
        }

        if (resp.statusCode() == 401 || resp.statusCode() == 403) throw new FalhaIa("chave recusada (HTTP " + resp.statusCode() + ")");
        if (resp.statusCode() == 402) throw new FalhaIa("saldo insuficiente na conta da IA (HTTP 402)");
        if (resp.statusCode() == 429) throw new FalhaIa("limite de requisições atingido (HTTP 429)");
        if (resp.statusCode() != 200) throw new FalhaIa("HTTP " + resp.statusCode());

        try {
            JsonNode raiz = mapper.readTree(resp.body());
            String conteudo = raiz.path("choices").path(0).path("message").path("content").asText("");
            conteudo = conteudo.trim();
            // tolera ```json ... ``` caso o modelo ignore o response_format
            if (conteudo.startsWith("```")) {
                conteudo = conteudo.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            }
            int ini = conteudo.indexOf('{');
            int fim = conteudo.lastIndexOf('}');
            if (ini < 0 || fim <= ini) throw new FalhaIa("resposta sem JSON");
            JsonNode json = mapper.readTree(conteudo.substring(ini, fim + 1));
            if (!json.isObject()) throw new FalhaIa("resposta sem objeto JSON");
            return json;
        } catch (FalhaIa e) {
            throw e;
        } catch (Exception e) {
            throw new FalhaIa("resposta em formato inesperado");
        }
    }
}
