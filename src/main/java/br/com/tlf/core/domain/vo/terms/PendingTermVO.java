package br.com.tlf.core.domain.vo.terms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTermVO {

    private String termId;
    private String termCode;
    private String title;
    private String contentSummary;
    private String contentUrl;
    private Boolean isMandatory;
}
