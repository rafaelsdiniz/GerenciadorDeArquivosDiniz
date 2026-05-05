package diniz.contabilidade.arquivos.service;

import java.util.List;

import diniz.contabilidade.arquivos.dto.request.UsuarioRequestDTO;
import diniz.contabilidade.arquivos.dto.response.UsuarioResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.listAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        return toResponseDTO(usuario);
    }

    public List<UsuarioResponseDTO> buscarPorEmpresa(Long idEmpresa) {

        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        return usuarioRepository.buscarPorEmpresa(empresa)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public UsuarioResponseDTO salvar(UsuarioRequestDTO dto) {

        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        Usuario usuario = new Usuario();

        usuario.setNome(dto.nome());
        usuario.setEmail(new Email(dto.email()));
        usuario.setSenha(BcryptUtil.bcryptHash(dto.senha()));
        usuario.setPerfilUsuario(dto.perfilUsuario());
        usuario.setEmpresa(empresa);

        usuarioRepository.persist(usuario);

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        usuario.setNome(dto.nome());
        usuario.setEmail(new Email(dto.email()));
        if (dto.senha() != null && !dto.senha().isBlank()) {
            usuario.setSenha(BcryptUtil.bcryptHash(dto.senha()));
        }
        usuario.setPerfilUsuario(dto.perfilUsuario());
        usuario.setEmpresa(empresa);

        return toResponseDTO(usuario);
    }

    @Transactional
    public void deletar(Long id) {

        Usuario usuario = usuarioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        usuarioRepository.delete(usuario);
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail().getEndereco(),
                usuario.getPerfilUsuario(),
                usuario.getEmpresa().getId()
        );
    }
}