package diniz.contabilidade.arquivos.repository;

import java.time.LocalDateTime;
import java.util.List;

import diniz.contabilidade.arquivos.model.entity.LogAcesso;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogAcessoRepository implements PanacheRepository<LogAcesso> {

    public List<LogAcesso> listarRecentes(int limite) {
        return find("order by dataCriacao desc").page(0, limite).list();
    }

    public List<LogAcesso> buscarPorUsuario(Usuario usuario) {
        return find("usuario = ?1 order by dataCriacao desc", usuario).list();
    }

    public List<LogAcesso> buscarPorEntidade(String entidade, Long idEntidade) {
        return find("entidade = ?1 and idEntidade = ?2 order by dataCriacao desc", entidade, idEntidade).list();
    }

    public List<LogAcesso> buscarPorAcao(AcaoLog acao) {
        return find("acao = ?1 order by dataCriacao desc", acao).list();
    }

    public List<LogAcesso> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return find("dataCriacao >= ?1 and dataCriacao <= ?2 order by dataCriacao desc", inicio, fim).list();
    }
}
