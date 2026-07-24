package br.com.tlf.api.rest.creditcore;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.tlf.api.rest.UrlConstant;
import br.com.tlf.api.rest.assembler.CreditCoreAssembler;
import br.com.tlf.api.rest.shared.ResponseDTO;
import br.com.tlf.core.port.in.creditcore.CreditCorePortIn;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ConsentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(UrlConstant.CREDIT_CORE_URL_BASE)
public class CreditCoreController {

    private final CreditCorePortIn creditCoreService;
    private final CreditCoreAssembler assembler;

    @GetMapping(UrlConstant.TERMS_URI)
    @ResponseStatus(HttpStatus.OK)
    public ResponseDTO getActiveConsents(@RequestHeader String authorization,
            @RequestParam String product) {
        return assembler.toResponseDTO(creditCoreService.getPendingTerms(authorization, product), "ok",
                "Active consents retrieved successfully");

    }

    @PostMapping(UrlConstant.CONSENTS_URI)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseDTO createConsent(@RequestHeader String authorization, @RequestBody ConsentRequestDTO request) {
        ConsentResponseDTO consentResponse = creditCoreService.createConsent(authorization, request);
        return assembler.toResponseDTO(consentResponse, "success", "Consent options registered successfully.");
    }
}
