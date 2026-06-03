package br.com.tlf.application.facade.creditcore;

import br.com.tlf.api.lending.rest.v1.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.api.lending.rest.v1.dto.request.consent.SignatureDTO;
import br.com.tlf.api.lending.rest.v1.dto.response.consent.ActiveConsentResponseDTO;
import br.com.tlf.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.MissingAuditDataException;
import br.com.tlf.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry;
import br.com.tlf.domain.service.customerconsent.CustomerConsentService;
import br.com.tlf.domain.service.termcatalog.TermCatalogService;
import br.com.tlf.domain.util.JwtTokenUtils;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static br.com.tlf.application.ApplicationConstants.CLASS_METHOD_MESSAGE_PATTERN;

@Component
@RequiredArgsConstructor
public class CreditCoreFacadeImpl implements CreditCoreFacade {


    private static final String FACADE_CONTEXT = "[Lending Core Facade]";

    private final CustomerConsentService customerConsentService;
    private final TermCatalogService termCatalogService;


    @Override
    public ActiveConsentResponseDTO getPendingTerms(String authorization, String product)  {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "getPendingTerms");
        LogUtils.log("{} Incoming get pending terms request for product: {}", FACADE_CONTEXT, product);

        LogUtils.log(" Extracting CPF from authorization token");
        String customerId = JwtTokenUtils.cpfToken(authorization);
        LogUtils.log(" CPF extracted successfully");

        LogUtils.log(" Fetching active terms for product:");
        List<TermsCatalogVO> termsCatalog = termCatalogService.findActiveTerms(product);
        LogUtils.log("Found {} active terms for product: {}", termsCatalog.size(), product);

        ActiveConsentResponseVO activeConsentResponseVO = customerConsentService.pendingTerms(termsCatalog, customerId);

        LogUtils.log(" Mapping active consent response VO to DTO");
        ActiveConsentResponseDTO activeConsentResponseDTO = CreditCoreMapper.INSTANCE.toActiveConsentResponseDTO(activeConsentResponseVO);
        LogUtils.log("activeConsentResponseDTO", activeConsentResponseDTO);
        return activeConsentResponseDTO;
    }

    @Override
    public void createConsent(String authorization, ConsentRequestDTO request) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "createConsent");
        LogUtils.log("Starting consent facade for product: {}", request.getProduct());

        LogUtils.log("Validating signature audit data");
        validateSignature(request.getSignature());
        LogUtils.log("Signature validation passed");

        LogUtils.log("Extracting CPF from authorization token");
        String cpf = JwtTokenUtils.cpfToken(authorization);
        LogUtils.log("CPF extracted successfully");

        LogUtils.log("Mapping request DTO to VO");
        ConsentRequestVO vo = CreditCoreMapper.INSTANCE.toVO(request, cpf);
        LogUtils.log("Request mapping completed, proceeding to service");

        LogUtils.log("Fetching active terms for product: {}", request.getProduct());
        List<TermsCatalogVO> activeTerms = termCatalogService.findActiveTerms(request.getProduct());
        LogUtils.log("Found {} active terms", activeTerms.size());

        try {
            customerConsentService.createConsent(activeTerms,vo);
            LogUtils.log("Consent facade processing completed successfully");
        } catch (Exception e) {
            LogUtils.error("Error in consent facade: {}", e.getMessage());
            throw e;
        }
    }

    private void validateSignature(SignatureDTO signature) {
        LogUtils.log("Validating signature fields: ip, deviceId, geolocation");
        List<ConsentErrorEntry> errors = new ArrayList<>();

        if (!StringUtils.hasText(signature.getIp())) {
            LogUtils.log("Signature validation failed: missing ip");
            errors.add(new ConsentErrorEntry("MISSING_AUDIT_DATA", "signature.ip", "O campo 'signature.ip' é obrigatório."));
        }
        if (!StringUtils.hasText(signature.getDeviceId())) {
            LogUtils.log("Signature validation failed: missing deviceId");
            errors.add(new ConsentErrorEntry("MISSING_AUDIT_DATA", "signature.deviceId", "O campo 'signature.deviceId' é obrigatório."));
        }
        if (signature.getGeolocation() == null) {
            LogUtils.log("Signature validation failed: missing geolocation");
            errors.add(new ConsentErrorEntry("MISSING_AUDIT_DATA", "signature.geolocation", "O campo 'signature.geolocation' é obrigatório."));
        }

        if (!errors.isEmpty()) {
            LogUtils.log("Signature validation failed with {} errors", errors.size());
            throw new MissingAuditDataException(errors);
        }
    }
}
