package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import diniz.contabilidade.arquivos.model.enums.EtapaFechamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Fechamento mensal de uma empresa numa competência ("MM/aaaa"): em que etapa está,
 * quem do escritório é o responsável e qual o prazo interno para concluir.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_fechamento_empresa_competencia",
        columnNames = {"empresa_id", "competencia"}))
public class FechamentoMensal extends DefaultEntity {

    /** Ao excluir a empresa, os fechamentos dela saem junto (só existem por causa dela). */
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Empresa empresa;

    /** Mês de referência "MM/aaaa" (mesma convenção das obrigações: vencimento menos 1 mês). */
    @Column(length = 7, nullable = false)
    private String competencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapaFechamento etapa;

    /** Pessoa do escritório (ADMIN) que conduz o fechamento. Excluir o usuário só limpa o campo. */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Usuario responsavel;

    /** Prazo interno para concluir (padrão: dia 20 do mês seguinte à competência). */
    private LocalDate prazo;

    @Column(length = 1000)
    private String observacao;

    private LocalDateTime concluidoEm;

    public FechamentoMensal() {}

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(String competencia) {
        this.competencia = competencia;
    }

    public EtapaFechamento getEtapa() {
        return etapa;
    }

    public void setEtapa(EtapaFechamento etapa) {
        this.etapa = etapa;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public void setPrazo(LocalDate prazo) {
        this.prazo = prazo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }
}
