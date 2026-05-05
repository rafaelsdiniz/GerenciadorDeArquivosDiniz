package diniz.contabilidade.arquivos.model.entity;

import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class LogAcesso extends DefaultEntity {

    @ManyToOne
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private AcaoLog acao;

    private String entidade;

    private Long idEntidade;

    @Column(length = 500)
    private String detalhes;

    public LogAcesso() {}

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public AcaoLog getAcao() {
        return acao;
    }

    public void setAcao(AcaoLog acao) {
        this.acao = acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public Long getIdEntidade() {
        return idEntidade;
    }

    public void setIdEntidade(Long idEntidade) {
        this.idEntidade = idEntidade;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}
