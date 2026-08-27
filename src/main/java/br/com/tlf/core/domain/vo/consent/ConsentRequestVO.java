package br.com.tlf.core.domain.vo.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestVO {

    private String customerId;
    private List<AcceptedTermVO> acceptedTerms;
    private SignatureVO signature;
}
