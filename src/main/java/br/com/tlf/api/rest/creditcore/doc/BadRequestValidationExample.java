package br.com.tlf.api.rest.creditcore.doc;

import java.util.List;
import java.util.function.Supplier;

import br.com.tlf.api.rest.config.exceptionhandler.ApiExceptionHandler;
import br.com.tlf.api.rest.config.exceptionhandler.DomainErrorRegistry;
import br.com.tlf.api.rest.config.exceptionhandler.model.ErrorDetail;
import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.api.rest.config.openapi.ApiErrorResponseCustomizer;
import br.com.tlf.core.domain.exception.DomainErrorCode;


public class BadRequestValidationExample implements Supplier<ProblemDetailResponse> {

    @Override
    public ProblemDetailResponse get() {
        return ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.BAD_REQUEST.getCode())
                .message(DomainErrorRegistry.metadataFor(DomainErrorCode.BAD_REQUEST).title())
                .details(ApiExceptionHandler.MISSING_REQUIRED_FIELD_DETAILS)
                .timestamp(ApiErrorResponseCustomizer.EXAMPLE_TIMESTAMP)
                .traceId(ApiErrorResponseCustomizer.EXAMPLE_TRACE_ID)
                .errors(List.of(ErrorDetail.builder()
                        .field("signature.deviceId")
                        .message("must not be blank")
                        .build()))
                .build();
    }
}
