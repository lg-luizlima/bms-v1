package br.com.tlf.api.person.rest;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.tlf.api.person.rest.dto.PersonDTO;
import br.com.tlf.configuration.common.rest.OpenAPIResponseCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = PersonAPI.PERSON_TAG, description = "REST API para gerenciamento cadastral de pessoas")
public interface PersonAPI {

	public static final String PERSON_TAG = "Person API";

	@Operation(summary = "Pesquisa uma pessoa pelo ID informado", tags = PersonAPI.PERSON_TAG)
	@ApiResponses(value = {
			@ApiResponse(responseCode = OpenAPIResponseCodes.OK_STATUS, description = "Se a pessoa foi encontrada na base de dados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PersonDTO.class))})
		   ,@ApiResponse(responseCode = OpenAPIResponseCodes.NOT_FOUND_STATUS, description = "Se nenhuma pessoa foi encontrada para o ID informado", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))})
	})
	@GetMapping("/{id}")
	public ResponseEntity<PersonDTO> get(@Parameter(description = "ID da pessoa a ser pesquisada") @PathVariable String id);

	@Operation(summary = "Pesquisa uma pessoa pelo primeiro e último nome", tags = PersonAPI.PERSON_TAG)
	@ApiResponses(value = {
			@ApiResponse(responseCode = OpenAPIResponseCodes.OK_STATUS, description = "Se a pessoa foi encontrada na base de dados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PersonDTO.class))})
		   ,@ApiResponse(responseCode = OpenAPIResponseCodes.NOT_FOUND_STATUS, description = "Se nenhuma pessoa foi encontrada para o ID informado", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))})
	})
	@GetMapping
	public ResponseEntity<Collection<PersonDTO>> getByName(
			@Parameter(description = "Primeiro nome da pessoa") @RequestParam String firstName,
			@Parameter(description = "Último nome da pessoa") @RequestParam String lastName);

	@Operation(summary = "Cria um novo registro da pessoa", tags = PersonAPI.PERSON_TAG)
	@ApiResponses(value = {
			@ApiResponse(responseCode = OpenAPIResponseCodes.CREATED_STATUS, description = "Se a pessoa puder ser criada com os dados informados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PersonDTO.class))})
		   ,@ApiResponse(responseCode = OpenAPIResponseCodes.UNPROCESSABLE_ENTITY_STATUS, description = "Se a pessoa não puder ser criada com os dados informados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))})
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> create(@Parameter(description = "Dados da pessoa a ser criada") @RequestBody @Valid PersonDTO person);

	@Operation(summary = "Atualiza o registro de uma pessoa", tags = PersonAPI.PERSON_TAG)
	@ApiResponses(value = {
			@ApiResponse(responseCode = OpenAPIResponseCodes.CREATED_STATUS, description = "Se a pessoa puder ser atualizada com os dados informados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PersonDTO.class))})
		   ,@ApiResponse(responseCode = OpenAPIResponseCodes.UNPROCESSABLE_ENTITY_STATUS, description = "Se a pessoa não puder ser atualizada com os dados informados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))})
	})
	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PersonDTO> update(@Parameter(description = "Dados da pessoa a ser atualizada") @RequestBody @Valid PersonDTO person);

	@Operation(summary = "Remove uma pessoa da base de dados", tags = PersonAPI.PERSON_TAG)
	@ApiResponses(value = {
			@ApiResponse(responseCode = OpenAPIResponseCodes.OK_STATUS, description = "Se a pessoa puder ser removida da base de dados", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PersonDTO.class))})
		   ,@ApiResponse(responseCode = OpenAPIResponseCodes.NOT_FOUND_STATUS, description = "Se a pessoa não existir", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))})
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<PersonDTO> delete(@Parameter(description = "The person's ID") @PathVariable("id") String id);

}