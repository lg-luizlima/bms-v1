package br.com.tlf.api.rest.config.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.core.domain.exception.DomainErrorCode;

/**
 * Like {@link ApiErrorResponse}, but for a {@link DomainErrorCode} whose example body can't be derived
 * from {@code DomainErrorRegistry} alone — e.g. a bean-validation error whose {@code errors[]} field
 * name is specific to one endpoint's DTO. {@code factory} builds the full {@link ProblemDetailResponse}
 * from the same building blocks the real handler uses ({@code DomainErrorRegistry}, illustrative
 * timestamp/traceId constants), so only the genuinely illustrative parts (which field, which message)
 * are hand-authored — no JSON text is. The matching {@code @ApiResponse(responseCode = ...)} must
 * already exist, same as {@link ApiErrorResponse}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiErrorExampleOverrides.class)
public @interface ApiErrorExampleOverride {

    DomainErrorCode code();

    Class<? extends Supplier<ProblemDetailResponse>> factory();
}
