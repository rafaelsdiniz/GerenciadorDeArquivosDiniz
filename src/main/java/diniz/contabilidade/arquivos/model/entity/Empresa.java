package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;
import java.util.List;

import diniz.contabilidade.arquivos.model.enums.NaturezaJuridica;
import diniz.contabilidade.arquivos.model.enums.RegimeTributario;
import diniz.contabilidade.arquivos.model.enums.SituacaoCadastral;
import diniz.contabilidade.arquivos.model.valueObject.Cnpj;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import diniz.contabilidade.arquivos.model.valueObject.Endereco;
import diniz.contabilidade.arquivos.model.valueObject.Telefone;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;

@Entity
public class Empresa extends DefaultEntity {

    private String nomeFantasia;

    private String razaoSocial;

    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "cnpj"))
    private Cnpj cnpj;

    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "telefone"))
    private Telefone telefone;

    @Embedded
    @AttributeOverride(name = "endereco", column = @Column(name = "email"))
    private Email email;

    @Embedded
    private Endereco endereco;

    private LocalDate dataAbertura;

    @Enumerated(EnumType.STRING)
    private SituacaoCadastral situacaoCadastral;

    @Enumerated(EnumType.STRING)
    private NaturezaJuridica naturezaJuridica;

    private String site;

    @Column(name = "inscricao_estadual")
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal")
    private String inscricaoMunicipal;

    @Enumerated(EnumType.STRING)
    private RegimeTributario regimeTributario;

    private String cnaePrincipal;

    @Column(length = 1000)
    private String cnaesSecundarios;

    @OneToMany(mappedBy = "empresa")
    private List<Usuario> usuarios;

    @OneToMany(mappedBy = "empresa")
    private List<Socio> socios;

    @OneToMany(mappedBy = "empresa")
    private List<Pasta> pastas;

    public Empresa() {}

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public Cnpj getCnpj() { return cnpj; }
    public void setCnpj(Cnpj cnpj) { this.cnpj = cnpj; }

    public Telefone getTelefone() { return telefone; }
    public void setTelefone(Telefone telefone) { this.telefone = telefone; }

    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }

    public SituacaoCadastral getSituacaoCadastral() { return situacaoCadastral; }
    public void setSituacaoCadastral(SituacaoCadastral situacaoCadastral) { this.situacaoCadastral = situacaoCadastral; }

    public NaturezaJuridica getNaturezaJuridica() { return naturezaJuridica; }
    public void setNaturezaJuridica(NaturezaJuridica naturezaJuridica) { this.naturezaJuridica = naturezaJuridica; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }

    public String getInscricaoMunicipal() { return inscricaoMunicipal; }
    public void setInscricaoMunicipal(String inscricaoMunicipal) { this.inscricaoMunicipal = inscricaoMunicipal; }

    public RegimeTributario getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(RegimeTributario regimeTributario) { this.regimeTributario = regimeTributario; }

    public String getCnaePrincipal() { return cnaePrincipal; }
    public void setCnaePrincipal(String cnaePrincipal) { this.cnaePrincipal = cnaePrincipal; }

    public String getCnaesSecundarios() { return cnaesSecundarios; }
    public void setCnaesSecundarios(String cnaesSecundarios) { this.cnaesSecundarios = cnaesSecundarios; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }

    public List<Socio> getSocios() { return socios; }
    public void setSocios(List<Socio> socios) { this.socios = socios; }

    public List<Pasta> getPastas() { return pastas; }
    public void setPastas(List<Pasta> pastas) { this.pastas = pastas; }
}
