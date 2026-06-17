package br.com.tlf.shared.constants;

public class ApplicationConstants {

    private ApplicationConstants() {}

    public static final String APPLICATION_NAME = "CREDIT_CORE_CONSENT_BMS";
    public static final String CLASS_METHOD_MESSAGE_PATTERN = "Class {}, method {}";
    public static final int DEFAULT_CODECS_SIZE = 16 * 1024 * 1024;
    public static final Long MAX_VALIDITY_DAYS = 30L;
    public static final String AGGREGATE_TYPE = "ConsentRegisteredEvent";
    public static final String TOPIC_NAME = "vivopay.credit.onboarding.consent.registered.v1";
}
