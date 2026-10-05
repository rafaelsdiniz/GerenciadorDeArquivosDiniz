package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ObrigacaoPendenteService {

    @Inject
    ObrigacaoPendenteRepository repository;

    @Inject
    ObrigacaoRecorrenteRepository recorrenteRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    public List<ObrigacaoPendenteResponseDTO> listar() {
        return comAnexos(repository.listAll());
    }

    public ObrigacaoPendenteResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidade(id));
    }

    public List<ObrigacaoPendenteResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return comAnexos(repository.buscarPorEmpresa(empresa));
    }

    public List<ObrigacaoPendenteResponseDTO> buscarPendentesPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return comAnexos(repository.buscarPendentesPorEmpresa(empresa));
    }

    public List<ObrigacaoPendenteResponseDTO> buscarVencidas() {
        return comAnexos(repository.buscarVencidas());
    }

    public List<ObrigacaoPendenteResponseDTO> buscarVencendoEm(int dias) {
        return comAnexos(repository.buscarVencendoEm(dias));
    }

    @Transactional
    public int gerarPendentesParaTodasRecorrentes() {
        List<ObrigacaoRecorrente> ativas = recorrenteRepository.buscarAtivas();
        int criadas = 0;
        for (ObrigacaoRecorrente rec : ativas) {
            if (gerarProximaPendente(rec)) {
                criadas++;
            }
        }
        return criadas;
    }

    @Transactional
    public ObrigacaoPendenteResponseDTO marcarComoEntregue(Long id) {
        ObrigacaoPendente p = buscarEntidade(id);
        p.setStatus(StatusObrigacao.ENTREGUE);
        p.setDataEntrega(LocalDate.now());
        return toResponseDTO(p);
    }

    /** Desfaz uma entrega marcada por engano: volta a PENDENTE (ou VENCIDA se o prazo já passou). */
    /** Confirma o pagamento da guia de uma obrigação do escritório já entregue. */
    @Transactional
    public ObrigacaoPendenteResponseDTO confirmarPagamento(Long id, LocalDate data) {
        ObrigacaoPendente p = buscarEntidade(id);
        if (p.getStatus() != StatusObrigacao.ENTREGUE) {
            throw new IllegalArgumentException("Só é possível confirmar o pagamento depois que a guia foi entregue.");
        }
        if (responsavel(p) != ResponsavelObrigacao.ESCRITORIO) {
            throw new IllegalArgumentException("Esta obrigação é de envio de documentos pelo cliente; não tem guia para pagar.");
        }
        if (!geraGuia(p)) {
            throw new IllegalArgumentException("Esta obrigação é uma declaração/entrega acessória; não tem guia para pagar.");
        }
        LocalDate pagamento = data != null ? data : LocalDate.now();
        if (pagamento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de pagamento não pode ser no futuro.");
        }
        p.setDataPagamento(pagamento);
        return toResponseDTO(p);
    }

    /** Desfaz a confirmação de pagamento (marcada por engano). */
    @Transactional
    public ObrigacaoPendenteResponseDTO desfazerPagamento(Long id) {
        ObrigacaoPendente p = buscarEntidade(id);
        p.setDataPagamento(null);
        return toResponseDTO(p);
    }

    private ResponsavelObrigacao responsavel(ObrigacaoPendente p) {
        return p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getResponsavel() : ResponsavelObrigacao.ESCRITORIO;
    }

    /** Declarações e entregas acessórias (balancete, DEFIS, DCTF, SPED...) não geram guia de pagamento. */
    private static final java.util.regex.Pattern SEM_GUIA = java.util.regex.Pattern.compile(
            "balancete|defis|dctf|declara|sped|efd|dirf|rais|livro|dief|caged|esocial",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private boolean geraGuia(ObrigacaoPendente p) {
        String nome = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getNome() : null;
        return nome == null || !SEM_GUIA.matcher(nome).find();
    }

    private String situacaoPagamento(ObrigacaoPendente p) {
        if (p.getStatus() != StatusObrigacao.ENTREGUE || responsavel(p) != ResponsavelObrigacao.ESCRITORIO || !geraGuia(p)) return "NAO_SE_APLICA";
        if (p.getDataPagamento() != null) return "PAGO";
        boolean venceu = p.getDataVencimento() != null && p.getDataVencimento().isBefore(LocalDate.now());
        return venceu ? "ATRASADO" : "AGUARDANDO";
    }

    @Transactional
    public ObrigacaoPendenteResponseDTO reabrir(Long id) {
        ObrigacaoPendente p = buscarEntidade(id);
        p.setDataEntrega(null);
        p.setDataPagamento(null);
        boolean atrasada = p.getDataVencimento() != null && p.getDataVencimento().isBefore(LocalDate.now());
        p.setStatus(atrasada ? StatusObrigacao.VENCIDA : StatusObrigacao.PENDENTE);
        return toResponseDTO(p);
    }

    /** Gera a próxima ocorrência de uma recorrente (idempotente). */
    @Transactional
    public boolean gerarProxima(ObrigacaoRecorrente rec) {
        return gerarProximaPendente(rec);
    }

    @Transactional
    public ObrigacaoPendenteResponseDTO atualizarVencimento(Long id, LocalDate dataVencimento) {
        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória.");
        }
        ObrigacaoPendente p = buscarEntidade(id);
        p.setDataVencimento(dataVencimento);
        if (p.getStatus() == StatusObrigacao.VENCIDA && dataVencimento.isAfter(LocalDate.now())) {
            p.setStatus(StatusObrigacao.PENDENTE);
        }
        return toResponseDTO(p);
    }

    @Transactional
    public int marcarVencidas() {
        List<ObrigacaoPendente> vencendo = repository.buscarParaMarcarVencidas();
        for (ObrigacaoPendente p : vencendo) {
            p.setStatus(StatusObrigacao.VENCIDA);
        }
        return vencendo.size();
    }

    private boolean gerarProximaPendente(ObrigacaoRecorrente rec) {
        LocalDate proximoVencimento = calcularProximoVencimento(rec);

        if (repository.buscarPorRecorrenteEData(rec, proximoVencimento).isPresent()) {
            return false;
        }

        ObrigacaoPendente pendente = new ObrigacaoPendente();
        pendente.setObrigacaoRecorrente(rec);
        pendente.setEmpresa(rec.getEmpresa());
        pendente.setDataVencimento(proximoVencimento);
        pendente.setStatus(StatusObrigacao.PENDENTE);
        repository.persist(pendente);
        return true;
    }

    private LocalDate calcularProximoVencimento(ObrigacaoRecorrente rec) {
        LocalDate hoje = LocalDate.now();
        YearMonth ym = YearMonth.from(hoje);

        LocalDate candidato = primeiroVencimentoDoPeriodo(ym, rec);

        while (!candidato.isAfter(hoje)) {
            ym = avancarPeriodo(ym, rec.getPeriodicidade());
            candidato = primeiroVencimentoDoPeriodo(ym, rec);
        }
        return candidato;
    }

    private LocalDate primeiroVencimentoDoPeriodo(YearMonth ym, ObrigacaoRecorrente rec) {
        int dia = Math.min(rec.getDiaVencimento(), ym.lengthOfMonth());
        return ym.atDay(dia);
    }

    private YearMonth avancarPeriodo(YearMonth ym, Periodicidade p) {
        return switch (p) {
            case MENSAL -> ym.plusMonths(1);
            case TRIMESTRAL -> ym.plusMonths(3);
            case ANUAL -> ym.plusYears(1);
        };
    }

    private ObrigacaoPendente buscarEntidade(Long id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Obrigação pendente não encontrada."));
    }

    /** Converte uma lista contando os anexos de todas numa única consulta. */
    private List<ObrigacaoPendenteResponseDTO> comAnexos(List<ObrigacaoPendente> lista) {
        if (lista.isEmpty()) return List.of();
        java.util.Map<Long, Long> anexos = new java.util.HashMap<>();
        List<Object[]> linhas = arquivoRepository.getEntityManager().createQuery(
                "select a.obrigacaoPendente.id, count(a) from Arquivo a "
                + "where a.obrigacaoPendente is not null and a.excluidoEm is null group by a.obrigacaoPendente.id",
                Object[].class).getResultList();
        for (Object[] l : linhas) anexos.put((Long) l[0], (Long) l[1]);
        // valor da guia: o do arquivo lido mais recente de cada obrigação (uma consulta para a lista toda)
        java.util.Map<Long, java.math.BigDecimal> valores = new java.util.HashMap<>();
        List<Object[]> guias = arquivoRepository.getEntityManager().createQuery(
                "select a.obrigacaoPendente.id, a.valor from Arquivo a "
                + "where a.obrigacaoPendente is not null and a.excluidoEm is null and a.valor is not null order by a.id",
                Object[].class).getResultList();
        for (Object[] g : guias) valores.put((Long) g[0], (java.math.BigDecimal) g[1]);
        return lista.stream().map(p -> toResponseDTO(p, anexos.getOrDefault(p.getId(), 0L), valores.get(p.getId()))).toList();
    }

    private ObrigacaoPendenteResponseDTO toResponseDTO(ObrigacaoPendente p) {
        return toResponseDTO(p, arquivoRepository.count("obrigacaoPendente = ?1 and excluidoEm is null", p));
    }

    private ObrigacaoPendenteResponseDTO toResponseDTO(ObrigacaoPendente p, long totalArquivos) {
        List<java.math.BigDecimal> v = arquivoRepository.getEntityManager().createQuery(
                "select a.valor from Arquivo a where a.obrigacaoPendente = :p and a.excluidoEm is null and a.valor is not null order by a.id desc",
                java.math.BigDecimal.class).setParameter("p", p).setMaxResults(1).getResultList();
        return toResponseDTO(p, totalArquivos, v.isEmpty() ? null : v.get(0));
    }

    private ObrigacaoPendenteResponseDTO toResponseDTO(ObrigacaoPendente p, long totalArquivos, java.math.BigDecimal valorGuia) {
        Long diasParaVencer = null;
        if (p.getDataVencimento() != null) {
            diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), p.getDataVencimento());
        }

        return new ObrigacaoPendenteResponseDTO(
                p.getId(),
                p.getEmpresa().getId(),
                p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getId() : null,
                p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getNome() : null,
                p.getDataVencimento(),
                p.getDataEntrega(),
                p.getStatus(),
                diasParaVencer,
                p.getEmpresa().getNomeFantasia(),
                competencia(p),
                p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getResponsavel() : ResponsavelObrigacao.ESCRITORIO,
                totalArquivos,
                p.getDataPagamento(),
                situacaoPagamento(p),
                valorGuia
        );
    }

    private String competencia(ObrigacaoPendente p) {
        if (p.getDataVencimento() == null) return null;
        Periodicidade per = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getPeriodicidade() : Periodicidade.MENSAL;
        if (per == Periodicidade.ANUAL) {
            return String.valueOf(p.getDataVencimento().getYear() - 1);
        }
        YearMonth ref = YearMonth.from(p.getDataVencimento()).minusMonths(1);
        return String.format("%02d/%d", ref.getMonthValue(), ref.getYear());
    }
}
