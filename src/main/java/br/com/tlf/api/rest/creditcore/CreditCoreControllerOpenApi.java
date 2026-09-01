package br.com.tlf.api.rest.creditcore;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.tlf.api.rest.UrlConstant;
import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.api.rest.config.openapi.ApiErrorExampleOverride;
import br.com.tlf.api.rest.config.openapi.ApiErrorResponse;
import br.com.tlf.api.rest.config.openapi.ApiSuccessExample;
import br.com.tlf.api.rest.creditcore.doc.BadRequestValidationExample;
import br.com.tlf.api.rest.creditcore.doc.CreateConsentSuccessExample;
import br.com.tlf.api.rest.creditcore.doc.GetActiveConsentsAllProductsExample;
import br.com.tlf.api.rest.creditcore.doc.GetActiveConsentsMandatoryPendingTermExample;
import br.com.tlf.api.rest.creditcore.doc.GetActiveConsentsNoPendingTermsExample;
import br.com.tlf.api.rest.creditcore.doc.GetActiveConsentsOptionalPendingTermExample;
import br.com.tlf.api.rest.creditcore.dto.request.ConsentRequestDTO;
import br.com.tlf.api.rest.creditcore.dto.response.ConsentResponseDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;
import br.com.tlf.core.domain.exception.DomainErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Credit Core",
    description = "Consulta de termos pendentes e registro de consentimento do cliente sobre os termos de um produto.")
public interface CreditCoreControllerOpenApi {

    String PROBLEM_DETAIL_MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;
    String GET_TERMS_SUCCESS_MESSAGE = "Active consents retrieved successfully";
    String CREATE_CONSENT_SUCCESS_MESSAGE = "Consent options registered successfully.";

    String AUTHORIZATION_HEADER_DESCRIPTION = "Bearer token do cliente autenticado";
    String CHANNEL_ID_HEADER_DESCRIPTION = "Identificador do canal de origem da requisição";
    String CORRELATION_ID_HEADER_DESCRIPTION = "Identificador de correlação da requisição, para rastreabilidade e idempotência de curta janela";
    String CUSTOMER_ID_HEADER_DESCRIPTION = "CPF do cliente (sobrepõe o CPF extraído do token, quando presente)";
    String CHANNEL_ID_EXAMPLE = "APP";
    String CORRELATION_ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    String CUSTOMER_ID_EXAMPLE = "52998224725";
    String UNEXPECTED_ERROR_DESCRIPTION = "Erro interno inesperado";


    @Operation(summary = "Retorna os termos pendentes de um produto",
        description = "Lista, para o cliente identificado, os termos vigentes de um produto que ainda não "
            + "foram aceitos (ou cujo aceite anterior foi invalidado por uma nova versão do termo). Se o "
            + "parâmetro 'product' não for informado, retorna em 'data' uma lista com um item por produto que "
            + "possua ao menos um termo pendente (produtos sem termos pendentes não aparecem na lista); se não "
            + "houver nenhum termo vigente em nenhum produto, retorna uma lista vazia. Quando 'product' é "
            + "informado, 'data' continua sendo um único objeto (mesmo formato de hoje), inclusive quando não "
            + "há termos pendentes (lista de termos vazia).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Termos pendentes retornados com sucesso"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class))),
        @ApiResponse(responseCode = "404", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class))),
        @ApiResponse(responseCode = "500", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class)))
    })
    @ApiSuccessExample(name = "NO_PENDING_TERMS", factory = GetActiveConsentsNoPendingTermsExample.class)
    @ApiSuccessExample(name = "OPTIONAL_PENDING_TERM", factory = GetActiveConsentsOptionalPendingTermExample.class)
    @ApiSuccessExample(name = "MANDATORY_PENDING_TERM", factory = GetActiveConsentsMandatoryPendingTermExample.class)
    @ApiSuccessExample(name = "ALL_PRODUCTS_PENDING", factory = GetActiveConsentsAllProductsExample.class)
    @ApiErrorResponse(description = "Requisição inválida: identificação do cliente ausente ou CPF inválido",
        codes = {DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION, DomainErrorCode.INVALID_CPF_PARAMETER})
    @ApiErrorResponse(description = "Produto não encontrado", codes = DomainErrorCode.PRODUCT_NOT_FOUND)
    @ApiErrorResponse(description = UNEXPECTED_ERROR_DESCRIPTION, codes = DomainErrorCode.UNEXPECTED_ERROR)
    @GetMapping(UrlConstant.TERMS_URI)
    @ResponseStatus(HttpStatus.OK)

    ResponseDTO<Object> getActiveConsents(

        @Parameter(description = AUTHORIZATION_HEADER_DESCRIPTION, required = true)
        @RequestHeader String authorization,

        @Parameter(description = "Código do produto cujos termos serão consultados. Se omitido, retorna "
            + "todos os produtos do cliente com termos pendentes de assinatura.", example = "CREDITO_PESSOAL")
        @RequestParam(required = false) String product,

        @Parameter(description = CHANNEL_ID_HEADER_DESCRIPTION, required = true, example = CHANNEL_ID_EXAMPLE)
        @RequestHeader("x-channel-id") String channelId,

        @Parameter(description = CORRELATION_ID_HEADER_DESCRIPTION,
            required = true, example = CORRELATION_ID_EXAMPLE)
        @RequestHeader("x-correlation-id") String correlationId,

        @Parameter(description = CUSTOMER_ID_HEADER_DESCRIPTION,
            required = true, example = CUSTOMER_ID_EXAMPLE)
        @RequestHeader("x-customer-id") String customerId
    );



    @Operation(summary = "Registra o consentimento do cliente sobre termos de um produto",
        description = "Recebe a lista de termos aceitos/recusados pelo cliente e a assinatura de auditoria, "
            + "valida cada termo contra o catálogo vigente e persiste um consentimento por termo em uma "
            + "única transação. Cada consentimento registrado é publicado de forma assíncrona (outbox + "
            + "Debezium) para consumidores downstream.")

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Consentimento registrado com sucesso"),
        @ApiResponse(responseCode = "400", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class))),
        @ApiResponse(responseCode = "422", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class))),
        @ApiResponse(responseCode = "500", content = @Content(mediaType = PROBLEM_DETAIL_MEDIA_TYPE,
            schema = @Schema(implementation = ProblemDetailResponse.class)))
    })

    @ApiSuccessExample(name = "SUCCESS", factory = CreateConsentSuccessExample.class)
    @ApiErrorExampleOverride(code = DomainErrorCode.BAD_REQUEST, factory = BadRequestValidationExample.class)

    @ApiErrorResponse(description = "Requisição inválida: corpo malformado/campos obrigatórios ausentes, "
        + "identificação do cliente ausente ou CPF inválido",
        codes = {DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION, DomainErrorCode.INVALID_CPF_PARAMETER})

    @ApiErrorResponse(description = "Termo inválido, expirado, não encontrado ou termo obrigatório não aceito",
        codes = {DomainErrorCode.INVALID_TERM, DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED,
            DomainErrorCode.TERM_NOT_FOUND, DomainErrorCode.TERM_OUT_OF_VALIDITY})

    @ApiErrorResponse(description = UNEXPECTED_ERROR_DESCRIPTION, codes = DomainErrorCode.UNEXPECTED_ERROR)

    @PostMapping(UrlConstant.CONSENTS_URI)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<ConsentResponseDTO> createConsent(

        @Parameter(description = AUTHORIZATION_HEADER_DESCRIPTION, required = true)
        @RequestHeader String authorization,

        @Valid @RequestBody ConsentRequestDTO request,

        @Parameter(description = CHANNEL_ID_HEADER_DESCRIPTION, required = true, example = CHANNEL_ID_EXAMPLE)
        @RequestHeader("x-channel-id") String channelId,
        @Parameter(description = CORRELATION_ID_HEADER_DESCRIPTION , required = true, example = CORRELATION_ID_EXAMPLE)
        @RequestHeader("x-correlation-id") String correlationId,
        @Parameter(description = CUSTOMER_ID_HEADER_DESCRIPTION, required = true, example = CUSTOMER_ID_EXAMPLE)
        @RequestHeader("x-customer-id") String customerId
    );
}
