package br.com.tlf.api.rest.controller.creditcore;

import br.com.tlf.core.application.service.creditcore.CreditCoreServiceImpl;
import br.com.tlf.api.rest.dto.request.consent.ConsentRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.tlf.api.rest.UrlConstant;
import br.com.tlf.api.rest.assembler.Assembler;
import br.com.tlf.api.rest.dto.response.ResponseDTO;
import br.com.tlf.api.rest.dto.response.consent.ActiveConsentResponseDTO;
import lombok.RequiredArgsConstructor;


@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(UrlConstant.CREDIT_CORE_URL_BASE)
public class CreditCoreController  {

    private static final String CONTROLLER_CONTEXT = "[Lending Core Controller]";

    private final CreditCoreServiceImpl creditCoreService;
    private final Assembler assembler;

    @GetMapping(UrlConstant.TERMS_URI)
    @ResponseStatus(HttpStatus.OK)
    public ResponseDTO getActiveConsents(@RequestHeader String authorization,
                                                                           @RequestParam String product) {
        log.info("{} Incoming get active consents request for product: {}", CONTROLLER_CONTEXT, product);

            ActiveConsentResponseDTO response = creditCoreService.getPendingTerms(authorization, product);
            log.info("{} Active consents retrieved successfully", CONTROLLER_CONTEXT);
            return assembler.toResponseDTO(response, "ok", "Active consents retrieved successfully");

    }

    @PostMapping(UrlConstant.CONSENTS_URI)
    @ResponseStatus(HttpStatus.CREATED)
    public void createConsent(String authorization, ConsentRequestDTO request) {
        log.info(CONTROLLER_CONTEXT + " Incoming consent request for product: {}", request.getProduct());
        log.info(CONTROLLER_CONTEXT + " Number of accepted terms: {}", request.getAcceptedTerms().size());

            creditCoreService.createConsent(authorization, request);
            log.info(CONTROLLER_CONTEXT + " Consent request processed successfully");
    }
}
