package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Telefone {

    private String numero;

    public Telefone() {
    }

    public Telefone(String numero) {

        if (numero == null) {
            throw new IllegalArgumentException("Telefone não pode ser nulo.");
        }

        String telefoneLimpo = numero.replaceAll("\\D", "");

        if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 11) {
            throw new IllegalArgumentException("Telefone inválido.");
        }

        this.numero = telefoneLimpo;
    }

    public String getNumero() {
        return numero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Telefone telefone)) return false;
        return Objects.equals(numero, telefone.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }
}