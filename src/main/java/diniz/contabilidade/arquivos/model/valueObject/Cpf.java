package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Cpf {

    @Column(name = "cpf", nullable = false, unique = true)
    private String numero;

    public Cpf() {}

    public Cpf(String numero) {
        numero = numero.replaceAll("\\D", "");

        if (numero.length() != 11 || numero.matches("(\\d)\\1{10}")) {
            throw new IllegalArgumentException("CPF inválido: " + numero);
        }

        this.numero = numero;
    }

    public String getNumero() {
        return numero;
    }

    public static Cpf of(String numero) {
        return new Cpf(numero);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cpf cpf)) return false;
        return Objects.equals(numero, cpf.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }
}