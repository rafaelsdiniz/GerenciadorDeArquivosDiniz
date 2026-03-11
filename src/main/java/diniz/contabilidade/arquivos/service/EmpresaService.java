package diniz.contabilidade.arquivos.service;

import java.util.List;

import diniz.contabilidade.arquivos.dto.request.EmpresaRequestDTO;
import diniz.contabilidade.arquivos.dto.response.EmpresaResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.valueObject.Cnpj;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import diniz.contabilidade.arquivos.model.valueObject.Telefone;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class EmpresaService {

    @Inject
    EmpresaRepository empresaRepository;

    public List<EmpresaResponseDTO> listar() {
        return empresaRepository.listAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public EmpresaResponseDTO buscarPorId(Long id) {
        Empresa empresa = empresaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada"));

        return toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO salvar(EmpresaRequestDTO dto) {

        Empresa empresa = new Empresa();

        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setCnpj(new Cnpj(dto.cnpj()));
        empresa.setTelefone(new Telefone(dto.telefone()));
        empresa.setEmail(new Email(dto.email()));

        empresaRepository.persist(empresa);

        return toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO atualizar(Long id, EmpresaRequestDTO dto) {

        Empresa empresa = empresaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada"));

        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setCnpj(new Cnpj(dto.cnpj()));
        empresa.setTelefone(new Telefone(dto.telefone()));
        empresa.setEmail(new Email(dto.email()));

        return toResponseDTO(empresa);
    }

    @Transactional
    public void deletar(Long id) {

        Empresa empresa = empresaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada"));

        empresaRepository.delete(empresa);
    }

    private EmpresaResponseDTO toResponseDTO(Empresa empresa) {

        return new EmpresaResponseDTO(
                empresa.getId(),
                empresa.getNomeFantasia(),
                empresa.getRazaoSocial(),
                empresa.getCnpj().getNumero(),
                empresa.getTelefone().getNumero(),
                empresa.getEmail().getEndereco()
        );
    }
}