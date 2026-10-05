package diniz.contabilidade.arquivos.model.enums;

/** Tipos de certidão negativa de débitos acompanhados pelo escritório. */
public enum TipoCertidao {
    /** CND conjunta Receita Federal / PGFN */
    FEDERAL("CND Federal"),
    /** Certidão negativa estadual (SEFAZ-TO) */
    ESTADUAL("CND Estadual"),
    /** Certidão negativa municipal (prefeitura) */
    MUNICIPAL("CND Municipal"),
    /** Certificado de Regularidade do FGTS (CRF / Caixa) */
    FGTS("CRF FGTS"),
    /** Certidão Negativa de Débitos Trabalhistas (CNDT / TST) */
    TRABALHISTA("CNDT Trabalhista"),
    /** Certidão negativa de falência e recuperação judicial */
    FALENCIA("Certidão de Falência"),
    OUTRA("Certidão");

    private final String rotulo;

    TipoCertidao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
