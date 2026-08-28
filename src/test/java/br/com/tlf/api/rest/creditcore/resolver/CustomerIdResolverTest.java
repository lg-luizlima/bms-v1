package br.com.tlf.api.rest.creditcore.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import br.com.tlf.core.domain.exception.InvalidCpfParameterException;
import br.com.tlf.core.domain.exception.MissingCustomerIdentificationException;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;

class CustomerIdResolverTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final String VALID_CPF = "52998224725";

    private final CustomerIdResolver underTest = new CustomerIdResolver();

    @Test
    void customerIdHeaderIsValidCpf_returnsHeaderValue() {
        assertThat(underTest.resolve(AUTHORIZATION, VALID_CPF)).isEqualTo(VALID_CPF);
    }

    @Test
    void customerIdHeaderIsInvalidCpf_throwsInvalidCpfParameter() {
        assertThrows(InvalidCpfParameterException.class, () -> underTest.resolve(AUTHORIZATION, "12345678900"));
    }

    @Test
    void customerIdHeaderMissing_fallsBackToTokenClaim() {
        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {
            jwtMock.when(() -> JwtTokenUtils.cpfToken(AUTHORIZATION)).thenReturn(VALID_CPF);

            assertThat(underTest.resolve(AUTHORIZATION, "")).isEqualTo(VALID_CPF);
        }
    }

    @Test
    void neitherHeaderNorTokenCarryCustomerId_throwsMissingCustomerIdentification() {
        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {
            jwtMock.when(() -> JwtTokenUtils.cpfToken(AUTHORIZATION)).thenReturn("");

            assertThrows(MissingCustomerIdentificationException.class, () -> underTest.resolve(AUTHORIZATION, ""));
        }
    }
}
