package diniz.contabilidade.arquivos.repository;

import java.util.List;

import diniz.contabilidade.arquivos.model.entity.FechamentoMensal;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FechamentoMensalRepository implements PanacheRepository<FechamentoMensal> {

    public List<FechamentoMensal> buscarPorCompetencia(String competencia) {
        return find("competencia = ?1 order by empresa.nomeFantasia", competencia).list();
    }

    public List<String> competenciasExistentes() {
        return getEntityManager()
                .createQuery("select distinct f.competencia from FechamentoMensal f", String.class)
                .getResultList();
    }
}
