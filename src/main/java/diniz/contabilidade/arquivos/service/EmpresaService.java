package diniz.contabilidade.arquivos.service;

import diniz.contabilidade.arquivos.repository.ComunicacaoDecRepository;
import java.util.List;

import diniz.contabilidade.arquivos.dto.request.EmpresaRequestDTO;
import diniz.contabilidade.arquivos.dto.response.EmpresaResponseDTO;
import diniz.contabilidade.arquivos.dto.response.EnderecoDTO;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.valueObject.Cnpj;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import diniz.contabilidade.arquivos.model.valueObject.Endereco;
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

    @Inject
    ComunicacaoDecRepository comunicacaoDecRepository;

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
        aplicar(empresa, dto);
        empresaRepository.persist(empresa);
        comunicacaoDecRepository.vincularEmpresa(empresa);
        return toResponseDTO(empresa);
    }

    @Transactional
    public EmpresaResponseDTO atualizar(Long id, EmpresaRequestDTO dto) {
        Empresa empresa = empresaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada"));
        aplicar(empresa, dto);
        return toResponseDTO(empresa);
    }

    @Transactional
    public void deletar(Long id) {
        Empresa empresa = empresaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada"));

        // dados próprios da empresa impedem a exclusão: avisa com clareza em vez de erro 500
        var em = empresaRepository.getEntityManager();
        String[][] dependentes = {
            {"Arquivo", "arquivo(s)"}, {"Pasta", "pasta(s)"}, {"Usuario", "usuário(s)"},
            {"Socio", "sócio(s)"}, {"ObrigacaoRecorrente", "obrigação(ões) recorrente(s)"},
            {"ObrigacaoPendente", "pendência(s)"}
        };
        List<String> itens = new java.util.ArrayList<>();
        for (String[] d : dependentes) {
            Long n = em.createQuery("select count(x) from " + d[0] + " x where x.empresa = :e", Long.class)
                    .setParameter("e", empresa).getSingleResult();
            if (n > 0) itens.add(n + " " + d[1]);
        }
        if (!itens.isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir: a empresa ainda tem " + String.join(", ", itens)
                    + ". Remova ou transfira esses dados antes.");
        }

        // comunicações do DEC voltam a ficar como "CNPJ não cadastrado"
        comunicacaoDecRepository.update("empresa = null where empresa = ?1", empresa);
        empresaRepository.delete(empresa);
    }

    private void aplicar(Empresa empresa, EmpresaRequestDTO dto) {
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setCnpj(new Cnpj(dto.cnpj()));
        empresa.setTelefone(new Telefone(dto.telefone()));
        empresa.setEmail(new Email(dto.email()));

        empresa.setDataAbertura(dto.dataAbertura());
        empresa.setSituacaoCadastral(dto.situacaoCadastral());
        empresa.setNaturezaJuridica(dto.naturezaJuridica());
        empresa.setSite(dto.site());

        if (dto.endereco() != null) {
            EnderecoDTO e = dto.endereco();
            empresa.setEndereco(new Endereco(
                    e.logradouro(), e.numero(), e.complemento(),
                    e.bairro(), e.cidade(), e.uf(), e.cep()
            ));
        } else {
            empresa.setEndereco(null);
        }

        empresa.setInscricaoEstadual(dto.inscricaoEstadual());
        empresa.setInscricaoMunicipal(dto.inscricaoMunicipal());
        empresa.setRegimeTributario(dto.regimeTributario());
        empresa.setCnaePrincipal(dto.cnaePrincipal());
        empresa.setCnaesSecundarios(dto.cnaesSecundarios());
    }

    private EmpresaResponseDTO toResponseDTO(Empresa empresa) {
        Endereco end = empresa.getEndereco();
        EnderecoDTO enderecoDto = end == null ? null : new EnderecoDTO(
                end.getLogradouro(), end.getNumero(), end.getComplemento(),
                end.getBairro(), end.getCidade(), end.getUf(), end.getCep()
        );

        return new EmpresaResponseDTO(
                empresa.getId(),
                empresa.getNomeFantasia(),
                empresa.getRazaoSocial(),
                empresa.getCnpj() != null ? empresa.getCnpj().getNumero() : null,
                empresa.getTelefone() != null ? empresa.getTelefone().getNumero() : null,
                empresa.getEmail() != null ? empresa.getEmail().getEndereco() : null,
                empresa.getDataAbertura(),
                empresa.getSituacaoCadastral(),
                empresa.getNaturezaJuridica(),
                empresa.getSite(),
                enderecoDto,
                empresa.getInscricaoEstadual(),
                empresa.getInscricaoMunicipal(),
                empresa.getRegimeTributario(),
                empresa.getCnaePrincipal(),
                empresa.getCnaesSecundarios()
        );
    }
}
