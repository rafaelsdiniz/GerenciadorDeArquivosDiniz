package diniz.contabilidade.arquivos.resource;

import java.time.Duration;
import java.util.Set;

import diniz.contabilidade.arquivos.dto.request.LoginRequestDTO;
import diniz.contabilidade.arquivos.dto.response.TokenResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    UsuarioRepository usuarioRepository;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequestDTO dto) {

        Usuario usuario = usuarioRepository.buscarPorEmail(dto.email())
                .orElseThrow(() -> new NotAuthorizedException("Email ou senha inválidos."));

        if (!usuario.getSenha().equals(dto.senha())) {
            throw new NotAuthorizedException("Email ou senha inválidos.");
        }

        String token = Jwt.issuer("diniz-contabilidade")
                .subject(usuario.getEmail().getEndereco())
                .groups(Set.of(usuario.getPerfilUsuario().name()))
                .claim("usuarioId", usuario.getId())
                .claim("empresaId", usuario.getEmpresa().getId())
                .expiresIn(Duration.ofHours(8))
                .sign();

        return Response.ok(new TokenResponseDTO(token)).build();
    }
}