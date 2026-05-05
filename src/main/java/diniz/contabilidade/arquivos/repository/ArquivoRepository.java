package diniz.contabilidade.arquivos.repository;

import java.time.LocalDate;
import java.util.List;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ArquivoRepository implements PanacheRepository<Arquivo> {

    private static final String NAO_EXCLUIDO = "excluidoEm is null";

    public List<Arquivo> listarAtivos() {
        return find(NAO_EXCLUIDO).list();
    }

    public List<Arquivo> listarLixeira() {
        return find("excluidoEm is not null").list();
    }

    public List<Arquivo> buscarPorEmpresa(Empresa empresa) {
        return find("empresa = ?1 and " + NAO_EXCLUIDO, empresa).list();
    }

    public List<Arquivo> buscarPorPasta(Pasta pasta) {
        return find("pasta = ?1 and " + NAO_EXCLUIDO, pasta).list();
    }

    public List<Arquivo> buscarPorUsuario(Usuario usuario) {
        return find("usuario = ?1 and " + NAO_EXCLUIDO, usuario).list();
    }

    public List<Arquivo> buscarPorNome(String nome) {
        return find("LOWER(nome) LIKE ?1 and " + NAO_EXCLUIDO, "%" + nome.toLowerCase() + "%").list();
    }

    public List<Arquivo> buscarPorTipo(TipoArquivo tipoArquivo) {
        return find("tipoArquivo = ?1 and " + NAO_EXCLUIDO, tipoArquivo).list();
    }

    public List<Arquivo> buscarPorCategoria(CategoriaFiscal categoria) {
        return find("categoriaFiscal = ?1 and " + NAO_EXCLUIDO, categoria).list();
    }

    public List<Arquivo> buscarPorCategoriaEEmpresa(CategoriaFiscal categoria, Empresa empresa) {
        return find("categoriaFiscal = ?1 and empresa = ?2 and " + NAO_EXCLUIDO, categoria, empresa).list();
    }

    public List<Arquivo> buscarPorPastaEEmpresa(Pasta pasta, Empresa empresa) {
        return find("pasta = ?1 and empresa = ?2 and " + NAO_EXCLUIDO, pasta, empresa).list();
    }

    public List<Arquivo> buscarPorStatus(StatusArquivo status) {
        return find("status = ?1 and " + NAO_EXCLUIDO, status).list();
    }

    public List<Arquivo> buscarVencendoEm(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(dias);
        return find(
                "dataVencimento is not null and dataVencimento >= ?1 and dataVencimento <= ?2 and status <> ?3 and " + NAO_EXCLUIDO,
                hoje, limite, StatusArquivo.ARQUIVADO
        ).list();
    }

    public List<Arquivo> buscarVencidos() {
        return find(
                "dataVencimento is not null and dataVencimento < ?1 and status <> ?2 and " + NAO_EXCLUIDO,
                LocalDate.now(), StatusArquivo.ARQUIVADO
        ).list();
    }

    public List<Arquivo> buscarVencendoEmPorEmpresa(Empresa empresa, int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(dias);
        return find(
                "empresa = ?1 and dataVencimento is not null and dataVencimento >= ?2 and dataVencimento <= ?3 and status <> ?4 and " + NAO_EXCLUIDO,
                empresa, hoje, limite, StatusArquivo.ARQUIVADO
        ).list();
    }

    public List<Arquivo> buscarParaMarcarVencidos() {
        return find(
                "dataVencimento is not null and dataVencimento < ?1 and status not in (?2, ?3) and " + NAO_EXCLUIDO,
                LocalDate.now(), StatusArquivo.VENCIDO, StatusArquivo.ARQUIVADO
        ).list();
    }

    public List<Arquivo> buscarComVencimentoEntre(Empresa empresa, LocalDate inicio, LocalDate fim) {
        return find(
                "empresa = ?1 and dataVencimento is not null and dataVencimento >= ?2 and dataVencimento <= ?3 and " + NAO_EXCLUIDO,
                empresa, inicio, fim
        ).list();
    }

    public long contarPorEmpresa(Empresa empresa) {
        return count("empresa = ?1 and " + NAO_EXCLUIDO, empresa);
    }

    public long contarPorEmpresaEStatus(Empresa empresa, StatusArquivo status) {
        return count("empresa = ?1 and status = ?2 and " + NAO_EXCLUIDO, empresa, status);
    }
}
