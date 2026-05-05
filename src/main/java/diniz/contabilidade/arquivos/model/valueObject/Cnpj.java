package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Cnpj {

    @Column(name = "cnpj", nullable = false, unique = true)
    private String numero;

    public Cnpj() {}

    public Cnpj(String numero) {
        if (numero == null) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo.");
        }

        numero = numero.replaceAll("\\D", "");

        if (numero.length() != 14 || numero.matches("(\\d)\\1{13}")) {
            throw new IllegalArgumentException("CNPJ inválido: " + numero);
        }

        this.numero = numero;
    }

    public String getNumero() {
        return numero;
    }

    public static Cnpj of(String numero) {
        return new Cnpj(numero);
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