package diniz.contabilidade.arquivos.model.enums;

/** Tipo de documento identificado pela leitura inteligente (IA ou padrões). */
public enum TipoDocumento {
    DAS("DAS — Simples Nacional", CategoriaFiscal.DAS),
    DARF("DARF — Receita Federal", CategoriaFiscal.DARF),
    GPS("GPS / INSS", CategoriaFiscal.GUIA_IMPOSTO),
    FGTS("Guia do FGTS (GRF/GFD)", CategoriaFiscal.GUIA_IMPOSTO),
    ICMS("ICMS (DARE/DAE)", CategoriaFiscal.GUIA_IMPOSTO),
    ISS("ISS — Imposto sobre Serviços", CategoriaFiscal.GUIA_IMPOSTO),
    NFE("Nota fiscal eletrônica (NF-e)", CategoriaFiscal.NFE_ENTRADA),
    NFSE("Nota fiscal de serviço (NFS-e)", CategoriaFiscal.NFE_ENTRADA),
    EXTRATO_BANCARIO("Extrato bancário", CategoriaFiscal.EXTRATO_BANCARIO),
    CERTIDAO("Certidão", CategoriaFiscal.CERTIDAO),
    CONTRATO("Contrato / alteração contratual", CategoriaFiscal.CONTRATO_SOCIAL),
    BALANCETE("Balancete", CategoriaFiscal.BALANCETE),
    FOLHA("Folha de pagamento", CategoriaFiscal.FOLHA_PAGAMENTO),
    OUTRO("Outro documento", CategoriaFiscal.OUTRO);

    private final String rotulo;
    private final CategoriaFiscal categoria;

    TipoDocumento(String rotulo, CategoriaFiscal categoria) {
        this.rotulo = rotulo;
        this.categoria = categoria;
    }

    public String getRotulo() {
        return rotulo;
    }

    public CategoriaFiscal getCategoria() {
        return categoria;
    }

    /** Guias de recolhimento (têm valor, vencimento e linha digitável). */
    public boolean isGuia() {
        return this == DAS || this == DARF || this == GPS || this == FGTS || this == ICMS || this == ISS;
    }

    public static TipoDocumento de(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (v.startsWith("GPS") || v.equals("INSS")) return GPS;
        if (v.startsWith("FGTS")) return FGTS;
        if (v.startsWith("ICMS") || v.startsWith("DARE") || v.startsWith("DAE")) return ICMS;
        if (v.startsWith("EXTRATO")) return EXTRATO_BANCARIO;
        for (TipoDocumento t : values()) {
            if (t.name().equals(v)) return t;
        }
        return null;
    }
}
