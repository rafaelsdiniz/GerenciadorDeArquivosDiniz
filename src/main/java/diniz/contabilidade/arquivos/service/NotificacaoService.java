package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificacaoService {

    private static final Logger LOG = Logger.getLogger(NotificacaoService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    Mailer mailer;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoPendenteRepository;

    @ConfigProperty(name = "notificacao.dias-aviso", defaultValue = "30,15,7,3,1")
    String diasAvisoConfig;

    @Transactional
    public int dispararAvisos() {
        int[] dias = parseDias();
        int total = 0;

        for (Empresa empresa : empresaRepository.listAll()) {
            Map<Integer, List<Arquivo>> arquivosPorJanela = new HashMap<>();
            Map<Integer, List<ObrigacaoPendente>> obrigacoesPorJanela = new HashMap<>();
            boolean temConteudo = false;

            for (int d : dias) {
                List<Arquivo> arquivos = arquivoRepository.buscarVencendoEmPorEmpresa(empresa, d)
                        .stream()
                        .filter(a -> a.getDataVencimento() != null
                                && ChronoUnit.DAYS.between(LocalDate.now(), a.getDataVencimento()) == d)
                        .toList();

                List<ObrigacaoPendente> obrigacoes = obrigacaoPendenteRepository.buscarVencendoEm(d)
                        .stream()
                        .filter(p -> p.getEmpresa().getId().equals(empresa.getId())
                                && p.getDataVencimento() != null
                                && ChronoUnit.DAYS.between(LocalDate.now(), p.getDataVencimento()) == d)
                        .toList();

                if (!arquivos.isEmpty() || !obrigacoes.isEmpty()) {
                    arquivosPorJanela.put(d, arquivos);
                    obrigacoesPorJanela.put(d, obrigacoes);
                    temConteudo = true;
                }
            }

            if (temConteudo) {
                enviarEmailEmpresa(empresa, arquivosPorJanela, obrigacoesPorJanela);
                total++;
            }
        }
        return total;
    }

    private void enviarEmailEmpresa(
            Empresa empresa,
            Map<Integer, List<Arquivo>> arquivosPorJanela,
            Map<Integer, List<ObrigacaoPendente>> obrigacoesPorJanela
    ) {
        String destino = (empresa.getEmail() != null) ? empresa.getEmail().getEndereco() : null;
        if (destino == null || destino.isBlank()) {
            LOG.warnf("Empresa %d sem email cadastrado, pulando notificação.", empresa.getId());
            return;
        }

        String assunto = "[Diniz Contabilidade] Documentos e obrigações com prazo próximo";
        String html = montarHtml(empresa, arquivosPorJanela, obrigacoesPorJanela);

        try {
            mailer.send(Mail.withHtml(destino, assunto, html));
            LOG.infof("Notificação enviada para empresa %d (%s).", empresa.getId(), destino);
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao enviar notificação para empresa %d", empresa.getId());
        }
    }

    private String montarHtml(
            Empresa empresa,
            Map<Integer, List<Arquivo>> arquivosPorJanela,
            Map<Integer, List<ObrigacaoPendente>> obrigacoesPorJanela
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family: Arial, sans-serif;\">");
        sb.append("<h2>Olá, ").append(escape(empresa.getNomeFantasia())).append("</h2>");
        sb.append("<p>Você tem documentos e/ou obrigações com prazo próximo:</p>");

        arquivosPorJanela.keySet().stream().sorted().forEach(dias -> {
            List<Arquivo> arquivos = arquivosPorJanela.get(dias);
            List<ObrigacaoPendente> obrigacoes = obrigacoesPorJanela.get(dias);

            sb.append("<h3>Vencendo em ").append(dias).append(" dia(s)</h3>");
            sb.append("<ul>");

            for (Arquivo a : arquivos) {
                sb.append("<li><b>")
                        .append(escape(a.getNomeOriginal()))
                        .append("</b> — vence em ")
                        .append(a.getDataVencimento().format(FMT));
                if (a.getDescricao() != null && !a.getDescricao().isBlank()) {
                    sb.append(" — ").append(escape(a.getDescricao()));
                }
                sb.append("</li>");
            }

            for (ObrigacaoPendente p : obrigacoes) {
                String nome = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getNome() : "Obrigação";
                sb.append("<li><b>[Obrigação] ")
                        .append(escape(nome))
                        .append("</b> — vence em ")
                        .append(p.getDataVencimento().format(FMT))
                        .append(" — ainda não entregue.</li>");
            }
            sb.append("</ul>");
        });

        sb.append("<p style=\"color:#888;font-size:12px;\">Mensagem automática — Diniz Contabilidade.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private int[] parseDias() {
        String[] partes = diasAvisoConfig.split(",");
        int[] dias = new int[partes.length];
        for (int i = 0; i < partes.length; i++) {
            dias[i] = Integer.parseInt(partes[i].trim());
        }
        return dias;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
