package diniz.contabilidade.arquivos.model.entity;

import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class ObrigacaoRecorrente extends DefaultEntity {

    private String nome;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private Periodicidade periodicidade;

    private Integer diaVencimento;

    @Enumerated(EnumType.STRING)
    private TipoArquivo tipoArquivoEsperado;

    private Boolean ativo;

    @ManyToOne
    private Empresa empresa;

    public ObrigacaoRecorrente() {}

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

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(Periodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public TipoArquivo getTipoArquivoEsperado() {
        return tipoArquivoEsperado;
    }

    public void setTipoArquivoEsperado(TipoArquivo tipoArquivoEsperado) {
        this.tipoArquivoEsperado = tipoArquivoEsperado;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}
