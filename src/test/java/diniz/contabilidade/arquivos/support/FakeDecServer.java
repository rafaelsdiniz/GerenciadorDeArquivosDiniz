package diniz.contabilidade.arquivos.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Servidor HTTP falso que imita a API de integração do DEC Monitor
 * (GET /api/integracao/comunicacoes, protegida por "Authorization: Bearer").
 *
 * Sobe numa porta livre antes da aplicação e aponta dec.url / dec.token para ele,
 * então os testes nunca chamam o DEC real.
 *
 * Os testes controlam as respostas por HTTP (rotas /teste/...), o que evita
 * compartilhar estado estático entre class loaders do Quarkus:
 *   POST /teste/itens  corpo = array JSON de comunicações devolvido nas próximas chamadas
 *   POST /teste/token  corpo = token que o servidor passa a aceitar
 *   POST /teste/reset  volta ao estado inicial (sem itens, token padrão)
 */
public class FakeDecServer implements QuarkusTestResourceLifecycleManager {

    public static final String TOKEN = "token-de-teste-dec";

    private HttpServer server;
    private volatile String itensJson = "[]";
    private volatile String tokenAceito = TOKEN;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível subir o DEC falso", e);
        }

        server.createContext("/api/integracao/comunicacoes", ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (!("Bearer " + tokenAceito).equals(auth)) {
                responder(ex, 401, "{\"erro\":\"token invalido\"}");
                return;
            }
            responder(ex, 200, "{\"escritorio\":{\"nome\":\"Diniz Assessoria Contábil\",\"cnpj\":\"11111111000101\"},"
                    + "\"itens\":" + itensJson + "}");
        });
        server.createContext("/teste/itens", ex -> {
            itensJson = corpo(ex);
            responder(ex, 204, null);
        });
        server.createContext("/teste/token", ex -> {
            tokenAceito = corpo(ex);
            responder(ex, 204, null);
        });
        server.createContext("/teste/reset", ex -> {
            itensJson = "[]";
            tokenAceito = TOKEN;
            responder(ex, 204, null);
        });
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        return Map.of("dec.url", url, "dec.token", TOKEN, "dec.demo", "false");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static String corpo(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void responder(HttpExchange ex, int status, String json) throws IOException {
        if (json == null) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
