package br.com.tlf.api.rest.config.exceptionhandler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorEntry {

    private String code;
    private String field;
    private String message;

    public ErrorEntry(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
