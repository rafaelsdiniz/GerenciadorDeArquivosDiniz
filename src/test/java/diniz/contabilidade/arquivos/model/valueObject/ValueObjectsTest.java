package diniz.contabilidade.arquivos.model.valueObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes de unidade puros (sem Quarkus) dos objetos de valor do domínio. */
@DisplayName("Objetos de valor")
class ValueObjectsTest {

    @Nested
    @DisplayName("Cnpj")
    class CnpjTest {

        @Test
        @DisplayName("Aceita CNPJ com máscara e guarda só os 14 dígitos")
        void normalizaMascara() {
            assertEquals("12345678000190", new Cnpj("12.345.678/0001-90").getNumero());
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "1234567800019", "123456780001901", "11111111111111", "abc"})
        @DisplayName("Recusa nulo, tamanho errado e dígitos todos iguais")
        void recusaInvalidos(String numero) {
            assertThrows(IllegalArgumentException.class, () -> new Cnpj(numero));
        }

        @Test
        @DisplayName("Dois CNPJs com o mesmo número são iguais, com ou sem máscara")
        void igualdadePorValor() {
            assertEquals(new Cnpj("12345678000190"), new Cnpj("12.345.678/0001-90"));
        }
    }

    @Nested
    @DisplayName("Cpf")
    class CpfTest {

        @Test
        @DisplayName("Aceita CPF com máscara e guarda só os 11 dígitos")
        void normalizaMascara() {
            assertEquals("12345678901", new Cpf("123.456.789-01").getNumero());
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @ValueSource(strings = {"1234567890", "123456789012", "00000000000"})
        @DisplayName("Recusa tamanho errado e dígitos todos iguais")
        void recusaInvalidos(String numero) {
            assertThrows(IllegalArgumentException.class, () -> new Cpf(numero));
        }
    }

    @Nested
    @DisplayName("Email")
    class EmailTest {

        @Test
        @DisplayName("Remove espaços e converte para minúsculas")
        void normaliza() {
            assertEquals("contato@diniz.com.br", new Email("  Contato@Diniz.com.BR ").getEndereco());
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @NullSource
        @ValueSource(strings = {"sem-arroba", "a@", "@dominio.com", "com espaco@x.com"})
        @DisplayName("Recusa endereços malformados")
        void recusaInvalidos(String endereco) {
            assertThrows(IllegalArgumentException.class, () -> new Email(endereco));
        }
    }

    @Nested
    @DisplayName("Telefone")
    class TelefoneTest {

        @Test
        @DisplayName("Guarda só os dígitos e formata celular (11) e fixo (10)")
        void formata() {
            Telefone celular = new Telefone("(63) 99999-1234");
            assertEquals("63999991234", celular.getNumero());
            assertEquals("(63) 99999-1234", celular.getFormatado());
            assertEquals("(63) 3215-0000", new Telefone("6332150000").getFormatado());
        }

        @ParameterizedTest(name = "recusa \"{0}\"")
        @ValueSource(strings = {"123456789", "123456789012", "9999999999"})
        @DisplayName("Recusa tamanho errado e dígitos todos iguais")
        void recusaInvalidos(String numero) {
            assertThrows(IllegalArgumentException.class, () -> new Telefone(numero));
        }
    }
}
