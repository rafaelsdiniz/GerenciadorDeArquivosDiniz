package diniz.contabilidade.arquivos.assistente;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/** Limite simples em memória: no máximo {@value #MAXIMO} mensagens por usuário por minuto. */
@ApplicationScoped
public class AssistenteLimitador {

    static final int MAXIMO = 20;
    private static final long JANELA_MS = 60_000;

    private final Map<String, Deque<Long>> envios = new ConcurrentHashMap<>();

    /** Registra a tentativa e diz se está dentro do limite. */
    public boolean permitir(String chaveUsuario) {
        return permitir(chaveUsuario, System.currentTimeMillis());
    }

    boolean permitir(String chaveUsuario, long agora) {
        String chave = chaveUsuario == null ? "anonimo" : chaveUsuario;
        Deque<Long> fila = envios.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > JANELA_MS) fila.pollFirst();
            if (fila.size() >= MAXIMO) return false;
            fila.addLast(agora);
        }
        // limpeza ocasional de usuários inativos
        if (envios.size() > 500) {
            envios.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || agora - e.getValue().peekLast() > JANELA_MS;
                }
            });
        }
        return true;
    }
}
