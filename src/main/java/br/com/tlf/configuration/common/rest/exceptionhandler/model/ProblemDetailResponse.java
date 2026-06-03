package br.com.tlf.configuration.common.rest.exceptionhandler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailResponse {

    private Integer errorCode;
    private String message;
    private String details;
    private String timestamp;
    private String traceId;
    private List<ConsentErrorEntry> errors;
}
