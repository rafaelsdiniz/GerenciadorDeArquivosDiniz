package diniz.contabilidade.arquivos.dto.request;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.dto.response.EnderecoDTO;
import diniz.contabilidade.arquivos.model.enums.NaturezaJuridica;
import diniz.contabilidade.arquivos.model.enums.RegimeTributario;
import diniz.contabilidade.arquivos.model.enums.SituacaoCadastral;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmpresaRequestDTO(

    @NotBlank(message = "O nome fantasia é obrigatório.")
    String nomeFantasia,

    @NotBlank(message = "A razão social é obrigatória.")
    String razaoSocial,

    @NotBlank(message = "O CNPJ é obrigatório.")
    String cnpj,

    @NotBlank(message = "O telefone é obrigatório.")
    String telefone,

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "O email informado é inválido.")
    String email,

    // --- campos opcionais (cadastro completo) ---

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
