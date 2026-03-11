package diniz.contabilidade.arquivos.model.entity;

import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class Usuario extends DefaultEntity {

    private String nome;

    @Embedded
    private Email email;

    private String senha;

    @Enumerated(EnumType.STRING)
    private PerfilUsuario perfilUsuario;

    @ManyToOne
    private Empresa empresa;

    public Usuario(){

    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public PerfilUsuario getPerfilUsuario() {
        return perfilUsuario;
    }

    public void setPerfilUsuario(PerfilUsuario perfil) {
        this.perfilUsuario = perfil;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    
}