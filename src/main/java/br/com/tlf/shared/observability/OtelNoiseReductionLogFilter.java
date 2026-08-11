package br.com.tlf.shared.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class OtelNoiseReductionLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if ("true".equals(event.getMDCPropertyMap().get(OtelLogSuppressionFilter.MDC_KEY))) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}
