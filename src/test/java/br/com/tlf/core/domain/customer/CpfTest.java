package br.com.tlf.core.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.tlf.core.domain.exception.InvalidCpfParameterException;

class CpfTest {

    @Test
    void acceptsAValidCpfAndKeepsTheValueUnchanged() {
        assertThat(Cpf.of("52998224725").value()).isEqualTo("52998224725");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678900",   // wrong check digits
            "11111111111",   // all identical digits
            "5299822472",    // too short
            "529982247251",  // too long
            "529.982.247-25" // formatted
    })
    void rejectsAnythingThatIsNotElevenValidDigits(String candidate) {
        assertThat(Cpf.isValid(candidate)).isFalse();
        assertThrows(InvalidCpfParameterException.class, () -> Cpf.of(candidate));
    }
}
