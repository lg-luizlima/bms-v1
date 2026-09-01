package br.com.tlf.api.rest.creditcore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Product the pending terms belong to", example = "CREDITO_PESSOAL",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("product")
    private String product;

    @Schema(description = "Whether there is at least one mandatory term still pending acceptance",
            example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("hasPendingMandatoryTerms")
    private Boolean hasPendingMandatoryTerms;

    @Schema(description = "Terms of the product not yet signed by the customer",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("pendingTerms")
    private List<PendingTermDTO> pendingTerms;
}

