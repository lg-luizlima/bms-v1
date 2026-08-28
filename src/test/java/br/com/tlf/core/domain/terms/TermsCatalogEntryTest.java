package br.com.tlf.core.domain.terms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TermsCatalogEntryTest {

    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    private TermsCatalogEntry term(String version, Instant startAt, Instant endAt) {
        return TermsCatalogEntry.builder().version(version).startAt(startAt).endAt(endAt).build();
    }

    @ParameterizedTest
    @CsvSource({
            "2.0, 1.0, true",
            "1.0, 2.0, false",
            "1.0, 1.0, false",
            "1.10, 1.9, true",
            "1.0.1, 1.0, true",
            "v2, v1, true",
    })
    void isNewerThan_ordersVersionsSegmentBySegment(String left, String right, boolean expected) {
        assertThat(term(left, NOW, null).isNewerThan(term(right, NOW, null))).isEqualTo(expected);
    }

    @Test
    void unparsableVersionCountsAsZeroInsteadOfFailing() {
        assertThat(term("stable", NOW, null).isNewerThan(term("1.0", NOW, null))).isFalse();
        assertThat(term("1.0", NOW, null).isNewerThan(term("stable", NOW, null))).isTrue();
    }

    @Test
    void isVigentAt_requiresStartedAndNotFinished() {
        Instant yesterday = NOW.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = NOW.plus(1, ChronoUnit.DAYS);

        assertThat(term("1.0", yesterday, null).isVigentAt(NOW)).isTrue();
        assertThat(term("1.0", yesterday, tomorrow).isVigentAt(NOW)).isTrue();
        assertThat(term("1.0", tomorrow, null).isVigentAt(NOW)).isFalse();
        assertThat(term("1.0", yesterday, yesterday).isVigentAt(NOW)).isFalse();
        assertThat(term("1.0", null, null).isVigentAt(NOW)).isFalse();
    }

    @Test
    void expiryFrom_isNullWhenTheTermNeverExpires() {
        TermsCatalogEntry noValidity = TermsCatalogEntry.builder().validityDays(null).build();
        TermsCatalogEntry thirtyDays = TermsCatalogEntry.builder().validityDays(30).build();

        assertThat(noValidity.expiryFrom(NOW)).isNull();
        assertThat(thirtyDays.expiryFrom(NOW)).isEqualTo(NOW.plus(30, ChronoUnit.DAYS));
    }
}
