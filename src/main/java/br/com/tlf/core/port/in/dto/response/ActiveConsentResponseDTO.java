package br.com.tlf.core.port.in.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveConsentResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 203752103527580588L;

    @JsonProperty("product")
    private String product;

    @JsonProperty("hasPendingMandatoryTerms")
    private Boolean hasPendingMandatoryTerms;

    @JsonProperty("pendingTerms")
    private List<PendingTermDTO> pendingTerms;
}

