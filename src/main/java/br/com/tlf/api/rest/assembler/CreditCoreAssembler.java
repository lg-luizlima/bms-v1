package br.com.tlf.api.rest.assembler;

import org.springframework.stereotype.Component;

import br.com.tlf.api.rest.shared.ResponseDTO;

@Component
public class CreditCoreAssembler {

    public ResponseDTO toResponseDTO(Object data, String status, String message) {

        return ResponseDTO.builder()
                .data(data)
                .status(status)
                .message(message)
                .build();
    }
}