package diniz.contabilidade.arquivos.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import diniz.contabilidade.arquivos.dto.response.ComunicacaoDecResponseDTO;
import diniz.contabilidade.arquivos.model.entity.ComunicacaoDec;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.repository.ComunicacaoDecRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Traz para o Gerenciador tudo o que chegou no DEC (Domicílio Eletrônico do
 * Contribuinte / SEFAZ-TO), lendo a API de integração do DEC Monitor.
 *
 * Somente leitura: nada é enviado ao DEC. A cada sincronização a janela inteira
 * (dec.dias) é relida e gravada por upsert no id externo — assim chegam as
 * comunicações novas e também as mudanças de status, ciência e prazo.
 *
 * Configuração (variáveis de ambiente):
 *   DEC_URL   — endereço do DEC Monitor (ex.: https://domicilio-....vercel.app)
 *   DEC_TOKEN — mesmo valor de INTEGRACAO_TOKEN no DEC
 * Sem as duas, a integração fica desligada e as telas mostram como configurar.
 */
@ApplicationScoped
public class DecIntegracaoService {

    private static final Logger LOG = Logger.getLogger(DecIntegracaoService.class);
    private static final ZoneId FUSO = ZoneId.of("America/Araguaina");

    @ConfigProperty(name = "dec.url")
    Optional<String> url;

    @ConfigProperty(name = "dec.token")
    Optional<String> token;

    @ConfigProperty(name = "dec.dias", defaultValue = "180")
    int dias;

    /** Modo demonstração: comunicações fictícias, sem chamar o DEC (versão pública, LGPD). */
    @ConfigProperty(name = "dec.demo", defaultValue = "false")
    boolean demo;

    @Inject
    ComunicacaoDecRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ObjectMapper mapper;

    // HTTP/1.1: o upgrade para HTTP/2 em texto puro falha com o servidor do Next em desenvolvimento
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // estado da última sincronização (em memória; some no restart, a próxima rodada repõe)
    private volatile LocalDateTime ultimaSincronizacao;
    private volatile String ultimoErro;
    private volatile int ultimoTotal;
    private volatile int ultimasNovas;
    private volatile String escritorioDec;

    public record StatusIntegracao(
            boolean configurada,
            String url,
            LocalDateTime ultimaSincronizacao,
            String ultimoErro,
            int ultimoTotal,
            int ultimasNovas,
            String escritorio,
            long semEmpresa) {}

    public record ResultadoSincronizacao(int recebidas, int novas, int atualizadas, int semEmpresa) {}

    public boolean configurada() {
        return demo || (url.filter(s -> !s.isBlank()).isPresent() && token.filter(s -> !s.isBlank()).isPresent());
    }

    public StatusIntegracao status() {
        long semEmpresa = configurada() ? repository.count("empresa is null") : 0;
        return new StatusIntegracao(configurada(), demo ? "demonstração (dados fictícios)" : url.orElse(null), ultimaSincronizacao, ultimoErro,
                ultimoTotal, ultimasNovas, escritorioDec, semEmpresa);
    }

    /** Roda a cada 10 min (e logo após subir). Sem configuração, não faz nada. */
    @Scheduled(every = "10m", delayed = "20s", identity = "sync-dec",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sincronizarAgendado() {
        if (!configurada()) return;
        try {
            sincronizar();
        } catch (Exception e) {
            LOG.warnf("[dec] falha na sincronização agendada: %s", e.getMessage());
        }
    }

    /** Sincroniza agora. Lança IllegalStateException com mensagem amigável em caso de erro. */
    public synchronized ResultadoSincronizacao sincronizar() {
        if (!configurada()) {
            throw new IllegalStateException("Integração com o DEC não configurada (defina DEC_URL e DEC_TOKEN).");
        }
        if (demo) {
            int novas = QuarkusTransaction.requiringNew().call(this::carregarDemo);
            ultimaSincronizacao = LocalDateTime.now(FUSO);
            ultimoErro = null;
            ultimoTotal = (int) repository.count();
            ultimasNovas = novas;
            escritorioDec = "Diniz Assessoria Contábil (demonstração)";
            return new ResultadoSincronizacao(ultimoTotal, novas, ultimoTotal - novas, 0);
        }
        try {
            JsonNode raiz = buscar();
            ResultadoSincronizacao r = QuarkusTransaction.requiringNew().call(() -> gravar(raiz));
            ultimaSincronizacao = LocalDateTime.now(FUSO);
            ultimoErro = null;
            ultimoTotal = r.recebidas();
            ultimasNovas = r.novas();
            LOG.infof("[dec] sincronizado: %d recebidas, %d novas, %d atualizadas, %d sem empresa",
                    r.recebidas(), r.novas(), r.atualizadas(), r.semEmpresa());
            return r;
        } catch (IllegalStateException e) {
            ultimoErro = e.getMessage();
            throw e;
        } catch (Exception e) {
            ultimoErro = "Não foi possível falar com o DEC: " + e.getMessage();
            throw new IllegalStateException(ultimoErro, e);
        }
    }

    private JsonNode buscar() throws Exception {
        String base = url.get().replaceAll("/+$", "");
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/api/integracao/comunicacoes?dias=" + dias + "&limite=5000"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token.get())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            throw new IllegalStateException("O DEC recusou a chave (401). Confira DEC_TOKEN e INTEGRACAO_TOKEN.");
        }
        if (resp.statusCode() == 404) {
            throw new IllegalStateException("O DEC não encontrou o escritório da integração (404). Confira INTEGRACAO_ESCRITORIO_CNPJ no DEC, ou se a versão com a integração já foi publicada.");
        }
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("O DEC respondeu HTTP " + resp.statusCode() + ".");
        }
        return mapper.readTree(resp.body());
    }

    ResultadoSincronizacao gravar(JsonNode raiz) {
        JsonNode esc = raiz.path("escritorio");
        escritorioDec = esc.isMissingNode() ? null : esc.path("nome").asText(null);

        Map<String, Empresa> porCnpj = new HashMap<>();
        for (Empresa e : empresaRepository.listAll()) {
            if (e.getCnpj() != null && e.getCnpj().getNumero() != null) {
                porCnpj.put(digitos(e.getCnpj().getNumero()), e);
            }
        }

        int novas = 0, atualizadas = 0, semEmpresa = 0, recebidas = 0;
        LocalDateTime agora = LocalDateTime.now(FUSO);
        for (JsonNode n : raiz.path("itens")) {
            recebidas++;
            String idExterno = n.path("id").asText();
            if (idExterno.isBlank()) continue;
            ComunicacaoDec c = repository.buscarPorIdExterno(idExterno).orElse(null);
            boolean nova = c == null;
            if (nova) {
                c = new ComunicacaoDec();
                c.setIdExterno(idExterno);
            }
            String cnpj = digitos(n.path("cnpj").asText(""));
            Empresa empresa = porCnpj.get(cnpj);
            if (empresa == null) semEmpresa++;

            c.setEmpresa(empresa);
            c.setCnpj(cnpj);
            c.setRazaoSocial(texto(n, "razaoSocial"));
            c.setNumero(texto(n, "numero"));
            c.setTipo(texto(n, "tipo"));
            c.setAssunto(corta(texto(n, "assunto"), 1000));
            c.setCorpo(texto(n, "corpo"));
            c.setRemetente(texto(n, "remetente"));
            c.setDisponibilizadaEm(data(n, "disponibilizadaEm"));
            c.setCienteEm(data(n, "cienteEm"));
            c.setPrazoCienciaEm(data(n, "prazoCienciaEm"));
            c.setDiasParaResposta(n.hasNonNull("diasParaResposta") ? n.get("diasParaResposta").asInt() : null);
            c.setColetadaEm(instante(n, "coletadaEm"));
            c.setStatus(texto(n, "status"));
            c.setUrgencia(texto(n, "urgencia"));
            c.setMotivo(corta(texto(n, "motivo"), 500));
            c.setCienciaTacitaEm(data(n, "cienciaTacitaEm"));
            c.setPrazoRespostaEm(data(n, "prazoRespostaEm"));
            c.setEncerrada(n.path("encerrada").asBoolean(false));
            c.setTemInteiroTeor(n.path("temInteiroTeor").asBoolean(false));
            c.setLink(corta(texto(n, "link"), 500));
            c.setSincronizadaEm(agora);

            if (nova) {
                repository.persist(c);
                novas++;
            } else {
                atualizadas++;
            }
        }
        return new ResultadoSincronizacao(recebidas, novas, atualizadas, semEmpresa);
    }

    // ------------------------------------------------------------ demonstração

    /**
     * Comunicações fictícias, ligadas às empresas de exemplo pelo CNPJ e com datas
     * relativas a hoje. Só insere se a tabela estiver vazia. Nenhum dado real.
     */
    int carregarDemo() {
        if (repository.count() > 0) return 0;
        List<Empresa> empresas = empresaRepository.listAll();
        if (empresas.isEmpty()) return 0;
        LocalDate hoje = LocalDate.now(FUSO);
        Object[][] base = {
            // tipo, assunto, dias atrás, ciência (dias atrás ou null), urgência, status, motivo
            {"INTIMACAO", "Intimação para apresentação de documentos fiscais – EFD ICMS/IPI", 6, null, "CRITICA", "NOVA",
                "Intimação com efeito processual: a ciência tácita ocorre em poucos dias e abre prazo de resposta."},
            {"NOTIFICACAO", "Notificação de divergência entre EFD e documentos fiscais eletrônicos", 9, null, "ALTA", "EM_ANDAMENTO",
                "Notificação sem ciência; responder antes da ciência tácita evita autuação."},
            {"DOCUMENTO_ADMINISTRATIVO", "Débitos disponíveis para negociação no Domicílio Eletrônico do Contribuinte (DEC)", 8, null, "BAIXA", "NOVA",
                "Informativo de débitos; sem prazo de resposta, mas a ciência tácita se aproxima."},
            {"ALERTA", "Alerta de Débitos em Aberto – EFD", 14, 12, "MEDIA", "RESOLVIDA",
                "Alerta já com ciência registrada."},
            {"AVISO", "Aviso de omissão de entrega da DIEF", 20, 18, "ALTA", "RESOLVIDA",
                "Aviso respondido pelo escritório."},
            {"COMUNICADO", "Comunicado: alteração no calendário de obrigações acessórias", 25, 22, "SEM_RISCO", "ARQUIVADA",
                "Comunicado informativo, sem efeito processual."},
            {"NOTIFICACAO", "Notificação para regularização de inscrição estadual", 3, null, "MEDIA", "NOVA",
                "Notificação recente; ciência tácita em 10 dias corridos."},
            {"INFORMATIVO", "Informativo sobre o novo leiaute da NF-e", 40, 35, "SEM_RISCO", "ARQUIVADA",
                "Informativo geral."}
        };
        int criadas = 0;
        LocalDateTime agora = LocalDateTime.now(FUSO);
        for (int i = 0; i < base.length; i++) {
            Object[] b = base[i];
            Empresa e = empresas.get(i % empresas.size());
            ComunicacaoDec c = new ComunicacaoDec();
            c.setIdExterno("demo-" + (i + 1));
            c.setEmpresa(e);
            c.setCnpj(e.getCnpj() != null ? digitos(e.getCnpj().getNumero()) : "");
            c.setRazaoSocial(e.getRazaoSocial());
            c.setNumero(String.valueOf(360000 + i * 137));
            c.setTipo((String) b[0]);
            c.setAssunto((String) b[1]);
            LocalDate disponibilizada = hoje.minusDays((Integer) b[2]);
            c.setDisponibilizadaEm(disponibilizada);
            c.setCienteEm(b[3] == null ? null : hoje.minusDays((Integer) b[3]));
            c.setCienciaTacitaEm(disponibilizada.plusDays(10));
            c.setPrazoRespostaEm("INTIMACAO".equals(b[0]) || "NOTIFICACAO".equals(b[0]) ? disponibilizada.plusDays(40) : null);
            c.setUrgencia((String) b[4]);
            c.setStatus((String) b[5]);
            c.setMotivo((String) b[6]);
            c.setEncerrada("ARQUIVADA".equals(b[5]));
            c.setTemInteiroTeor(i % 2 == 0);
            c.setRemetente("SEFAZ-TO – Secretaria da Fazenda do Tocantins");
            c.setCorpo("Prezado(a) contribuinte " + e.getRazaoSocial() + ",\n\n"
                    + "Esta é uma comunicação de DEMONSTRAÇÃO, com conteúdo fictício, gerada para apresentar o sistema.\n"
                    + "Assunto: " + b[1] + ".\n\n"
                    + "Em um ambiente real, o texto integral da SEFAZ-TO aparece aqui, sincronizado do DEC Monitor.");
            c.setColetadaEm(disponibilizada.atTime(13, 0));
            c.setLink(null);
            c.setSincronizadaEm(agora);
            repository.persist(c);
            criadas++;
        }
        return criadas;
    }

    // ------------------------------------------------------------ leitura

    public List<ComunicacaoDecResponseDTO> listar() {
        return repository.listarTodas().stream().map(this::toDTO).toList();
    }

    public List<ComunicacaoDecResponseDTO> listarPorEmpresa(Long idEmpresa) {
        return repository.listarPorEmpresa(idEmpresa).stream().map(this::toDTO).toList();
    }

    public ComunicacaoDecResponseDTO toDTO(ComunicacaoDec c) {
        Long diasRestantes = c.getCienciaTacitaEm() == null ? null
                : ChronoUnit.DAYS.between(LocalDate.now(FUSO), c.getCienciaTacitaEm());
        return new ComunicacaoDecResponseDTO(
                c.getId(), c.getIdExterno(),
                c.getEmpresa() != null ? c.getEmpresa().getId() : null,
                c.getEmpresa() != null ? c.getEmpresa().getNomeFantasia() : null,
                c.getCnpj(), c.getRazaoSocial(), c.getNumero(), c.getTipo(), c.getAssunto(), c.getCorpo(),
                c.getRemetente(), c.getDisponibilizadaEm(), c.getCienteEm(), c.getPrazoCienciaEm(),
                c.getDiasParaResposta(), c.getColetadaEm(), c.getStatus(), c.getUrgencia(), c.getMotivo(),
                diasRestantes, c.getCienciaTacitaEm(), c.getPrazoRespostaEm(), c.getEncerrada(),
                c.getTemInteiroTeor(), c.getLink(), c.getSincronizadaEm());
    }

    // ------------------------------------------------------------ util

    private static String digitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String texto(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }

    private static String corta(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static LocalDate data(JsonNode n, String campo) {
        String v = texto(n, campo);
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime instante(JsonNode n, String campo) {
        String v = texto(n, campo);
        if (v == null || v.isBlank()) return null;
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(FUSO).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
