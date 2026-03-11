package diniz.contabilidade.arquivos.model.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Pasta extends DefaultEntity {

    private String nome;

    private String descricao;

    @ManyToOne
    private Empresa empresa;

    @ManyToOne
    private Pasta pastaPai;

    @OneToMany(mappedBy = "pastaPai")
    private List<Pasta> subpastas;

    @OneToMany(mappedBy = "pasta")
    private List<Arquivo> arquivos;

    public Pasta(){

    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Pasta getPastaPai() {
        return pastaPai;
    }

    public void setPastaPai(Pasta pastaPai) {
        this.pastaPai = pastaPai;
    }

    public List<Pasta> getSubpastas() {
        return subpastas;
    }

    public void setSubpastas(List<Pasta> subpastas) {
        this.subpastas = subpastas;
    }

    public List<Arquivo> getArquivos() {
        return arquivos;
    }

    public void setArquivos(List<Arquivo> arquivos) {
        this.arquivos = arquivos;
    }

    
}
