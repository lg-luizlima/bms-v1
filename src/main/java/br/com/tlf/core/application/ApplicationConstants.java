package br.com.tlf.core.application;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApplicationConstants {

    public static final String CLASS_METHOD_MESSAGE_PATTERN = "Class {}, method {}";
    public static final int DEFAULT_CODECS_SIZE = 16 * 1024 * 1024;
    public static final Long MAX_VALIDITY_DAYS = 30L;
    public static final String AGGREGATE_TYPE = "CustomerConsent";
    public static final String TOPIC_NAME = "vivopay.credit.onboarding.consent.registered.v1";
}
