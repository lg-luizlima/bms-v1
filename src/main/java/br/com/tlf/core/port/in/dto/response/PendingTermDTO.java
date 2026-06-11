package br.com.tlf.core.port.in.dto.response;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("termId")
    private String termId;

    @JsonProperty("termCode")
    private String termCode;

    @JsonProperty("contentSummary")
    private String contentSummary;

    @JsonProperty("contentUrl")
    private String contentUrl;

    @JsonProperty("isMandatory")
    private Boolean isMandatory;

}

