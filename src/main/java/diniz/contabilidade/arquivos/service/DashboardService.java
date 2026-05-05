package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import diniz.contabilidade.arquivos.dto.response.DashboardResponseDTO;
import diniz.contabilidade.arquivos.dto.response.DashboardResponseDTO.MaiorAtraso;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DashboardService {

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoPendenteRepository;

    public DashboardResponseDTO porEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        List<Arquivo> arquivos = arquivoRepository.buscarPorEmpresa(empresa);
        List<ObrigacaoPendente> obrigacoes = obrigacaoPendenteRepository.buscarPorEmpresa(empresa);

        return montar(empresa, arquivos, obrigacoes);
    }

    private DashboardResponseDTO montar(Empresa empresa, List<Arquivo> arquivos, List<ObrigacaoPendente> obrigacoes) {
        LocalDate hoje = LocalDate.now();

        long total = arquivos.size();

        long vencidos = arquivos.stream()
                .filter(a -> a.getStatus() == StatusArquivo.VENCIDO)
                .count();

        long vencendo7 = arquivos.stream()
                .filter(a -> emJanela(a, hoje, 7))
                .count();

        long vencendo30 = arquivos.stream()
                .filter(a -> emJanela(a, hoje, 30))
                .count();

        Double taxaEntrega = calcularTaxaEntregaNoPrazo(obrigacoes);

        long obrigacoesPendentes = obrigacoes.stream()
                .filter(o -> o.getStatus() == StatusObrigacao.PENDENTE)
                .count();

        long obrigacoesVencidas = obrigacoes.stream()
                .filter(o -> o.getStatus() == StatusObrigacao.VENCIDA)
                .count();

        Map<StatusArquivo, Long> porStatus = new EnumMap<>(StatusArquivo.class);
        for (StatusArquivo s : StatusArquivo.values()) porStatus.put(s, 0L);
        for (Arquivo a : arquivos) {
            if (a.getStatus() != null) porStatus.merge(a.getStatus(), 1L, Long::sum);
        }

        Map<CategoriaFiscal, Long> porCategoria = new EnumMap<>(CategoriaFiscal.class);
        for (Arquivo a : arquivos) {
            if (a.getCategoriaFiscal() != null) porCategoria.merge(a.getCategoriaFiscal(), 1L, Long::sum);
        }

        List<MaiorAtraso> top5 = arquivos.stream()
                .filter(a -> a.getStatus() == StatusArquivo.VENCIDO && a.getDataVencimento() != null)
                .sorted(Comparator.comparing(Arquivo::getDataVencimento))
                .limit(5)
                .map(a -> new MaiorAtraso(
                        a.getId(),
                        a.getNomeOriginal(),
                        a.getDescricao(),
                        ChronoUnit.DAYS.between(a.getDataVencimento(), hoje)
                ))
                .toList();

        return new DashboardResponseDTO(
                empresa.getId(),
                empresa.getNomeFantasia(),
                total,
                vencidos,
                vencendo7,
                vencendo30,
                taxaEntrega,
                obrigacoesPendentes,
                obrigacoesVencidas,
                porStatus,
                porCategoria,
                top5
        );
    }

    private boolean emJanela(Arquivo a, LocalDate hoje, int dias) {
        if (a.getDataVencimento() == null) return false;
        if (a.getStatus() == StatusArquivo.ARQUIVADO) return false;
        long diff = ChronoUnit.DAYS.between(hoje, a.getDataVencimento());
        return diff >= 0 && diff <= dias;
    }

    private Double calcularTaxaEntregaNoPrazo(List<ObrigacaoPendente> obrigacoes) {
        LocalDate corte = LocalDate.now().minusYears(1);
        List<ObrigacaoPendente> recentes = obrigacoes.stream()
                .filter(o -> o.getDataVencimento() != null && o.getDataVencimento().isAfter(corte))
                .filter(o -> o.getStatus() != StatusObrigacao.PENDENTE)
                .toList();

        if (recentes.isEmpty()) return null;

        long noPrazo = recentes.stream()
                .filter(o -> o.getStatus() == StatusObrigacao.ENTREGUE
                        && o.getDataEntrega() != null
                        && !o.getDataEntrega().isAfter(o.getDataVencimento()))
                .count();

        return (noPrazo * 100.0) / recentes.size();
    }
}
