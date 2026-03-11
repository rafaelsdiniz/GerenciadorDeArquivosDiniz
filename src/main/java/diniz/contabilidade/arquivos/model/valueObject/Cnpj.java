package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Cnpj {

    private String numero;

    public Cnpj() {}

    public Cnpj(String numero) {
        numero = numero.replaceAll("\\D", "");

        if (numero.length() != 14) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }

        this.numero = numero;
    }

    public String getNumero() {
        return numero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cnpj cnpj)) return false;
        return Objects.equals(numero, cnpj.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }
}