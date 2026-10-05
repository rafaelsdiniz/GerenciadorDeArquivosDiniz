package diniz.contabilidade.arquivos.service;

import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import java.util.List;

import diniz.contabilidade.arquivos.dto.request.ObrigacaoRecorrenteRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ObrigacaoRecorrenteResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ObrigacaoRecorrenteService {

    @Inject
    ObrigacaoRecorrenteRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ObrigacaoPendenteService pendenteService;

    public List<ObrigacaoRecorrenteResponseDTO> listar() {
        return repository.listAll().stream().map(this::toResponseDTO).toList();
    }

    public ObrigacaoRecorrenteResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidade(id));
    }

    public List<ObrigacaoRecorrenteResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return repository.buscarPorEmpresa(empresa).stream().map(this::toResponseDTO).toList();
    }

    @Transactional
    public ObrigacaoRecorrenteResponseDTO salvar(ObrigacaoRecorrenteRequestDTO dto) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        ObrigacaoRecorrente entidade = new ObrigacaoRecorrente();
        aplicar(dto, entidade, empresa);
        repository.persist(entidade);
        // já cria a próxima ocorrência, sem esperar o job do dia 1º
        if (Boolean.TRUE.equals(entidade.getAtivo())) {
            pendenteService.gerarProxima(entidade);
        }
        return toResponseDTO(entidade);
    }

    @Transactional
    public ObrigacaoRecorrenteResponseDTO atualizar(Long id, ObrigacaoRecorrenteRequestDTO dto) {
        ObrigacaoRecorrente entidade = buscarEntidade(id);
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        aplicar(dto, entidade, empresa);
        if (Boolean.TRUE.equals(entidade.getAtivo())) {
            pendenteService.gerarProxima(entidade);
        }
        return toResponseDTO(entidade);
    }

    @Transactional
    public void deletar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private ObrigacaoRecorrente buscarEntidade(Long id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Obrigação recorrente não encontrada."));
    }

    private void aplicar(ObrigacaoRecorrenteRequestDTO dto, ObrigacaoRecorrente entidade, Empresa empresa) {
        entidade.setEmpresa(empresa);
        entidade.setNome(dto.nome());
        entidade.setDescricao(dto.descricao());
        entidade.setPeriodicidade(dto.periodicidade());
        entidade.setDiaVencimento(dto.diaVencimento());
        entidade.setTipoArquivoEsperado(dto.tipoArquivoEsperado());
        entidade.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        entidade.setResponsavel(dto.responsavel() == null ? ResponsavelObrigacao.ESCRITORIO : dto.responsavel());
    }

    private ObrigacaoRecorrenteResponseDTO toResponseDTO(ObrigacaoRecorrente o) {
        return new ObrigacaoRecorrenteResponseDTO(
                o.getId(),
                o.getEmpresa().getId(),
                o.getNome(),
                o.getDescricao(),
                o.getPeriodicidade(),
                o.getDiaVencimento(),
                o.getTipoArquivoEsperado(),
                o.getAtivo(),
                o.getResponsavel()
        );
    }
}
