package br.com.tlf.api.rest.assembler;

import br.com.tlf.api.rest.dto.response.ResponseDTO;
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