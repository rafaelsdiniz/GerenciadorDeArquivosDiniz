package diniz.contabilidade.arquivos.service;

import java.util.List;

import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SchedulerService {

    private static final Logger LOG = Logger.getLogger(SchedulerService.class);

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    ObrigacaoPendenteService obrigacaoPendenteService;

    @Inject
    NotificacaoService notificacaoService;

    @Scheduled(cron = "0 0 7 * * ?", identity = "job-diario")
    void jobDiario() {
        LOG.info("[scheduler] iniciando job diário");

        int arquivosVencidos = marcarArquivosVencidos();
        LOG.infof("[scheduler] arquivos marcados como VENCIDO: %d", arquivosVencidos);

        int obrigacoesVencidas = obrigacaoPendenteService.marcarVencidas();
        LOG.infof("[scheduler] obrigações marcadas como VENCIDA: %d", obrigacoesVencidas);

        int avisosEnviados = notificacaoService.dispararAvisos();
        LOG.infof("[scheduler] empresas notificadas: %d", avisosEnviados);
    }

    @Scheduled(cron = "0 0 6 1 * ?", identity = "job-mensal-obrigacoes")
    void jobMensalGerarObrigacoes() {
        LOG.info("[scheduler] gerando obrigações pendentes do mês");
        int criadas = obrigacaoPendenteService.gerarPendentesParaTodasRecorrentes();
        LOG.infof("[scheduler] obrigações pendentes criadas: %d", criadas);
    }

    @Transactional
    public int marcarArquivosVencidos() {
        List<Arquivo> arquivos = arquivoRepository.buscarParaMarcarVencidos();
        for (Arquivo a : arquivos) {
            a.setStatus(StatusArquivo.VENCIDO);
        }
        return arquivos.size();
    }
}
