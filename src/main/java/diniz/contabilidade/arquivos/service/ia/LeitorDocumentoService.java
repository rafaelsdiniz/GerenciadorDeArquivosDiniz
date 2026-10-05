package diniz.contabilidade.arquivos.service.ia;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import diniz.contabilidade.arquivos.dto.response.DocumentoAnalisado;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;
import jakarta.ws.rs.NotFoundException;

/**
 * Leitura inteligente de documentos: extrai o texto, lê por padrões e, se houver chave configurada,
 * consulta a IA (DeepSeek), validando e conferindo o resultado com a empresa/obrigação.
 *
 * Configuração: DEEPSEEK_API_KEY (sem ela, só a leitura por padrões), DEEPSEEK_URL, DEEPSEEK_MODEL.
 */
@ApplicationScoped
public class LeitorDocumentoService {

    private static final Logger LOG = Logger.getLogger(LeitorDocumentoService.class);
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private static final int CACHE_MAX = 64;

    @ConfigProperty(name = "ia.deepseek.api-key")
    Optional<String> chave;

    @ConfigProperty(name = "ia.deepseek.url", defaultValue = "https://api.deepseek.com")
    String url;

    @ConfigProperty(name = "ia.deepseek.modelo", defaultValue = "deepseek-chat")
    String modelo;

    @ConfigProperty(name = "ia.deepseek.timeout-segundos", defaultValue = "25")
    int timeoutSegundos;

    @Inject
    ObjectMapper mapper;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    TransactionManager transactionManager;

    private AnalisadorDocumento analisador;
    private final Map<String, Cacheado> cache = new ConcurrentHashMap<>();
    private final ExecutorService fila = Executors.newVirtualThreadPerTaskExecutor();

    private record Cacheado(DocumentoAnalisado resultado, long em) {}

    @PostConstruct
    void iniciar() {
        DeepSeekCliente cliente = configurada()
                ? new DeepSeekCliente(url, chave.get().trim(), modelo, Duration.ofSeconds(timeoutSegundos), mapper)
                : null;
        analisador = new AnalisadorDocumento(cliente);
        LOG.infof("[ia] leitura inteligente: %s", configurada() ? "IA ativa (" + modelo + ")" : "somente leitura por padrões (sem DEEPSEEK_API_KEY)");
    }

    @PreDestroy
    void parar() {
        fila.shutdownNow();
    }

    public boolean configurada() {
        return chave.filter(s -> !s.isBlank()).isPresent();
    }

    public String modelo() {
        return modelo;
    }

    // ------------------------------------------------------------------ análise

    /** Analisa sem gravar nada. Empresa e obrigação são opcionais (usadas nas conferências). */
    public DocumentoAnalisado analisar(byte[] conteudo, String nomeArquivo, Empresa empresa, ObrigacaoPendente obrigacao) {
        ContextoLeitura ctx = contexto(empresa, obrigacao);
        String chaveCache = chaveCache(conteudo, ctx);
        Cacheado c = chaveCache != null ? cache.get(chaveCache) : null;
        if (c != null && System.currentTimeMillis() - c.em() < CACHE_TTL_MS) return c.resultado();

        DocumentoAnalisado r = analisador.analisar(conteudo, nomeArquivo, ctx);
        boolean iaFalhou = r.alertas().stream().anyMatch(a -> "IA_INDISPONIVEL".equals(a.codigo()));
        if (chaveCache != null && !iaFalhou) {
            if (cache.size() >= CACHE_MAX) cache.clear();
            cache.put(chaveCache, new Cacheado(r, System.currentTimeMillis()));
        }
        return r;
    }

    /** Lê um arquivo já armazenado e grava os dados extraídos (preenche descrição/categoria/vencimento vazios). */
    public DocumentoAnalisado analisarArquivo(Long idArquivo) {
        Entrada e = QuarkusTransaction.requiringNew().call(() -> carregar(idArquivo));
        if (e.conteudo() == null) {
            throw new IllegalArgumentException("Este arquivo não tem conteúdo armazenado (dado de demonstração) — envie o documento real para ler.");
        }
        DocumentoAnalisado r = analisar(e.conteudo(), e.nome(), e.empresa(), e.obrigacao());
        QuarkusTransaction.requiringNew().run(() -> gravar(idArquivo, r, true));
        return r;
    }

    /**
     * Agenda a leitura para depois do commit do upload (em segundo plano). Nunca lança exceção:
     * uma falha aqui não pode atrapalhar o envio do arquivo.
     */
    public void agendarAposCommit(Long idArquivo) {
        try {
            transactionManager.getTransaction().registerSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_COMMITTED) fila.submit(() -> lerEmSegundoPlano(idArquivo));
                }
            });
        } catch (Exception ex) {
            LOG.debugf("[ia] não foi possível agendar a leitura do arquivo %d: %s", idArquivo, ex.getMessage());
        }
    }

    private void lerEmSegundoPlano(Long idArquivo) {
        ManagedContext req = Arc.container() != null ? Arc.container().requestContext() : null;
        boolean ativou = false;
        try {
            if (req != null && !req.isActive()) { req.activate(); ativou = true; }
            Entrada e = QuarkusTransaction.requiringNew().call(() -> carregar(idArquivo));
            if (e.conteudo() == null) return;
            DocumentoAnalisado r = analisar(e.conteudo(), e.nome(), e.empresa(), e.obrigacao());
            if (!r.textoLegivel()) return;
            QuarkusTransaction.requiringNew().run(() -> gravar(idArquivo, r, false));
        } catch (Exception ex) {
            LOG.warnf("[ia] leitura automática do arquivo %d falhou: %s", idArquivo, ex.getMessage());
        } finally {
            if (ativou) req.terminate();
        }
    }

    private record Entrada(byte[] conteudo, String nome, Empresa empresa, ObrigacaoPendente obrigacao) {}

    private Entrada carregar(Long idArquivo) {
        Arquivo a = arquivoRepository.findByIdOptional(idArquivo)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
        byte[] bytes = a.getArquivoBase64() != null ? Base64.getDecoder().decode(a.getArquivoBase64()) : null;
        ObrigacaoPendente o = a.getObrigacaoPendente();
        if (o != null && o.getObrigacaoRecorrente() != null) o.getObrigacaoRecorrente().getNome(); // inicializa
        return new Entrada(bytes, a.getNomeOriginal(), a.getEmpresa(), o);
    }

    private void gravar(Long idArquivo, DocumentoAnalisado r, boolean preencherVazios) {
        Arquivo a = arquivoRepository.findById(idArquivo);
        if (a == null) return;
        a.setValor(r.valor());
        a.setLinhaDigitavel(r.linhaDigitavel());
        a.setCompetenciaDocumento(r.competencia());
        a.setCnpjDocumento(r.cnpj());
        a.setTipoDocumento(r.tipoDocumento());
        a.setFonteLeitura(r.fonte());
        a.setAnalisadoEm(LocalDateTime.now());
        // alertas que dependem do dia (ex.: "a guia venceu") ficam só na tela da análise
        String alertas = r.alertas().stream()
                .filter(x -> !"VENCIDO".equals(x.codigo()) && !"IA_INDISPONIVEL".equals(x.codigo()))
                .map(x -> x.mensagem().replace('\n', ' '))
                .collect(Collectors.joining("\n"));
        a.setAlertasLeitura(alertas.length() > 1990 ? alertas.substring(0, 1990) : alertas);
        try {
            a.setDadosIaJson(mapper.writeValueAsString(r));
        } catch (Exception ex) {
            a.setDadosIaJson(null);
        }
        if (preencherVazios) {
            if ((a.getDescricao() == null || a.getDescricao().isBlank()) && r.descricaoSugerida() != null) a.setDescricao(r.descricaoSugerida());
            if (a.getCategoriaFiscal() == null && r.categoriaFiscalSugerida() != null && r.tipoDocumento() != diniz.contabilidade.arquivos.model.enums.TipoDocumento.OUTRO) {
                a.setCategoriaFiscal(r.categoriaFiscalSugerida());
            }
            if (a.getDataVencimento() == null && r.vencimento() != null && r.tipoDocumento().isGuia()) a.setDataVencimento(r.vencimento());
        }
    }

    // ------------------------------------------------------------------ contexto

    public static ContextoLeitura contexto(Empresa empresa, ObrigacaoPendente obrigacao) {
        Empresa e = empresa != null ? empresa : (obrigacao != null ? obrigacao.getEmpresa() : null);
        String cnpj = e != null && e.getCnpj() != null ? e.getCnpj().getNumero() : null;
        String nomeEmpresa = e != null ? e.getNomeFantasia() : null;
        if (obrigacao == null) return new ContextoLeitura(cnpj, nomeEmpresa, null, null, null);
        String nome = obrigacao.getObrigacaoRecorrente() != null ? obrigacao.getObrigacaoRecorrente().getNome() : null;
        return new ContextoLeitura(cnpj, nomeEmpresa, nome, obrigacao.getDataVencimento(), competencia(obrigacao));
    }

    /** Mesma regra do ObrigacaoPendenteService: vencimento menos 1 mês (anual = ano anterior). */
    static String competencia(ObrigacaoPendente p) {
        if (p.getDataVencimento() == null) return null;
        Periodicidade per = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getPeriodicidade() : Periodicidade.MENSAL;
        if (per == Periodicidade.ANUAL) return String.valueOf(p.getDataVencimento().getYear() - 1);
        YearMonth ref = YearMonth.from(p.getDataVencimento()).minusMonths(1);
        return String.format("%02d/%d", ref.getMonthValue(), ref.getYear());
    }

    private static String chaveCache(byte[] conteudo, ContextoLeitura ctx) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(md.digest(conteudo));
            return hash + "|" + ctx;
        } catch (Exception e) {
            return null;
        }
    }

    /** Usado nos testes. */
    List<String> chavesEmCache() {
        return List.copyOf(cache.keySet());
    }
}
