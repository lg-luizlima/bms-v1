package br.com.tlf.api.lending.rest.v1.openapi.controller;

import br.com.tlf.api.lending.rest.v1.dto.request.consent.ConsentRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.tlf.api.lending.rest.v1.UrlConstant;
import br.com.tlf.api.lending.rest.v1.dto.response.ResponseDTO;
import br.com.tlf.api.lending.rest.v1.dto.response.contract.ContractListResponseDTO;
import br.com.tlf.configuration.common.rest.OpenAPIResponseCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Credit Operations")
public interface CreditCoreControllerOpenApi {

    @Operation(
            summary = "List consents for the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = OpenAPIResponseCodes.OK_STATUS,
                            description = "Consents retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ContractListResponseDTO.class))
                    ),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.UNAUTHORIZED_STATUS, description = "Unauthorized"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.NOT_FOUND_STATUS, description = "Not Found"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.INTERNAL_SERVER_ERROR_STATUS, description = "Internal Server Error"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.SERVICE_UNAVAILABLE_STATUS, description = "Service Unavailable")
            }
    )
    @GetMapping(UrlConstant.TERMS_URI)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO getActiveConsents(@RequestHeader("Authorization") String authorization,
                            @RequestParam String product);


    @Operation(
            summary = "Register user consent for a credit term",
            responses = {
                    @ApiResponse(
                            responseCode = OpenAPIResponseCodes.CREATED_STATUS,
                            description = "Consent registered successfully"
                    ),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.BAD_REQUEST_STATUS, description = "Missing required audit data"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.UNAUTHORIZED_STATUS, description = "Unauthorized"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.UNPROCESSABLE_ENTITY_STATUS, description = "Invalid term or version"),
                    @ApiResponse(responseCode = OpenAPIResponseCodes.INTERNAL_SERVER_ERROR_STATUS, description = "Internal Server Error")
            }
    )
    @PostMapping(UrlConstant.CONSENTS_URI)
    @ResponseStatus(HttpStatus.CREATED)
    void createConsent(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ConsentRequestDTO request
    );
}
