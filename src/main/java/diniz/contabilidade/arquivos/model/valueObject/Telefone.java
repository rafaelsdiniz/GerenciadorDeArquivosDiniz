package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Telefone {

    @Column(name = "telefone", nullable = false)
    private String numero;

    public Telefone() {}

    public Telefone(String numero) {

        if (numero == null) {
            throw new IllegalArgumentException("Telefone não pode ser nulo.");
        }

        String telefoneLimpo = numero.replaceAll("\\D", "");

        if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 11 ||
            telefoneLimpo.matches("(\\d)\\1{9,10}")) {
            throw new IllegalArgumentException("Telefone inválido: " + numero);
        }

        this.numero = telefoneLimpo;
    }

    public String getNumero() {
        return numero;
    }

    public String getFormatado() {
        if (numero.length() == 11) {
            return "(" + numero.substring(0, 2) + ") " +
                   numero.substring(2, 7) + "-" +
                   numero.substring(7);
        }
        return "(" + numero.substring(0, 2) + ") " +
               numero.substring(2, 6) + "-" +
               numero.substring(6);
    }

    public static Telefone of(String numero) {
        return new Telefone(numero);
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