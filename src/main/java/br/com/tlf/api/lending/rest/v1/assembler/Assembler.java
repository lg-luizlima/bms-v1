package br.com.tlf.api.lending.rest.v1.assembler;

import br.com.tlf.api.lending.rest.v1.dto.response.ResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class Assembler {

    public ResponseDTO toResponseDTO(Object data, String status, String message) {

        return ResponseDTO.builder()
                .data(data)
                .status(status)
                .message(message)
                .build();
    }
}