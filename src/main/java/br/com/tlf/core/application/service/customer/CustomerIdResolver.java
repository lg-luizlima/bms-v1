package br.com.tlf.core.application.service.customer;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;

@Component
public class CustomerIdResolver {

        public String resolve(String authorization, String customerIdHeader) {
                String customerId = customerIdHeader;
                if (customerIdHeader != null && !customerIdHeader.isEmpty()) {
                        customerId = JwtTokenUtils.cpfToken(authorization);
                }

                if (customerId == null || customerId.isEmpty()) {
                        throw new MandatoryTermNotAcceptedException("customerId is required",
                                        List.of("x-customer-id or Authorization header is missing"));
                }

                return customerId;
        }
}
