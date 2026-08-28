package br.com.tlf.api.rest.creditcore.resolver;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.tlf.core.domain.customer.Cpf;
import br.com.tlf.core.domain.exception.MissingCustomerIdentificationException;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;


@Component
public class CustomerIdResolver {

    public String resolve(String authorization, String customerIdHeader) {
        String customerId = hasText(customerIdHeader)
                ? customerIdHeader
                : JwtTokenUtils.cpfToken(authorization);

        if (!hasText(customerId)) {
            throw new MissingCustomerIdentificationException("customerId is required",
                    List.of("x-customer-id or Authorization header is missing"));
        }

        return Cpf.of(customerId).value();
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
