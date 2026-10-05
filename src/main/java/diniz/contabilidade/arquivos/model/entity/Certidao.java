package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import diniz.contabilidade.arquivos.model.enums.SituacaoCertidao;
import diniz.contabilidade.arquivos.model.enums.TipoCertidao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Certidão negativa de débitos (CND) obtida pelo escritório para uma empresa,
 * com controle de validade. O PDF fica no Drive da empresa (arquivo opcional).
 */
@Entity
@Table(indexes = {
    @Index(name = "idx_certidao_validade", columnList = "dataValidade")
})
public class Certidao extends DefaultEntity {

    /** certidões são da empresa: somem junto com ela */
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCertidao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoCertidao situacao;

    private String numero;

    private LocalDate dataEmissao;

    @Column(nullable = false)
    private LocalDate dataValidade;

    private String orgaoEmissor;

    @Column(length = 1000)
    private String observacao;

    /** PDF da certidão no Drive; se o arquivo for excluído de vez, o vínculo é desfeito */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Arquivo arquivo;

    public Certidao() {
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public TipoCertidao getTipo() {
        return tipo;
    }

    public void setTipo(TipoCertidao tipo) {
        this.tipo = tipo;
    }

    public SituacaoCertidao getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoCertidao situacao) {
        this.situacao = situacao;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getOrgaoEmissor() {
        return orgaoEmissor;
    }

    public void setOrgaoEmissor(String orgaoEmissor) {
        this.orgaoEmissor = orgaoEmissor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Arquivo getArquivo() {
        return arquivo;
    }

    public void setArquivo(Arquivo arquivo) {
        this.arquivo = arquivo;
    }
}
