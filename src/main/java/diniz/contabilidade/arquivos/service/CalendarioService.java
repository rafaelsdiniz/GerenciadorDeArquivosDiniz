package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import diniz.contabilidade.arquivos.dto.response.EventoCalendarioDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CalendarioService {

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoPendenteRepository;

    public List<EventoCalendarioDTO> eventosDoMes(Long idEmpresa, int ano, int mes) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<EventoCalendarioDTO> eventos = new ArrayList<>();

        for (Arquivo a : arquivoRepository.buscarComVencimentoEntre(empresa, inicio, fim)) {
            eventos.add(new EventoCalendarioDTO(
                    a.getDataVencimento(),
                    "ARQUIVO",
                    a.getId(),
                    a.getNomeOriginal(),
                    a.getDescricao(),
                    a.getStatus() != null ? a.getStatus().name() : null,
                    a.getCategoriaFiscal() != null ? a.getCategoriaFiscal().name() : null
            ));
        }

        for (ObrigacaoPendente p : obrigacaoPendenteRepository.buscarPorEmpresa(empresa)) {
            if (p.getDataVencimento() == null) continue;
            if (p.getDataVencimento().isBefore(inicio) || p.getDataVencimento().isAfter(fim)) continue;

            String titulo = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getNome() : "Obrigação";
            String descricao = p.getObrigacaoRecorrente() != null ? p.getObrigacaoRecorrente().getDescricao() : null;

            eventos.add(new EventoCalendarioDTO(
                    p.getDataVencimento(),
                    "OBRIGACAO",
                    p.getId(),
                    titulo,
                    descricao,
                    p.getStatus() != null ? p.getStatus().name() : null,
                    null
            ));
        }

        eventos.sort(Comparator.comparing(EventoCalendarioDTO::data));
        return eventos;
    }
}
