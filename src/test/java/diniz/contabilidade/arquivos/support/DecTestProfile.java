package diniz.contabilidade.arquivos.support;

import java.util.List;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Perfil dos testes de integração com o DEC: mesma configuração de teste (H2),
 * mais o servidor DEC falso. Por ter recursos próprios, a aplicação é reiniciada
 * só para esse grupo de testes.
 */
public class DecTestProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(FakeDecServer.class));
    }
}
