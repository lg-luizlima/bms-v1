package br.com.tlf.api.rest.creditcore;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.tlf.api.rest.UrlConstant;
import br.com.tlf.api.rest.creditcore.dto.request.ConsentRequestDTO;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.dto.response.ConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.mapper.CreditCoreApiMapper;
import br.com.tlf.api.rest.creditcore.resolver.CustomerIdResolver;
import br.com.tlf.api.rest.shared.ResponseDTO;
import br.com.tlf.core.port.in.CreateConsentPort;
import br.com.tlf.core.port.in.GetPendingTermsPort;
import br.com.tlf.core.port.in.command.PendingTermsQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(UrlConstant.CREDIT_CORE_URL_BASE)
public class CreditCoreController {

    private final GetPendingTermsPort getPendingTermsPort;
    private final CreateConsentPort createConsentPort;
    private final CustomerIdResolver customerIdResolver;
    private final CreditCoreApiMapper apiMapper;

    @GetMapping(UrlConstant.TERMS_URI)
    @ResponseStatus(HttpStatus.OK)
    public ResponseDTO<ActiveConsentResponseDTO> getActiveConsents(
            @RequestHeader String authorization,
            @RequestParam(required = false) String product,
            @RequestHeader("x-channel-id") String channelId,
            @RequestHeader("x-correlation-id") String correlationId,
            @RequestHeader("x-customer-id") String customerId) {

        PendingTermsQuery query = new PendingTermsQuery(
                customerIdResolver.resolve(authorization, customerId), product, correlationId, channelId);

        return ResponseDTO.ok(apiMapper.toResponse(getPendingTermsPort.execute(query)),
                "Active consents retrieved successfully");
    }

    @PostMapping(UrlConstant.CONSENTS_URI)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDTO<ConsentResponseDTO> createConsent(
            @RequestHeader String authorization,
            @Valid @RequestBody ConsentRequestDTO request,
            @RequestHeader("x-channel-id") String channelId,
            @RequestHeader("x-correlation-id") String correlationId,
            @RequestHeader("x-customer-id") String customerId) {

        var command = apiMapper.toCommand(request,
                customerIdResolver.resolve(authorization, customerId), correlationId, channelId);

        return ResponseDTO.success(apiMapper.toResponse(createConsentPort.execute(command)),
                "Consent options registered successfully.");
    }
}
