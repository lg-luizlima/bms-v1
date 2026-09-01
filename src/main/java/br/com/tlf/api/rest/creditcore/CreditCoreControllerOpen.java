package br.com.tlf.api.rest.creditcore;

import org.springframework.web.bind.annotation.RequestMapping;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(UrlConstant.CREDIT_CORE_URL_BASE)
public class CreditCoreControllerOpen implements CreditCoreControllerOpenApi {

    private final GetPendingTermsPort getPendingTermsPort;
    private final CreateConsentPort createConsentPort;
    private final CustomerIdResolver customerIdResolver;
    private final CreditCoreApiMapper apiMapper;

    @Override
    public ResponseDTO<ActiveConsentResponseDTO> getActiveConsents(String authorization, String product,
            String channelId, String correlationId, String customerId) {

        PendingTermsQuery query = new PendingTermsQuery(
                customerIdResolver.resolve(authorization, customerId), product, correlationId, channelId);

        return ResponseDTO.ok(apiMapper.toResponse(getPendingTermsPort.execute(query)),
                GET_TERMS_SUCCESS_MESSAGE);
    }

    @Override
    public ResponseDTO<ConsentResponseDTO> createConsent(String authorization, ConsentRequestDTO request,
            String channelId, String correlationId, String customerId) {

        var command = apiMapper.toCommand(request,
                customerIdResolver.resolve(authorization, customerId), correlationId, channelId);

        return ResponseDTO.success(apiMapper.toResponse(createConsentPort.execute(command)),
                CREATE_CONSENT_SUCCESS_MESSAGE);
    }
}
