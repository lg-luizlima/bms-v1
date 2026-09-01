package br.com.tlf.api.rest.config.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import br.com.tlf.api.rest.shared.ResponseDTO;

/**
 * Points at a {@link Supplier} that builds a full {@link ResponseDTO} envelope example (data + status +
 * message) for one named scenario of an endpoint's 2xx response. {@code ApiSuccessExampleCustomizer}
 * instantiates it at OpenAPI-doc-generation time. The factory should call the same
 * {@code ResponseDTO.ok(...)}/{@code .success(...)} factory and the same message constant the real
 * controller method uses, so the generated example can never textually diverge from a real response —
 * only the sample {@code data} is illustrative. Repeatable so one operation can show several realistic
 * outcomes (e.g. empty list vs. optional pending term vs. mandatory pending term), each under its own
 * {@code name}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiSuccessExamples.class)
public @interface ApiSuccessExample {

    String name();

    Class<? extends Supplier<? extends ResponseDTO<?>>> factory();
}
