package br.com.tlf.domain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.Objects;

public class LogUtils {

    private LogUtils() { }

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    private static final String ANSI_RESET  = "[0m";
    private static final String ANSI_VIOLET = "[38;2;238;130;238m"; // Request
    private static final String ANSI_GREEN  = "[32m";               // Response
    private static final String ANSI_RED    = "[38;2;255;150;150m"; // Error
    private static final String ANSI_YELLOW = "[33m";               // Info

    private static final String LOG_LEVEL_INFO = "info";
    private static final String LOG_LEVEL_SL4J;

    static {
        String logLevelSl4J = System.getenv("LOG_LEVEL_SL4J");
        LOG_LEVEL_SL4J = Objects.isNull(logLevelSl4J) ? "debug" : logLevelSl4J.toLowerCase();
    }

    public static void log(String msg) {
        String formatted = formatMessage(msg);
        if (LOG_LEVEL_SL4J.equals(LOG_LEVEL_INFO)) {
            LOGGER.info(formatted);
        } else {
            LOGGER.debug(formatted);
        }
    }

    public static void log(String format, Object... arguments) {
        String slf4jFormatted = MessageFormatter.arrayFormat(format, arguments).getMessage();
        log(slf4jFormatted);
    }

    public static void warn(String format, Object... arguments) {
        String slf4jFormatted = MessageFormatter.arrayFormat(format, arguments).getMessage();
        LOGGER.warn("{}{}{}", ANSI_YELLOW + "⚠️ ", slf4jFormatted, ANSI_RESET);
    }

    public static void error(String format, Object... arguments) {
        String slf4jFormatted = MessageFormatter.arrayFormat(format, arguments).getMessage();
        LOGGER.error("{}{}{}", ANSI_RED + "❌ ", slf4jFormatted, ANSI_RESET);
    }

    private static String formatMessage(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("requestdto")) {
            return ANSI_VIOLET + "➡️  " + message + ANSI_RESET;
        } else if (lower.contains("responsedto")) {
            return ANSI_GREEN + "⬅️  " + message + ANSI_RESET;
        } else if (lower.contains("error") || lower.contains("exception")) {
            return ANSI_RED + "❌  " + message + ANSI_RESET;
        } else if (lower.contains("nrdocumento") || lower.contains("accountid")) {
            return ANSI_YELLOW + "➡️  " + message + ANSI_RESET;
        } else {
            return message;
        }
    }

}
