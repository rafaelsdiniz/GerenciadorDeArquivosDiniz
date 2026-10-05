package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.dto.request.FechamentoUpdateRequestDTO;
import diniz.contabilidade.arquivos.dto.response.CompetenciaDTO;
import diniz.contabilidade.arquivos.dto.response.FechamentoMensalResponseDTO;
import diniz.contabilidade.arquivos.dto.response.FechamentoMensalResponseDTO.Indicadores;
import diniz.contabilidade.arquivos.dto.response.FechamentoResumoDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoPendenteResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.FechamentoMensal;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.EtapaFechamento;
import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.SituacaoCadastral;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.FechamentoMensalRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Quadro do fechamento mensal: uma linha por empresa e competência, criada automaticamente
 * na primeira consulta da competência, com indicadores calculados a partir das obrigações.
 *
 * Competência segue o resto do sistema: obrigações que vencem em outubro/2026 pertencem
 * à competência 09/2026. A competência padrão é o mês anterior ao atual.
 */
@ApplicationScoped
public class FechamentoMensalService {

    private static final Logger LOG = Logger.getLogger(FechamentoMensalService.class);

    private static final Pattern FORMATO = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{4})$");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    /** Dia do mês seguinte à competência usado como prazo interno padrão. */
    private static final int DIA_PRAZO = 20;
    /** Quantos meses para trás a tela oferece (e cria linhas automaticamente). */
    private static final int MESES_HISTORICO = 12;
    private static final Set<SituacaoCadastral> INATIVAS = Set.of(SituacaoCadastral.BAIXADA, SituacaoCadastral.NULA);

    @Inject
    FechamentoMensalRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    ObrigacaoPendenteService obrigacaoService;

    // ------------------------------------------------------------------ consultas

    public List<FechamentoMensalResponseDTO> listar(String competencia) {
        YearMonth ym = parse(competencia);
        String comp = formatar(ym);
        garantirLinhas(ym);

        Map<Long, List<ObrigacaoPendenteResponseDTO>> obrigacoes = obrigacaoService.listar().stream()
                .filter(o -> comp.equals(o.competencia()) && o.idEmpresa() != null)
                .collect(Collectors.groupingBy(ObrigacaoPendenteResponseDTO::idEmpresa));

        return repository.buscarPorCompetencia(comp).stream()
                .map(f -> toDTO(f, indicadores(obrigacoes.getOrDefault(f.getEmpresa().getId(), List.of()))))
                .toList();
    }

    public FechamentoResumoDTO resumo(String competencia) {
        YearMonth ym = parse(competencia);
        garantirLinhas(ym);
        List<FechamentoMensal> linhas = repository.buscarPorCompetencia(formatar(ym));

        Map<EtapaFechamento, Long> porEtapa = new EnumMap<>(EtapaFechamento.class);
        for (EtapaFechamento e : EtapaFechamento.values()) porEtapa.put(e, 0L);
        LocalDate hoje = LocalDate.now();
        long atrasados = 0, semResponsavel = 0;
        for (FechamentoMensal f : linhas) {
            porEtapa.merge(f.getEtapa(), 1L, Long::sum);
            if (f.getEtapa() != EtapaFechamento.CONCLUIDO) {
                if (f.getPrazo() != null && f.getPrazo().isBefore(hoje)) atrasados++;
                if (f.getResponsavel() == null) semResponsavel++;
            }
        }
        long total = linhas.size();
        int pct = total == 0 ? 0 : (int) Math.round(porEtapa.get(EtapaFechamento.CONCLUIDO) * 100.0 / total);
        return new FechamentoResumoDTO(formatar(ym), total, porEtapa, pct, atrasados, semResponsavel);
    }

    /** Últimos 12 meses + a competência seguinte (mês atual), mais as que já existem no banco. Mais recente primeiro. */
    public List<CompetenciaDTO> competencias() {
        YearMonth atual = YearMonth.now();
        YearMonth padrao = competenciaPadrao();
        TreeSet<YearMonth> meses = new TreeSet<>();
        for (int i = 0; i <= MESES_HISTORICO; i++) meses.add(atual.minusMonths(i));
        for (String c : repository.competenciasExistentes()) {
            try {
                meses.add(parse(c));
            } catch (IllegalArgumentException ignorada) {
                // valor fora do padrão no banco: não oferece na lista
            }
        }
        List<CompetenciaDTO> lista = new ArrayList<>();
        for (YearMonth ym : meses.descendingSet()) {
            lista.add(new CompetenciaDTO(formatar(ym), rotulo(ym), ym.equals(padrao)));
        }
        return lista;
    }

    // ------------------------------------------------------------------ alteração

    @Transactional
    public FechamentoMensalResponseDTO atualizar(Long id, FechamentoUpdateRequestDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Informe ao menos um campo para alterar.");
        FechamentoMensal f = repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Fechamento não encontrado."));

        if (dto.etapa() != null && dto.etapa() != f.getEtapa()) {
            f.setEtapa(dto.etapa());
            f.setConcluidoEm(dto.etapa() == EtapaFechamento.CONCLUIDO ? LocalDateTime.now() : null);
        }

        if (Boolean.TRUE.equals(dto.removerResponsavel())) {
            f.setResponsavel(null);
        } else if (dto.idResponsavel() != null) {
            Usuario u = usuarioRepository.findByIdOptional(dto.idResponsavel())
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            if (u.getPerfilUsuario() != PerfilUsuario.ADMIN) {
                throw new IllegalArgumentException("O responsável precisa ser alguém do escritório (perfil administrador).");
            }
            f.setResponsavel(u);
        }

        if (dto.observacao() != null) {
            String obs = dto.observacao().strip();
            if (obs.length() > 1000) {
                throw new IllegalArgumentException("A observação pode ter no máximo 1000 caracteres.");
            }
            f.setObservacao(obs.isEmpty() ? null : obs);
        }

        repository.flush();
        String comp = f.getCompetencia();
        Long idEmpresa = f.getEmpresa().getId();
        List<ObrigacaoPendenteResponseDTO> obrigacoes = obrigacaoService.listar().stream()
                .filter(o -> comp.equals(o.competencia()) && idEmpresa.equals(o.idEmpresa()))
                .toList();
        return toDTO(f, indicadores(obrigacoes));
    }

    // ------------------------------------------------------------------ criação automática

    /**
     * Cria (em transação própria) as linhas que faltam para a competência: uma por empresa ativa,
     * na etapa "Aguardando documentos", herdando o responsável do mês anterior.
     * Só cria para competências entre 12 meses atrás e o mês atual; fora disso apenas lista o que existe.
     */
    synchronized void garantirLinhas(YearMonth ym) {
        YearMonth atual = YearMonth.now();
        if (ym.isAfter(atual) || ym.isBefore(atual.minusMonths(MESES_HISTORICO))) return;
        String comp = formatar(ym);
        try {
            QuarkusTransaction.requiringNew().run(() -> criarFaltantes(ym, comp));
        } catch (RuntimeException ex) {
            // outra instância criou as mesmas linhas ao mesmo tempo (chave única): segue com o que existe
            LOG.warnf("Não foi possível criar fechamentos de %s: %s", comp, ex.getMessage());
        }
    }

    private void criarFaltantes(YearMonth ym, String comp) {
        Set<Long> existentes = new HashSet<>();
        for (FechamentoMensal f : repository.buscarPorCompetencia(comp)) existentes.add(f.getEmpresa().getId());

        Map<Long, Usuario> responsavelAnterior = new HashMap<>();
        for (FechamentoMensal f : repository.buscarPorCompetencia(formatar(ym.minusMonths(1)))) {
            if (f.getResponsavel() != null) responsavelAnterior.put(f.getEmpresa().getId(), f.getResponsavel());
        }

        for (Empresa e : empresaRepository.listAll()) {
            // Set.of(...).contains(null) lança NPE: empresa sem situação informada conta como ativa
            boolean inativa = e.getSituacaoCadastral() != null && INATIVAS.contains(e.getSituacaoCadastral());
            if (existentes.contains(e.getId()) || inativa) continue;
            FechamentoMensal novo = new FechamentoMensal();
            novo.setEmpresa(e);
            novo.setCompetencia(comp);
            novo.setEtapa(EtapaFechamento.AGUARDANDO_DOCUMENTOS);
            novo.setPrazo(prazoPadrao(ym));
            novo.setResponsavel(responsavelAnterior.get(e.getId()));
            repository.persist(novo);
        }
    }

    // ------------------------------------------------------------------ utilitários

    public static YearMonth competenciaPadrao() {
        return YearMonth.now().minusMonths(1);
    }

    public static LocalDate prazoPadrao(YearMonth competencia) {
        return competencia.plusMonths(1).atDay(DIA_PRAZO);
    }

    /** "MM/aaaa" → YearMonth; vazio = competência padrão. */
    public static YearMonth parse(String competencia) {
        if (competencia == null || competencia.isBlank()) return competenciaPadrao();
        Matcher m = FORMATO.matcher(competencia.strip());
        if (!m.matches()) {
            throw new IllegalArgumentException("Competência inválida. Use o formato MM/aaaa (ex.: 09/2026).");
        }
        int ano = Integer.parseInt(m.group(2));
        if (ano < 2000 || ano > 2100) throw new IllegalArgumentException("Ano da competência fora do intervalo aceito.");
        return YearMonth.of(ano, Integer.parseInt(m.group(1)));
    }

    public static String formatar(YearMonth ym) {
        return String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
    }

    private static String rotulo(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " de " + ym.getYear();
    }

    private Indicadores indicadores(List<ObrigacaoPendenteResponseDTO> lista) {
        int docTotal = 0, docPend = 0, escTotal = 0, escEntregues = 0, escPend = 0, vencidas = 0;
        int aguardando = 0, atrasadas = 0, pagas = 0;
        for (ObrigacaoPendenteResponseDTO o : lista) {
            boolean entregue = o.status() == StatusObrigacao.ENTREGUE;
            boolean venceu = !entregue && (o.status() == StatusObrigacao.VENCIDA
                    || (o.diasParaVencer() != null && o.diasParaVencer() < 0));
            if (venceu) vencidas++;
            if (o.responsavel() == ResponsavelObrigacao.CLIENTE) {
                docTotal++;
                if (!entregue) docPend++;
            } else {
                escTotal++;
                if (entregue) escEntregues++; else escPend++;
            }
            String pag = o.situacaoPagamento();
            if ("AGUARDANDO".equals(pag)) {
                aguardando++;
            } else if ("ATRASADO".equals(pag)) {
                aguardando++;
                atrasadas++;
            } else if ("PAGO".equals(pag)) {
                pagas++;
            }
        }
        return new Indicadores(docTotal, docPend, escTotal, escEntregues, escPend, vencidas, aguardando, atrasadas, pagas);
    }

    private FechamentoMensalResponseDTO toDTO(FechamentoMensal f, Indicadores ind) {
        Empresa e = f.getEmpresa();
        Usuario r = f.getResponsavel();
        Long dias = f.getPrazo() != null ? ChronoUnit.DAYS.between(LocalDate.now(), f.getPrazo()) : null;
        boolean atrasado = f.getEtapa() != EtapaFechamento.CONCLUIDO && dias != null && dias < 0;
        return new FechamentoMensalResponseDTO(
                f.getId(),
                e.getId(),
                e.getNomeFantasia(),
                e.getCnpj() != null ? e.getCnpj().getNumero() : null,
                e.getRegimeTributario() != null ? e.getRegimeTributario().name() : null,
                f.getCompetencia(),
                f.getEtapa(),
                r != null ? r.getId() : null,
                r != null ? r.getNome() : null,
                f.getPrazo(),
                dias,
                atrasado,
                f.getObservacao(),
                f.getConcluidoEm(),
                f.getDataAtualizacao() != null ? f.getDataAtualizacao() : f.getDataCriacao(),
                ind);
    }
}
