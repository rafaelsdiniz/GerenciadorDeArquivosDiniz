package diniz.contabilidade.arquivos.model.entity;

import diniz.contabilidade.arquivos.model.valueObject.Cpf;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Socio extends DefaultEntity {

    private String nome;

    @Embedded
    private Cpf cpf;

    @ManyToOne
    private Empresa empresa;

    private Double participacao;

    private Boolean administrador;

    public Socio() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Cpf getCpf() { return cpf; }
    public void setCpf(Cpf cpf) { this.cpf = cpf; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Double getParticipacao() { return participacao; }
    public void setParticipacao(Double participacao) { this.participacao = participacao; }

    public Boolean getAdministrador() { return administrador; }
    public void setAdministrador(Boolean administrador) { this.administrador = administrador; }
}
