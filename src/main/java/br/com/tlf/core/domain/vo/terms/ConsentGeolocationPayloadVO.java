package br.com.tlf.core.domain.vo.terms;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentGeolocationPayloadVO {

    private String lat;

    @JsonProperty("long")
    private String lon;
}
