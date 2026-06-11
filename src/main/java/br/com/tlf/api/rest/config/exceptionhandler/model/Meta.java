package br.com.tlf.api.rest.config.exceptionhandler.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Meta {
    private Integer totalRecords;
    private Integer totalPages;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
    private LocalDateTime requestDateTime;
}
