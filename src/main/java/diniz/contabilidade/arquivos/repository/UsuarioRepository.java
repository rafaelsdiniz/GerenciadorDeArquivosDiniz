package diniz.contabilidade.arquivos.repository;

import java.util.List;
import java.util.Optional;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepository<Usuario> {

    public Optional<Usuario> buscarPorEmail(String email){
        return find("email.endereco", email).firstResultOptional();
    }

    public List<Usuario> buscarPorEmpresa(Empresa empresa){
        return find("empresa", empresa).list();
    }

}