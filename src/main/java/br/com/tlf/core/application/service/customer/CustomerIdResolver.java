package br.com.tlf.core.application.service.customer;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import br.com.tlf.core.domain.exception.InvalidCpfParameterException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;

@Slf4j
@Component
public class CustomerIdResolver {

        private static final String CPF_DIGITS_REGEX = "\\d{11}";

        public String resolve(String authorization, String customerIdHeader) {
                String customerId = customerIdHeader;
                log.info("Resolving customerId for authorization: {} and customerIdHeader: {}", authorization, customerIdHeader);

                if (customerIdHeader == null || customerIdHeader.isEmpty()) {
                        customerId = JwtTokenUtils.cpfToken(authorization);
                }

                if (customerId == null || customerId.isEmpty()) {
                        throw new MandatoryTermNotAcceptedException("customerId is required",
                                        List.of("x-customer-id or Authorization header is missing"));
                }

                validateCpf(customerId);

                return customerId;
        }

        private void validateCpf(String customerId) {
                if (!isValidCpf(customerId)) {
                        throw new InvalidCpfParameterException("Invalid CPF parameter",
                                        List.of("x-customer-id must be a valid CPF with 11 numeric digits"));
                }
        }

        private boolean isValidCpf(String cpf) {
                if (cpf == null || !cpf.matches(CPF_DIGITS_REGEX)) {
                        return false;
                }

                if (cpf.chars().distinct().count() == 1) {
                        return false;
                }

                int firstDigit = calculateVerifierDigit(cpf, 9, 10);
                int secondDigit = calculateVerifierDigit(cpf, 10, 11);

                return firstDigit == Character.getNumericValue(cpf.charAt(9))
                                && secondDigit == Character.getNumericValue(cpf.charAt(10));
        }

        private int calculateVerifierDigit(String cpf, int length, int weightStart) {
                int sum = 0;
                for (int i = 0; i < length; i++) {
                        sum += Character.getNumericValue(cpf.charAt(i)) * (weightStart - i);
                }
                int remainder = sum % 11;
                return remainder < 2 ? 0 : 11 - remainder;
        }
}
