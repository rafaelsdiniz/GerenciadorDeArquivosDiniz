package diniz.contabilidade.arquivos.repository;

import java.util.List;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArquivoRepository implements PanacheRepository<Arquivo> {

    public List<Arquivo> buscarPorEmpresa(Empresa empresa) {
        return find("empresa", empresa).list();
    }

    public List<Arquivo> buscarPorPasta(Pasta pasta) {
        return find("pasta", pasta).list();
    }

    public List<Arquivo> buscarPorUsuario(Usuario usuario) {
        return find("usuario", usuario).list();
    }

    public List<Arquivo> buscarPorNome(String nome) {
        return find("LOWER(nome) LIKE ?1", "%" + nome.toLowerCase() + "%").list();
    }

    public List<Arquivo> buscarPorTipo(TipoArquivo tipoArquivo) {
        return find("tipoArquivo", tipoArquivo).list();
    }

    public List<Arquivo> buscarPorPastaEEmpresa(Pasta pasta, Empresa empresa) {
        return find("pasta = ?1 and empresa = ?2", pasta, empresa).list();
    }
}