package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Email {

    @Column(name = "email", nullable = false, unique = true)
    private String endereco;

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public Email() {}

    public Email(String endereco) {
        if (endereco == null) {
            throw new IllegalArgumentException("Email não pode ser nulo.");
        }

        endereco = endereco.trim().toLowerCase();

        if (!endereco.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email inválido: " + endereco);
        }

        this.endereco = endereco;
    }

    public String getEndereco() {
        return endereco;
    }

    public static Email of(String endereco) {
        return new Email(endereco);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(endereco, email.endereco);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endereco);
    }
}