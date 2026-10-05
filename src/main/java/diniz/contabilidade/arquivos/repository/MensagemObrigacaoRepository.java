package diniz.contabilidade.arquivos.repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import diniz.contabilidade.arquivos.model.entity.MensagemObrigacao;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MensagemObrigacaoRepository implements PanacheRepository<MensagemObrigacao> {

    public List<MensagemObrigacao> listarPorObrigacao(Long idObrigacao) {
        return find("obrigacaoPendente.id = ?1 order by dataCriacao, id", idObrigacao).list();
    }

    /** Marca como lidas pelo escritório as mensagens do cliente nesta obrigação. */
    public int marcarLidasPeloEscritorio(Long idObrigacao, LocalDateTime agora) {
        return update("lidaPeloEscritorioEm = ?1 where obrigacaoPendente.id = ?2 and doEscritorio = false "
                + "and lidaPeloEscritorioEm is null", agora, idObrigacao);
    }

    /** Marca como lidas pelo cliente as mensagens do escritório nesta obrigação. */
    public int marcarLidasPeloCliente(Long idObrigacao, LocalDateTime agora) {
        return update("lidaPeloClienteEm = ?1 where obrigacaoPendente.id = ?2 and doEscritorio = true "
                + "and lidaPeloClienteEm is null", agora, idObrigacao);
    }

    /**
     * Não lidas por obrigação para um dos lados.
     *
     * @param escritorio true = mensagens do cliente ainda não lidas pelo escritório (todas as empresas);
     *                   false = mensagens do escritório ainda não lidas pelo cliente da empresa informada.
     */
    public Map<Long, Long> naoLidasPorObrigacao(boolean escritorio, Long idEmpresa) {
        String jpql = escritorio
                ? "select m.obrigacaoPendente.id, count(m) from MensagemObrigacao m "
                        + "where m.doEscritorio = false and m.lidaPeloEscritorioEm is null "
                        + "group by m.obrigacaoPendente.id"
                : "select m.obrigacaoPendente.id, count(m) from MensagemObrigacao m "
                        + "where m.doEscritorio = true and m.lidaPeloClienteEm is null "
                        + "and m.obrigacaoPendente.empresa.id = :empresa "
                        + "group by m.obrigacaoPendente.id";
        var query = getEntityManager().createQuery(jpql, Object[].class);
        if (!escritorio) {
            query.setParameter("empresa", idEmpresa);
        }
        Map<Long, Long> resultado = new LinkedHashMap<>();
        for (Object[] linha : query.getResultList()) {
            resultado.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return resultado;
    }

    /** Última mensagem de cada conversa, da mais recente para a mais antiga. idEmpresa null = todas. */
    public List<MensagemObrigacao> ultimasPorConversa(Long idEmpresa, int limite) {
        String base = "from MensagemObrigacao m where m.id = (select max(m2.id) from MensagemObrigacao m2 "
                + "where m2.obrigacaoPendente = m.obrigacaoPendente)";
        String ordem = " order by m.dataCriacao desc, m.id desc";
        if (idEmpresa == null) {
            return find(base + ordem).page(Page.ofSize(limite)).list();
        }
        return find(base + " and m.obrigacaoPendente.empresa.id = ?1" + ordem, idEmpresa)
                .page(Page.ofSize(limite)).list();
    }
}
