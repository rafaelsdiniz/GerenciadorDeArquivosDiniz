package diniz.contabilidade.arquivos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObrigacaoPendenteRepository implements PanacheRepository<ObrigacaoPendente> {

    public List<ObrigacaoPendente> buscarPorEmpresa(Empresa empresa) {
        return find("empresa", empresa).list();
    }

    public List<ObrigacaoPendente> buscarPendentesPorEmpresa(Empresa empresa) {
        return find("empresa = ?1 and status = ?2", empresa, StatusObrigacao.PENDENTE).list();
    }

    public List<ObrigacaoPendente> buscarVencidas() {
        return find("status = ?1", StatusObrigacao.VENCIDA).list();
    }

    public List<ObrigacaoPendente> buscarParaMarcarVencidas() {
        return find(
                "status = ?1 and dataVencimento < ?2",
                StatusObrigacao.PENDENTE, LocalDate.now()
        ).list();
    }

    public List<ObrigacaoPendente> buscarVencendoEm(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(dias);
        return find(
                "status = ?1 and dataVencimento >= ?2 and dataVencimento <= ?3",
                StatusObrigacao.PENDENTE, hoje, limite
        ).list();
    }

    public Optional<ObrigacaoPendente> buscarPorRecorrenteEData(ObrigacaoRecorrente recorrente, LocalDate data) {
        return find("obrigacaoRecorrente = ?1 and dataVencimento = ?2", recorrente, data).firstResultOptional();
    }
}
