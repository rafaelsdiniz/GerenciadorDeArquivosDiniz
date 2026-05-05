package diniz.contabilidade.arquivos.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class Arquivo extends DefaultEntity{

    private String nome;
    private String nomeOriginal;
    private Long tamanho;

    @Enumerated(EnumType.STRING)
    private TipoArquivo tipoArquivo;

    private String caminho;

    @jakarta.persistence.Column(columnDefinition="TEXT")
    private String arquivoBase64;

    private String hash;

    private String descricao;

    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusArquivo status;

    @Enumerated(EnumType.STRING)
    private CategoriaFiscal categoriaFiscal;

    private LocalDateTime excluidoEm;

    @ManyToOne
    private Empresa empresa;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Pasta pasta;

    @ManyToOne
    private ObrigacaoPendente obrigacaoPendente;

    public String getNome() {
        return nome;
    }

    public Arquivo(){}

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public Long getTamanho() {
        return tamanho;
    }

    public void setTamanho(Long tamanho) {
        this.tamanho = tamanho;
    }

    public TipoArquivo getTipoArquivo() {
        return tipoArquivo;
    }

    public void setTipoArquivo(TipoArquivo tipoArquivo) {
        this.tipoArquivo = tipoArquivo;
    }

    public String getCaminho() {
        return caminho;
    }

    public void setCaminho(String caminho) {
        this.caminho = caminho;
    }

    public String getArquivoBase64() {
        return arquivoBase64;
    }

    public void setArquivoBase64(String arquivoBase64) {
        this.arquivoBase64 = arquivoBase64;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Pasta getPasta() {
        return pasta;
    }

    public void setPasta(Pasta pasta) {
        this.pasta = pasta;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public StatusArquivo getStatus() {
        return status;
    }

    public void setStatus(StatusArquivo status) {
        this.status = status;
    }

    public ObrigacaoPendente getObrigacaoPendente() {
        return obrigacaoPendente;
    }

    public void setObrigacaoPendente(ObrigacaoPendente obrigacaoPendente) {
        this.obrigacaoPendente = obrigacaoPendente;
    }

    public CategoriaFiscal getCategoriaFiscal() {
        return categoriaFiscal;
    }

    public void setCategoriaFiscal(CategoriaFiscal categoriaFiscal) {
        this.categoriaFiscal = categoriaFiscal;
    }

    public LocalDateTime getExcluidoEm() {
        return excluidoEm;
    }

    public void setExcluidoEm(LocalDateTime excluidoEm) {
        this.excluidoEm = excluidoEm;
    }
}
