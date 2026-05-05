package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Endereco {

    @Column(name = "logradouro")
    private String logradouro;

    @Column(name = "numero_endereco")
    private String numero;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "bairro")
    private String bairro;

    @Column(name = "cidade")
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep")
    private String cep;

    public Endereco() {}

    public Endereco(String logradouro, String numero, String complemento,
                    String bairro, String cidade, String uf, String cep) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf != null ? uf.toUpperCase() : null;
        this.cep = cep != null ? cep.replaceAll("\\D", "") : null;
    }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf != null ? uf.toUpperCase() : null; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep != null ? cep.replaceAll("\\D", "") : null; }
}
