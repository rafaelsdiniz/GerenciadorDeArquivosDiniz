package diniz.contabilidade.arquivos.service;

import java.util.List;

import diniz.contabilidade.arquivos.dto.request.PastaRequestDTO;
import diniz.contabilidade.arquivos.dto.response.PastaResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.PastaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PastaService {

    @Inject
    PastaRepository pastaRepository;

    @Inject
    EmpresaRepository empresaRepository;

    public List<PastaResponseDTO> listar() {
        return pastaRepository.listAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PastaResponseDTO buscarPorId(Long id) {
        Pasta pasta = pastaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        return toResponseDTO(pasta);
    }

    public List<PastaResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        return pastaRepository.buscarPorEmpresa(empresa)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<PastaResponseDTO> buscarPastasRaiz(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        return pastaRepository.buscarPastasRaiz(empresa)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<PastaResponseDTO> buscarSubpastas(Long idPastaPai) {
        Pasta pastaPai = pastaRepository.findByIdOptional(idPastaPai)
                .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada."));

        return pastaRepository.buscarSubpastas(pastaPai)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PastaResponseDTO salvar(PastaRequestDTO dto) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        Pasta pasta = new Pasta();
        pasta.setNome(dto.nome());
        pasta.setDescricao(dto.descricao());
        pasta.setEmpresa(empresa);

        if (dto.idPastaPai() != null) {
            Pasta pastaPai = pastaRepository.findByIdOptional(dto.idPastaPai())
                    .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada."));

            if (!pastaPai.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("A pasta pai não pertence à empresa informada.");
            }

            pasta.setPastaPai(pastaPai);
        }

        pastaRepository.persist(pasta);

        return toResponseDTO(pasta);
    }

    @Transactional
    public PastaResponseDTO atualizar(Long id, PastaRequestDTO dto) {
        Pasta pasta = pastaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        pasta.setNome(dto.nome());
        pasta.setDescricao(dto.descricao());
        pasta.setEmpresa(empresa);

        if (dto.idPastaPai() != null) {
            Pasta pastaPai = pastaRepository.findByIdOptional(dto.idPastaPai())
                    .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada."));

            if (pastaPai.getId().equals(pasta.getId())) {
                throw new IllegalArgumentException("Uma pasta não pode ser pai dela mesma.");
            }

            if (!pastaPai.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("A pasta pai não pertence à empresa informada.");
            }

            pasta.setPastaPai(pastaPai);
        } else {
            pasta.setPastaPai(null);
        }

        return toResponseDTO(pasta);
    }

    @Transactional
    public void deletar(Long id) {
        Pasta pasta = pastaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        pastaRepository.delete(pasta);
    }

    private PastaResponseDTO toResponseDTO(Pasta pasta) {
        return new PastaResponseDTO(
                pasta.getId(),
                pasta.getNome(),
                pasta.getDescricao(),
                pasta.getEmpresa().getId(),
                pasta.getPastaPai() != null ? pasta.getPastaPai().getId() : null
        );
    }
}