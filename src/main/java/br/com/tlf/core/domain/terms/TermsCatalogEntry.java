package br.com.tlf.core.domain.terms;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.Builder;

@Builder(toBuilder = true)
public record TermsCatalogEntry(
        UUID id,
        String termCode,
        String title,
        String version,
        Boolean isMandatory,
        Integer validityDays,
        Boolean revokePreviousVersions,
        String contentType,
        String contentSummary,
        String contentText,
        String contentUrl,
        Instant startAt,
        Instant endAt,
        Boolean requiresPostProcessing,
        String templateId) {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");
    private static final Pattern VERSION_SEPARATOR = Pattern.compile("\\.");

    public boolean isVigentAt(Instant now) {
        return startAt != null
                && now.isAfter(startAt)
                && (endAt == null || now.isBefore(endAt));
    }

    public boolean isMandatoryTerm() {
        return Boolean.TRUE.equals(isMandatory);
    }

    public boolean revokesPreviousVersions() {
        return Boolean.TRUE.equals(revokePreviousVersions);
    }

    public boolean requiresPostProcessingStep() {
        return Boolean.TRUE.equals(requiresPostProcessing);
    }

    public Instant expiryFrom(Instant acceptedAt) {
        return validityDays == null ? null : acceptedAt.plus(validityDays, ChronoUnit.DAYS);
    }

    public boolean isNewerThan(TermsCatalogEntry other) {
        return compareVersions(version, other.version()) > 0;
    }


    static int compareVersions(String left, String right) {
        List<Long> leftSegments = numericSegments(left);
        List<Long> rightSegments = numericSegments(right);

        for (int i = 0; i < Math.max(leftSegments.size(), rightSegments.size()); i++) {
            int comparison = Long.compare(segmentAt(leftSegments, i), segmentAt(rightSegments, i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static List<Long> numericSegments(String version) {
        if (version == null || version.isBlank()) {
            return List.of();
        }
        return Arrays.stream(VERSION_SEPARATOR.split(version))
                .map(TermsCatalogEntry::toSegment)
                .toList();
    }

    private static long toSegment(String rawSegment) {
        String digits = NON_DIGITS.matcher(rawSegment).replaceAll("");
        try {
            return digits.isEmpty() ? 0L : Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static long segmentAt(List<Long> segments, int index) {
        return index < segments.size() ? segments.get(index) : 0L;
    }
}
