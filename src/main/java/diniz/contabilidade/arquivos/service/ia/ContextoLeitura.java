package diniz.contabilidade.arquivos.service.ia;

import java.time.LocalDate;

/**
 * O que se espera do documento: empresa da pasta/obrigação e dados da obrigação (para as conferências).
 * Todos os campos são opcionais.
 */
public record ContextoLeitura(
        String cnpjEmpresa,
        String nomeEmpresa,
        String nomeObrigacao,
        LocalDate vencimentoObrigacao,
        /** "MM/aaaa" (anuais "aaaa" não são comparadas). */
        String competenciaObrigacao) {

    public static ContextoLeitura vazio() {
        return new ContextoLeitura(null, null, null, null, null);
    }
}
