package diniz.contabilidade.arquivos.repository;

import java.util.List;

import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PastaRepository implements PanacheRepository<Pasta> {

    public List<Pasta> buscarPorEmpresa(Empresa empresa){
        return find("empresa", empresa).list();
    }

    public List<Pasta> buscarSubpastas(Pasta pastaPai){
        return find("pastaPai", pastaPai).list();
    }

    public List<Pasta> buscarPastasRaiz(Empresa empresa){
        return find("empresa = ?1 and pastaPai is null", empresa).list();
    }

    public List<Pasta> buscarPorNome(String nome){
        return find("LOWER(nome) LIKE ?1", "%" + nome.toLowerCase() + "%").list();
    }
}