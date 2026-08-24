package br.com.tlf.core.application.service.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import br.com.tlf.core.domain.exception.InvalidCpfParameterException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;

class CustomerIdResolverTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final String VALID_CPF = "52998224725";

    private final CustomerIdResolver underTest = new CustomerIdResolver();

    @Test
    void resolve_whenCustomerIdHeaderIsValidCpf_returnsHeaderValue() {
        String result = underTest.resolve(AUTHORIZATION, VALID_CPF);

        assertThat(result).isEqualTo(VALID_CPF);
    }

    @Test
    void resolve_whenCustomerIdHeaderIsInvalidCpf_throwsInvalidCpfParameterException() {
        assertThrows(InvalidCpfParameterException.class, () -> underTest.resolve(AUTHORIZATION, "12345678900"));
    }

    @Test
    void resolve_whenCustomerIdHeaderIsMissing_usesAuthorizationCpf() {
        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {
            jwtMock.when(() -> JwtTokenUtils.cpfToken(AUTHORIZATION)).thenReturn(VALID_CPF);

            String result = underTest.resolve(AUTHORIZATION, "");

            assertThat(result).isEqualTo(VALID_CPF);
        }
    }

    @Test
    void resolve_whenCustomerIdHeaderAndAuthorizationCpfAreMissing_throwsMandatoryTermNotAcceptedException() {
        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {
            jwtMock.when(() -> JwtTokenUtils.cpfToken(AUTHORIZATION)).thenReturn("");

            assertThrows(MandatoryTermNotAcceptedException.class, () -> underTest.resolve(AUTHORIZATION, ""));
        }
    }
}
