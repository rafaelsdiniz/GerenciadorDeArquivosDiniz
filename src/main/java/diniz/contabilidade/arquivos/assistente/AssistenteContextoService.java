package diniz.contabilidade.arquivos.assistente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.config.FusoHorario;
import diniz.contabilidade.arquivos.dto.response.CertidaoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.CertidaoService;
import diniz.contabilidade.arquivos.service.DecIntegracaoService;
import diniz.contabilidade.arquivos.service.ObrigacaoPendenteService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Monta o {@link AssistenteContexto} do usuário logado a partir dos serviços existentes.
 *
 * Isolamento: o FUNCIONARIO (cliente) recebe só dados da própria empresa — as consultas já
 * são feitas por empresa e, por segurança, tudo passa de novo por {@link UsuarioLogado#filtrar}.
 * Cada bloco é tolerante a falha: se um módulo der erro, o assistente segue com os demais.
 */
@ApplicationScoped
public class AssistenteContextoService {

    private static final Logger LOG = Logger.getLogger(AssistenteContextoService.class);

    @Inject
    UsuarioLogado usuario;

    @Inject
    ObrigacaoPendenteService obrigacaoService;

    @Inject
    DecIntegracaoService decService;

    @Inject
    CertidaoService certidaoService;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    /** Transação curta só de leitura: a conexão é devolvida antes da chamada à IA. */
    @Transactional
    public AssistenteContexto montar() {
        AssistenteContexto ctx = identificar();
        Long idEmpresa = ctx.idEmpresa;

        // cliente sem empresa vinculada: nada a mostrar (nunca cai para "todas")
        if (!ctx.escritorio && idEmpresa == null) {
            return ctx;
        }

        Empresa empresa = idEmpresa == null ? null : seguro("empresa", () -> empresaRepository.findById(idEmpresa), null);
        if (empresa != null) {
            ctx.nomeEmpresa = empresa.getNomeFantasia() != null && !empresa.getNomeFantasia().isBlank()
                    ? empresa.getNomeFantasia() : empresa.getRazaoSocial();
        }

        // ------------------------------------------------------------ obrigações
        List<ObrigacaoPendenteResponseDTO> obrigacoes = seguro("obrigações",
                () -> ctx.escritorio ? obrigacaoService.listar() : obrigacaoService.buscarPorEmpresa(idEmpresa), List.of());
        obrigacoes = usuario.filtrar(obrigacoes, ObrigacaoPendenteResponseDTO::idEmpresa);
        for (ObrigacaoPendenteResponseDTO o : obrigacoes) {
            if (o.status() != StatusObrigacao.ENTREGUE) {
                ctx.abertas.add(o);
            } else if ("AGUARDANDO".equals(o.situacaoPagamento()) || "ATRASADO".equals(o.situacaoPagamento())) {
                ctx.aPagar.add(o);
            }
        }

        // ------------------------------------------------------------ DEC
        List<ComunicacaoDecResponseDTO> dec = seguro("DEC",
                () -> ctx.escritorio ? decService.listar() : decService.listarPorEmpresa(idEmpresa), List.of());
        dec = usuario.filtrar(dec, ComunicacaoDecResponseDTO::idEmpresa);
        for (ComunicacaoDecResponseDTO c : dec) {
            boolean aberta = !Boolean.TRUE.equals(c.encerrada()) && !"RESOLVIDA".equals(c.status()) && !"ARQUIVADA".equals(c.status());
            if (aberta && c.cienteEm() == null) ctx.decSemCiencia.add(c);
        }

        // ------------------------------------------------------------ certidões
        List<CertidaoResponseDTO> certidoes = seguro("certidões",
                () -> ctx.escritorio ? certidaoService.listar() : certidaoService.listarPorEmpresa(idEmpresa), List.of());
        certidoes = usuario.filtrar(certidoes, CertidaoResponseDTO::idEmpresa);
        for (CertidaoResponseDTO c : certidoes) {
            if (CertidaoService.VENCENDO.equals(c.statusValidade()) || CertidaoService.VENCIDA.equals(c.statusValidade())) {
                ctx.certidoesAlerta.add(c);
            }
        }

        // ------------------------------------------------------------ arquivos / empresas
        if (ctx.escritorio) {
            ctx.totalArquivos = seguro("arquivos", () -> arquivoRepository.count("excluidoEm is null"), 0L);
            ctx.totalEmpresas = seguro("empresas", () -> empresaRepository.count(), 0L);
            ctx.empresas = resumoPorEmpresa(ctx, obrigacoes, dec, certidoes);
            List<Empresa> todas = seguro("empresas", () -> empresaRepository.listAll(), List.of());
            for (Empresa e : todas) {
                String nome = e.getNomeFantasia() != null && !e.getNomeFantasia().isBlank() ? e.getNomeFantasia() : e.getRazaoSocial();
                if (nome != null) ctx.nomesEmpresas.put(e.getId(), nome);
            }
        } else if (empresa != null) {
            ctx.totalArquivos = seguro("arquivos", () -> arquivoRepository.contarPorEmpresa(empresa), 0L);
            ctx.totalEmpresas = 1;
        }

        ctx.ordenar();
        return ctx;
    }

    /** Só perfil e nome do usuário (leve, para o GET /assistente/status). */
    @Transactional
    public AssistenteContexto identificar() {
        AssistenteContexto ctx = new AssistenteContexto();
        ctx.escritorio = usuario.isAdmin();
        ctx.hoje = LocalDate.now(java.time.ZoneId.of(FusoHorario.FUSO));
        ctx.idEmpresa = ctx.escritorio ? null : usuario.empresaId();
        Usuario u = seguro("usuário", () -> usuario.usuarioId() == null ? null : usuarioRepository.findById(usuario.usuarioId()), null);
        if (u != null && u.getNome() != null && !u.getNome().isBlank()) {
            ctx.nomeUsuario = u.getNome().trim();
        } else if (usuario.email() != null) {
            ctx.nomeUsuario = usuario.email().split("@")[0];
        }
        return ctx;
    }

    private List<AssistenteContexto.ResumoEmpresa> resumoPorEmpresa(AssistenteContexto ctx,
            List<ObrigacaoPendenteResponseDTO> obrigacoes, List<ComunicacaoDecResponseDTO> dec, List<CertidaoResponseDTO> certidoes) {
        Map<Long, int[]> cont = new HashMap<>();
        Map<Long, String> nomes = new HashMap<>();
        // índices: 0 vencidas, 1 semana, 2 aEnviar, 3 aPagar, 4 decAtencao, 5 certidoes
        for (ObrigacaoPendenteResponseDTO o : ctx.abertas) {
            if (o.idEmpresa() == null) continue;
            int[] c = cont.computeIfAbsent(o.idEmpresa(), k -> new int[6]);
            nomes.putIfAbsent(o.idEmpresa(), o.nomeEmpresa());
            if (AssistenteContexto.vencida(o)) c[0]++;
            else if (o.diasParaVencer() != null && o.diasParaVencer() >= 0 && o.diasParaVencer() <= 7) c[1]++;
            if (AssistenteContexto.doCliente(o)) c[2]++;
        }
        for (ObrigacaoPendenteResponseDTO o : ctx.aPagar) {
            if (o.idEmpresa() == null) continue;
            cont.computeIfAbsent(o.idEmpresa(), k -> new int[6])[3]++;
            nomes.putIfAbsent(o.idEmpresa(), o.nomeEmpresa());
        }
        for (ComunicacaoDecResponseDTO c : ctx.decSemCiencia) {
            if (c.idEmpresa() == null || !AssistenteContexto.decAtencao(c)) continue;
            cont.computeIfAbsent(c.idEmpresa(), k -> new int[6])[4]++;
            nomes.putIfAbsent(c.idEmpresa(), c.nomeEmpresa());
        }
        for (CertidaoResponseDTO c : ctx.certidoesAlerta) {
            if (c.idEmpresa() == null) continue;
            cont.computeIfAbsent(c.idEmpresa(), k -> new int[6])[5]++;
            nomes.putIfAbsent(c.idEmpresa(), c.nomeEmpresa());
        }
        List<AssistenteContexto.ResumoEmpresa> lista = new ArrayList<>();
        cont.forEach((id, c) -> lista.add(new AssistenteContexto.ResumoEmpresa(id,
                Objects.requireNonNullElse(nomes.get(id), "Empresa " + id), c[0], c[1], c[2], c[3], c[4], c[5])));
        return lista;
    }

    private <T> T seguro(String bloco, Supplier<T> consulta, T padrao) {
        try {
            T v = consulta.get();
            return v == null ? padrao : v;
        } catch (RuntimeException e) {
            // só o tipo do erro: nada de dados do usuário no log
            LOG.warnf("Assistente: não foi possível ler %s (%s)", bloco, e.getClass().getSimpleName());
            return padrao;
        }
    }
}
