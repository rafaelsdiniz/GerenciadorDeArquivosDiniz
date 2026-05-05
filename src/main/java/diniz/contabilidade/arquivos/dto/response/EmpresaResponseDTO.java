package diniz.contabilidade.arquivos.dto.response;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.model.enums.NaturezaJuridica;
import diniz.contabilidade.arquivos.model.enums.RegimeTributario;
import diniz.contabilidade.arquivos.model.enums.SituacaoCadastral;

public record EmpresaResponseDTO(

    Long id,
    String nomeFantasia,
    String razaoSocial,
    String cnpj,
    String telefone,
    String email,

    LocalDate dataAbertura,
    SituacaoCadastral situacaoCadastral,
    NaturezaJuridica naturezaJuridica,
    String site,

    EnderecoDTO endereco,

    String inscricaoEstadual,
    String inscricaoMunicipal,
    RegimeTributario regimeTributario,
    String cnaePrincipal,
    String cnaesSecundarios

) {}
