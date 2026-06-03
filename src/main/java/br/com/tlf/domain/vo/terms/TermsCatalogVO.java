package br.com.tlf.domain.vo.terms;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsCatalogVO {

    private UUID id;
    private String product;
    private String termCode;
    private String version;
    private Boolean isMandatory;
    private Integer validityDays;
    private Boolean revokePreviousVersions;
    private String contentType;
    private String contentSummary;
    private String contentText;
    private String contentUrl;
    private Instant startAt;
    private Instant endAt;
}
