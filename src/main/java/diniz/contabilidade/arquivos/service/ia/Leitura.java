package diniz.contabilidade.arquivos.service.ia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import diniz.contabilidade.arquivos.model.enums.TipoDocumento;

/** Campos lidos de um documento (mutável: usado pela leitura por padrões e pela IA antes da fusão). */
final class Leitura {

    TipoDocumento tipo;
    String descricao;
    String cnpj;
    /** Todos os CNPJs válidos encontrados (emitente, destinatário...). */
    List<String> cnpjs = new ArrayList<>();
    String razaoSocial;
    String competencia;
    LocalDate vencimento;
    BigDecimal valor;
    String linhaDigitavel;
    String codigoBarras;
    /** Dígitos verificadores da linha digitável conferem. */
    boolean linhaValida;
    /** Valor codificado na própria linha digitável (mais confiável que o texto). */
    BigDecimal valorDaLinha;
    String numeroDocumento;
    LocalDate validade;
    Double confianca;
    List<String> observacoes = new ArrayList<>();

    int camposEncontrados() {
        int n = 0;
        if (tipo != null && tipo != TipoDocumento.OUTRO) n++;
        if (cnpj != null) n++;
        if (valor != null) n++;
        if (vencimento != null) n++;
        if (competencia != null) n++;
        if (linhaDigitavel != null) n++;
        return n;
    }
}
