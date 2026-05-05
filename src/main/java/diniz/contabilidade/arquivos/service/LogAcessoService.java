package diniz.contabilidade.arquivos.service;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import diniz.contabilidade.arquivos.dto.response.LogAcessoResponseDTO;
import diniz.contabilidade.arquivos.model.entity.LogAcesso;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import diniz.contabilidade.arquivos.repository.LogAcessoRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LogAcessoService {

    @Inject
    LogAcessoRepository repository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    JsonWebToken jwt;

    @Transactional
    public void registrar(AcaoLog acao, String entidade, Long idEntidade, String detalhes) {
        registrarComUsuario(usuarioAtual(), acao, entidade, idEntidade, detalhes);
    }

    @Transactional
    public void registrarComUsuario(Usuario usuario, AcaoLog acao, String entidade, Long idEntidade, String detalhes) {
        LogAcesso log = new LogAcesso();
        log.setUsuario(usuario);
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setIdEntidade(idEntidade);
        log.setDetalhes(truncar(detalhes, 500));
        repository.persist(log);
    }

    public List<LogAcessoResponseDTO> listarRecentes(int limite) {
        return repository.listarRecentes(limite).stream().map(this::toResponseDTO).toList();
    }

    public List<LogAcessoResponseDTO> buscarPorEntidade(String entidade, Long idEntidade) {
        return repository.buscarPorEntidade(entidade, idEntidade).stream().map(this::toResponseDTO).toList();
    }

    public List<LogAcessoResponseDTO> buscarPorAcao(AcaoLog acao) {
        return repository.buscarPorAcao(acao).stream().map(this::toResponseDTO).toList();
    }

    public List<LogAcessoResponseDTO> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return repository.buscarPorPeriodo(inicio, fim).stream().map(this::toResponseDTO).toList();
    }

    private Usuario usuarioAtual() {
        if (jwt == null || jwt.getName() == null) {
            return null;
        }
        Object raw = jwt.getClaim("usuarioId");
        if (raw == null) {
            return null;
        }
        Long usuarioId;
        if (raw instanceof Number n) {
            usuarioId = n.longValue();
        } else if (raw instanceof jakarta.json.JsonNumber jn) {
            usuarioId = jn.longValue();
        } else {
            try {
                usuarioId = Long.parseLong(raw.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return usuarioRepository.findByIdOptional(usuarioId).orElse(null);
    }

    private String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private LogAcessoResponseDTO toResponseDTO(LogAcesso log) {
        return new LogAcessoResponseDTO(
                log.getId(),
                log.getDataCriacao(),
                log.getUsuario() != null ? log.getUsuario().getId() : null,
                log.getUsuario() != null ? log.getUsuario().getNome() : null,
                log.getAcao(),
                log.getEntidade(),
                log.getIdEntidade(),
                log.getDetalhes()
        );
    }
}
