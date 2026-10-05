package diniz.contabilidade.arquivos.support;

import java.time.Duration;
import java.util.Set;

import diniz.contabilidade.arquivos.model.entity.Usuario;
import io.smallrye.jwt.build.Jwt;

/**
 * Gera JWTs iguais aos emitidos pelo /auth/login (mesmo emissor, grupos e claims),
 * assinados com a chave de desenvolvimento do classpath. Evita depender do login
 * em todos os testes.
 */
public final class TokenTeste {

    private TokenTeste() {}

    /** Token do usuário informado (perfil, usuarioId e empresaId vêm do próprio usuário). */
    public static String de(Usuario usuario) {
        return gerar(usuario.getEmail().getEndereco(), usuario.getPerfilUsuario().name(),
                usuario.getId(), usuario.getEmpresa().getId());
    }

    public static String gerar(String email, String perfil, Long usuarioId, Long empresaId) {
        return Jwt.issuer("diniz-contabilidade")
                .subject(email)
                .groups(Set.of(perfil))
                .claim("usuarioId", usuarioId)
                .claim("empresaId", empresaId)
                .expiresIn(Duration.ofHours(1))
                .sign();
    }
}
