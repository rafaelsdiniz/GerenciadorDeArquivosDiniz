package diniz.contabilidade.arquivos.model.valueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Email {

    private String endereco;

    public Email() {}

    public Email(String endereco) {
        if (endereco == null || !endereco.contains("@")) {
            throw new IllegalArgumentException("Email inválido.");
        }
        this.endereco = endereco;
    }

    public String getEndereco() {
        return endereco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(endereco, email.endereco);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endereco);
    }
}