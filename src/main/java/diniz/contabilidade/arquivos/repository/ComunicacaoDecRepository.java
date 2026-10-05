package diniz.contabilidade.arquivos.repository;

import java.util.List;
import java.util.Optional;

import diniz.contabilidade.arquivos.model.entity.ComunicacaoDec;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComunicacaoDecRepository implements PanacheRepository<ComunicacaoDec> {

    private static final Sort ORDEM = Sort.descending("disponibilizadaEm").and("coletadaEm", Sort.Direction.Descending);

    public Optional<ComunicacaoDec> buscarPorIdExterno(String idExterno) {
        return find("idExterno", idExterno).firstResultOptional();
    }

    public List<ComunicacaoDec> listarTodas() {
        return findAll(ORDEM).list();
    }

    /** Liga à empresa as comunicações do DEC do mesmo CNPJ que estavam sem empresa. */
    public int vincularEmpresa(Empresa empresa) {
        if (empresa.getCnpj() == null || empresa.getCnpj().getNumero() == null) return 0;
        String cnpj = empresa.getCnpj().getNumero().replaceAll("\\D", "");
        return update("empresa = ?1 where cnpj = ?2 and empresa is null", empresa, cnpj);
    }

    public List<ComunicacaoDec> listarPorEmpresa(Long idEmpresa) {
        return find("empresa.id", ORDEM, idEmpresa).list();
    }
}
