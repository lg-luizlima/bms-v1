package br.com.tlf.shared.constants;

public class ApplicationConstants {

    private ApplicationConstants() {}

    public static final String APPLICATION_NAME = "CREDIT_CORE_CONSENT_BMS";
    public static final String CLASS_METHOD_MESSAGE_PATTERN = "Class {}, method {}";
    public static final Long MAX_VALIDITY_DAYS = 30L;
    public static final String AGGREGATE_TYPE = "ConsentRegisteredEvent";
    public static final String TOPIC_NAME = "vivopay.credit.consent.events.v1";
}
