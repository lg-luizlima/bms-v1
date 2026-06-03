package br.com.tlf.api.lending.rest.v1.controller.creditcore;

import br.com.tlf.api.lending.rest.v1.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.application.facade.creditcore.CreditCoreFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.tlf.api.lending.rest.v1.UrlConstant;
import br.com.tlf.api.lending.rest.v1.assembler.Assembler;
import br.com.tlf.api.lending.rest.v1.dto.response.ResponseDTO;
import br.com.tlf.api.lending.rest.v1.dto.response.consent.ActiveConsentResponseDTO;
import br.com.tlf.api.lending.rest.v1.openapi.controller.CreditCoreControllerOpenApi;
import br.com.tlf.domain.util.LogUtils;
import lombok.RequiredArgsConstructor;

import static br.com.tlf.application.ApplicationConstants.CLASS_METHOD_MESSAGE_PATTERN;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(UrlConstant.CREDIT_CORE_URL_BASE)
public class CreditCoreController implements CreditCoreControllerOpenApi {

    private static final String CONTROLLER_CONTEXT = "[Lending Core Controller]";

    private final CreditCoreFacade creditCoreFacade;
    private final Assembler assembler;

    @Override
    public ResponseDTO getActiveConsents(@RequestHeader String authorization,
                                         @RequestParam String product) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "getActiveConsents");
        LogUtils.log("{} Incoming get active consents request for product: {}", CONTROLLER_CONTEXT, product);

        try {
            ActiveConsentResponseDTO response = creditCoreFacade.getPendingTerms(authorization, product);
            LogUtils.log("{} Active consents retrieved successfully", CONTROLLER_CONTEXT);
            return assembler.toResponseDTO(response, "ok", "Active consents retrieved successfully");
        } catch (Exception e) {
            LogUtils.error("{} Error retrieving active consents: {}", CONTROLLER_CONTEXT, e.getMessage());
            throw e;
        }
    }

    @Override
    public void createConsent(String authorization, ConsentRequestDTO request) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "createConsent");
        LogUtils.log(CONTROLLER_CONTEXT + " Incoming consent request for product: {}", request.getProduct());
        LogUtils.log(CONTROLLER_CONTEXT + " Number of accepted terms: {}", request.getAcceptedTerms().size());

        try {
            creditCoreFacade.createConsent(authorization, request);
            LogUtils.log(CONTROLLER_CONTEXT + " Consent request processed successfully");
        } catch (Exception e) {
            LogUtils.error(CONTROLLER_CONTEXT + " Error processing consent request: {}", e.getMessage());
            throw e;
        }
    }


}
