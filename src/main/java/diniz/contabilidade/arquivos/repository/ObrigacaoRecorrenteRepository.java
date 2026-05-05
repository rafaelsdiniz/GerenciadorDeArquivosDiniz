package diniz.contabilidade.arquivos.repository;

import java.util.List;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObrigacaoRecorrenteRepository implements PanacheRepository<ObrigacaoRecorrente> {

    public List<ObrigacaoRecorrente> buscarPorEmpresa(Empresa empresa) {
        return find("empresa", empresa).list();
    }

    public List<ObrigacaoRecorrente> buscarAtivas() {
        return find("ativo", true).list();
    }

    public List<ObrigacaoRecorrente> buscarAtivasPorEmpresa(Empresa empresa) {
        return find("empresa = ?1 and ativo = true", empresa).list();
    }
}
