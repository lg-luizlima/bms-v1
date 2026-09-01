package br.com.tlf.api.rest.config.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.com.tlf.core.domain.exception.DomainErrorCode;

/**
 * Declares, in one place, everything about one HTTP error status an endpoint can return: the
 * human-readable description shown in Swagger and the {@link DomainErrorCode}s that map to it.
 * {@code ApiErrorResponseCustomizer} reads these at OpenAPI-doc-generation time, sets the description
 * on the matching {@code @ApiResponse(responseCode = ...)} (which must already exist, for schema
 * linkage) and generates one example per code from {@code DomainErrorRegistry} — no JSON or
 * description text is duplicated between the two.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiErrorResponses.class)
public @interface ApiErrorResponse {

    String description();

    DomainErrorCode[] codes();
}
