package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/**
 * Mensagem trocada entre o escritório e o cliente dentro de uma obrigação.
 *
 * Cada lado tem o seu "lido em": uma mensagem do cliente fica não lida para o escritório
 * até um ADMIN abrir a conversa, e vice-versa. O autor já nasce com o próprio lado lido.
 */
@Entity
public class MensagemObrigacao extends DefaultEntity {

    public static final int TAMANHO_MAXIMO = 2000;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ObrigacaoPendente obrigacaoPendente;

    @ManyToOne(optional = false)
    private Usuario autor;

    /** true = enviada pelo escritório (ADMIN); false = enviada pelo cliente. */
    @Column(nullable = false)
    private boolean doEscritorio;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    /** Arquivo citado na mensagem (opcional). Se o arquivo for excluído, a mensagem permanece. */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Arquivo arquivo;

    private LocalDateTime lidaPeloEscritorioEm;

    private LocalDateTime lidaPeloClienteEm;

    public MensagemObrigacao() {}

    public ObrigacaoPendente getObrigacaoPendente() {
        return obrigacaoPendente;
    }

    public void setObrigacaoPendente(ObrigacaoPendente obrigacaoPendente) {
        this.obrigacaoPendente = obrigacaoPendente;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public boolean isDoEscritorio() {
        return doEscritorio;
    }

    public void setDoEscritorio(boolean doEscritorio) {
        this.doEscritorio = doEscritorio;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Arquivo getArquivo() {
        return arquivo;
    }

    public void setArquivo(Arquivo arquivo) {
        this.arquivo = arquivo;
    }

    public LocalDateTime getLidaPeloEscritorioEm() {
        return lidaPeloEscritorioEm;
    }

    public void setLidaPeloEscritorioEm(LocalDateTime lidaPeloEscritorioEm) {
        this.lidaPeloEscritorioEm = lidaPeloEscritorioEm;
    }

    public LocalDateTime getLidaPeloClienteEm() {
        return lidaPeloClienteEm;
    }

    public void setLidaPeloClienteEm(LocalDateTime lidaPeloClienteEm) {
        this.lidaPeloClienteEm = lidaPeloClienteEm;
    }
}
