package diniz.contabilidade.arquivos.service;

import java.util.List;

import diniz.contabilidade.arquivos.dto.request.SocioRequestDTO;
import diniz.contabilidade.arquivos.dto.response.SocioResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Socio;
import diniz.contabilidade.arquivos.model.valueObject.Cpf;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.SocioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class SocioService {

    @Inject
    SocioRepository socioRepository;

    @Inject
    EmpresaRepository empresaRepository;

    public List<SocioResponseDTO> listar() {
        return socioRepository.listAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public SocioResponseDTO buscarPorId(Long id) {
        Socio socio = socioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Sócio não encontrado."));
        return toResponseDTO(socio);
    }

    public List<SocioResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return socioRepository.buscarPorEmpresa(empresa)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public SocioResponseDTO salvar(SocioRequestDTO dto) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        Socio socio = new Socio();
        socio.setNome(dto.nome());
        socio.setCpf(new Cpf(dto.cpf()));
        socio.setEmpresa(empresa);
        socio.setParticipacao(dto.participacao());
        socio.setAdministrador(dto.administrador());

        socioRepository.persist(socio);
        return toResponseDTO(socio);
    }

    @Transactional
    public SocioResponseDTO atualizar(Long id, SocioRequestDTO dto) {
        Socio socio = socioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Sócio não encontrado."));

        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        socio.setNome(dto.nome());
        socio.setCpf(new Cpf(dto.cpf()));
        socio.setEmpresa(empresa);
        socio.setParticipacao(dto.participacao());
        socio.setAdministrador(dto.administrador());

        return toResponseDTO(socio);
    }

    @Transactional
    public void deletar(Long id) {
        Socio socio = socioRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Sócio não encontrado."));
        socioRepository.delete(socio);
    }

    private SocioResponseDTO toResponseDTO(Socio socio) {
        return new SocioResponseDTO(
                socio.getId(),
                socio.getNome(),
                socio.getCpf() != null ? socio.getCpf().getNumero() : null,
                socio.getEmpresa() != null ? socio.getEmpresa().getId() : null,
                socio.getParticipacao(),
                socio.getAdministrador()
        );
    }
}
