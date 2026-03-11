package diniz.contabilidade.arquivos.repository;

import java.util.List;
import java.util.Optional;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmpresaRepository implements PanacheRepository<Empresa> {

    public Optional<Empresa> buscarPorCnpj(String cnpj){
        return find("cnpj.numero", cnpj).firstResultOptional();
    }

    public List<Empresa> buscarPorNomeFantasia(String nomeFantasia){
        return find("LOWER(nomeFantasia) LIKE ?1", "%" + nomeFantasia.toLowerCase() + "%").list();
    }

    public List<Empresa> buscarPorRazaoSocial(String razaoSocial){
        return find("LOWER(razaoSocial) LIKE ?1", "%" + razaoSocial.toLowerCase() + "%").list();
    }
}