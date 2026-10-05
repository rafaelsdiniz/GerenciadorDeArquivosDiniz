package diniz.contabilidade.arquivos.security;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Dados do usuário autenticado (extraídos do JWT) e regras de isolamento por empresa.
 *
 * ADMIN (escritório) enxerga todas as empresas.
 * FUNCIONARIO (cliente) só enxerga e altera dados da própria empresa. Registros de outra
 * empresa respondem 404, para não revelar que existem.
 */
@RequestScoped
public class UsuarioLogado {

    @Inject
    JsonWebToken jwt;

    public boolean isAdmin() {
        return jwt != null && jwt.getGroups() != null && jwt.getGroups().contains("ADMIN");
    }

    public Long usuarioId() {
        return claimLong("usuarioId");
    }

    public Long empresaId() {
        return claimLong("empresaId");
    }

    public String email() {
        return jwt != null ? jwt.getSubject() : null;
    }

    public boolean podeAcessarEmpresa(Long idEmpresa) {
        return isAdmin() || (idEmpresa != null && Objects.equals(idEmpresa, empresaId()));
    }

    /** Lança 404 se o usuário não puder ver a empresa. */
    public void exigirEmpresa(Long idEmpresa) {
        if (!podeAcessarEmpresa(idEmpresa)) {
            throw new NotFoundException("Registro não encontrado.");
        }
    }

    /** Lança 403 para ações exclusivas do escritório. */
    public void exigirAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("Ação permitida apenas para o escritório.");
        }
    }

    /** Filtra uma lista mantendo só itens das empresas visíveis ao usuário. */
    public <T> List<T> filtrar(List<T> itens, Function<T, Long> idEmpresa) {
        if (isAdmin()) return itens;
        Long minha = empresaId();
        return itens.stream().filter(i -> Objects.equals(idEmpresa.apply(i), minha)).toList();
    }

    /** Devolve o item se for visível; senão 404. */
    public <T> T verificar(T item, Function<T, Long> idEmpresa) {
        exigirEmpresa(idEmpresa.apply(item));
        return item;
    }

    private Long claimLong(String nome) {
        if (jwt == null) return null;
        Object v = jwt.getClaim(nome);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof jakarta.json.JsonNumber jn) return jn.longValue();
        try {
            return Long.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
