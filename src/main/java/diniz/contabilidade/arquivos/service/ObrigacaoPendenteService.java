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
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
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

    public List<ObrigacaoPendenteResponseDTO> listar() {
        return repository.listAll().stream().map(this::toResponseDTO).toList();
    }

    public ObrigacaoPendenteResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidade(id));
    }

    public List<ObrigacaoPendenteResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return repository.buscarPorEmpresa(empresa).stream().map(this::toResponseDTO).toList();
    }

    public List<ObrigacaoPendenteResponseDTO> buscarPendentesPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return repository.buscarPendentesPorEmpresa(empresa).stream().map(this::toResponseDTO).toList();
    }

    public List<ObrigacaoPendenteResponseDTO> buscarVencidas() {
        return repository.buscarVencidas().stream().map(this::toResponseDTO).toList();
    }

    public List<ObrigacaoPendenteResponseDTO> buscarVencendoEm(int dias) {
        return repository.buscarVencendoEm(dias).stream().map(this::toResponseDTO).toList();
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

    private ObrigacaoPendenteResponseDTO toResponseDTO(ObrigacaoPendente p) {
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
                diasParaVencer
        );
    }
}
