package diniz.contabilidade.arquivos.repository;

import java.time.LocalDate;
import java.util.List;

import diniz.contabilidade.arquivos.model.entity.Certidao;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CertidaoRepository implements PanacheRepository<Certidao> {

    private static final Sort ORDEM = Sort.ascending("dataValidade").and("id");

    public List<Certidao> listarTodas() {
        return findAll(ORDEM).list();
    }

    public List<Certidao> listarPorEmpresa(Long idEmpresa) {
        return find("empresa.id", ORDEM, idEmpresa).list();
    }

    public List<Certidao> buscarComValidadeEntre(Long idEmpresa, LocalDate inicio, LocalDate fim) {
        return find("empresa.id = ?1 and dataValidade between ?2 and ?3", ORDEM, idEmpresa, inicio, fim).list();
    }
}
