package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cópia local (somente leitura) de uma comunicação do DEC — Domicílio Eletrônico
 * do Contribuinte (SEFAZ-TO), sincronizada a partir do DEC Monitor.
 *
 * O DEC é a fonte da verdade: aqui só se exibe. A empresa é ligada pelo CNPJ;
 * comunicações de CNPJ que não existe no Gerenciador ficam com empresa nula e
 * aparecem só para o escritório.
 */
@Entity
@Table(indexes = {
    @Index(name = "idx_comdec_externo", columnList = "idExterno", unique = true),
    @Index(name = "idx_comdec_cnpj", columnList = "cnpj")
})
public class ComunicacaoDec extends DefaultEntity {

    /** id da comunicação no DEC (uuid). */
    private String idExterno;

    @ManyToOne
    private Empresa empresa;

    private String cnpj;
    private String razaoSocial;
    private String numero;
    private String tipo;

    @Column(length = 1000)
    private String assunto;

    @Column(columnDefinition = "TEXT")
    private String corpo;

    private String remetente;
    private LocalDate disponibilizadaEm;
    private LocalDate cienteEm;
    private LocalDate prazoCienciaEm;
    private Integer diasParaResposta;
    private LocalDateTime coletadaEm;

    /** NOVA | EM_ANDAMENTO | RESOLVIDA | ARQUIVADA (no DEC) */
    private String status;

    /** CRITICA | ALTA | MEDIA | BAIXA | SEM_RISCO (calculada pelo DEC) */
    private String urgencia;

    @Column(length = 500)
    private String motivo;

    private LocalDate cienciaTacitaEm;
    private LocalDate prazoRespostaEm;
    private Boolean encerrada;
    private Boolean temInteiroTeor;

    @Column(length = 500)
    private String link;

    private LocalDateTime sincronizadaEm;

    public ComunicacaoDec() {}

    public String getIdExterno() { return idExterno; }
    public void setIdExterno(String idExterno) { this.idExterno = idExterno; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public String getCorpo() { return corpo; }
    public void setCorpo(String corpo) { this.corpo = corpo; }
    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }
    public LocalDate getDisponibilizadaEm() { return disponibilizadaEm; }
    public void setDisponibilizadaEm(LocalDate disponibilizadaEm) { this.disponibilizadaEm = disponibilizadaEm; }
    public LocalDate getCienteEm() { return cienteEm; }
    public void setCienteEm(LocalDate cienteEm) { this.cienteEm = cienteEm; }
    public LocalDate getPrazoCienciaEm() { return prazoCienciaEm; }
    public void setPrazoCienciaEm(LocalDate prazoCienciaEm) { this.prazoCienciaEm = prazoCienciaEm; }
    public Integer getDiasParaResposta() { return diasParaResposta; }
    public void setDiasParaResposta(Integer diasParaResposta) { this.diasParaResposta = diasParaResposta; }
    public LocalDateTime getColetadaEm() { return coletadaEm; }
    public void setColetadaEm(LocalDateTime coletadaEm) { this.coletadaEm = coletadaEm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUrgencia() { return urgencia; }
    public void setUrgencia(String urgencia) { this.urgencia = urgencia; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDate getCienciaTacitaEm() { return cienciaTacitaEm; }
    public void setCienciaTacitaEm(LocalDate cienciaTacitaEm) { this.cienciaTacitaEm = cienciaTacitaEm; }
    public LocalDate getPrazoRespostaEm() { return prazoRespostaEm; }
    public void setPrazoRespostaEm(LocalDate prazoRespostaEm) { this.prazoRespostaEm = prazoRespostaEm; }
    public Boolean getEncerrada() { return encerrada; }
    public void setEncerrada(Boolean encerrada) { this.encerrada = encerrada; }
    public Boolean getTemInteiroTeor() { return temInteiroTeor; }
    public void setTemInteiroTeor(Boolean temInteiroTeor) { this.temInteiroTeor = temInteiroTeor; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getSincronizadaEm() { return sincronizadaEm; }
    public void setSincronizadaEm(LocalDateTime sincronizadaEm) { this.sincronizadaEm = sincronizadaEm; }
}
