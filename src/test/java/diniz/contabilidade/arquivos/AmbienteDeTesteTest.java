package diniz.contabilidade.arquivos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import diniz.contabilidade.arquivos.service.DecIntegracaoService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Salvaguarda: os testes rodam num H2 em memória e com a integração do DEC desligada,
 * nunca no banco de desenvolvimento/produção nem no DEC real.
 */
@QuarkusTest
@DisplayName("Ambiente de teste isolado")
class AmbienteDeTesteTest {

    @Inject
    DataSource dataSource;

    @Inject
    DecIntegracaoService decService;

    @Test
    @DisplayName("O banco usado pelos testes é o H2 em memória")
    void bancoEmMemoria() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            String url = c.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:h2:mem:"), "URL inesperada: " + url);
        }
    }

    @Test
    @DisplayName("A integração com o DEC real fica desligada nos testes")
    void decDesligado() {
        assertFalse(decService.configurada());
    }
}
