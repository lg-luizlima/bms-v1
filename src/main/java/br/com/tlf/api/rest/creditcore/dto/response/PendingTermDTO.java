package br.com.tlf.api.rest.creditcore.dto.response;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTermDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7929416991190707732L;

    @Schema(description = "Term identifier (UUID)", example = "41dfaa2f-3fee-432f-96a0-ffee95cfb2be",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("termId")
    private String termId;

    @Schema(description = "Business code of the term", example = "PRIVACY_POLICY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("termCode")
    private String termCode;

    @Schema(description = "Human-readable title of the term", example = "Política de Privacidade",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("title")
    private String title;

    @Schema(description = "Short summary of the term's content", example = "Resumo dos termos de uso e "
            + "tratamento de dados pessoais.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("contentSummary")
    private String contentSummary;

    @Schema(description = "URL where the full term content can be read",
            example = "https://vivo.com.br/termos/privacidade", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("contentUrl")
    private String contentUrl;

    @Schema(description = "Whether accepting this term is mandatory to use the product", example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isMandatory")
    private Boolean isMandatory;

}

