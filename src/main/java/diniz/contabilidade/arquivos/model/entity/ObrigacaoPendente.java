package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;

import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class ObrigacaoPendente extends DefaultEntity {

    @ManyToOne
    private ObrigacaoRecorrente obrigacaoRecorrente;

    @ManyToOne
    private Empresa empresa;

    private LocalDate dataVencimento;

    private LocalDate dataEntrega;

    /** Data em que a guia foi paga (confirmada pelo cliente ou pelo escritório). */
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    private StatusObrigacao status;

    public ObrigacaoPendente() {}

    public ObrigacaoRecorrente getObrigacaoRecorrente() {
        return obrigacaoRecorrente;
    }

    public void setObrigacaoRecorrente(ObrigacaoRecorrente obrigacaoRecorrente) {
        this.obrigacaoRecorrente = obrigacaoRecorrente;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public StatusObrigacao getStatus() {
        return status;
    }

    public void setStatus(StatusObrigacao status) {
        this.status = status;
    }
}
