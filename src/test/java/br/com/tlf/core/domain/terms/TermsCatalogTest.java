package br.com.tlf.core.domain.terms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TermsCatalogTest {

    private TermsCatalogEntry entry(String termCode, String version) {
        return TermsCatalogEntry.builder().id(UUID.randomUUID()).termCode(termCode).version(version).build();
    }

    @Test
    void latestVersionPerTermCode_keepsOnlyTheHighestVersionOfEachCode() {
        TermsCatalogEntry oldHard = entry("HARD", "1.0");
        TermsCatalogEntry newHard = entry("HARD", "2.0");
        TermsCatalogEntry soft = entry("SOFT", "1.0");

        TermsCatalog latest = TermsCatalog.of(List.of(oldHard, newHard, soft)).latestVersionPerTermCode();

        assertThat(latest.entries()).containsExactly(newHard, soft);
    }

    @Test
    void latestVersionPerTermCode_isStableWhenVersionsTie() {
        TermsCatalogEntry first = entry("HARD", "1.0");
        TermsCatalogEntry second = entry("HARD", "1.0");

        assertThat(TermsCatalog.of(List.of(first, second)).latestVersionPerTermCode().entries())
                .containsExactly(first);
    }

    @Test
    void emptyCatalogReportsEmpty() {
        assertThat(TermsCatalog.of(List.of()).isEmpty()).isTrue();
    }
}
