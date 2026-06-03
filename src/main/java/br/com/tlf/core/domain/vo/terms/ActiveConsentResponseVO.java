package br.com.tlf.core.domain.vo.terms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveConsentResponseVO {

    private String product;
    private Boolean hasPendingMandatoryTerms;
    private List<PendingTermVO> pendingTerms;
}