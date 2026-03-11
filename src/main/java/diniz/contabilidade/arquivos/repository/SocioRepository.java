package diniz.contabilidade.arquivos.repository;

import java.util.List;
import java.util.Optional;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Socio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SocioRepository implements PanacheRepository<Socio> {

    public List<Socio> buscarPorEmpresa(Empresa empresa){
        return find("empresa", empresa).list();
    }

    public Optional<Socio> buscarPorCpf(String cpf){
        return find("cpf.numero", cpf).firstResultOptional();
    }
}