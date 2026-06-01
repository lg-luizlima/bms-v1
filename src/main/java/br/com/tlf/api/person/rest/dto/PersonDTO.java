package br.com.tlf.api.person.rest.dto;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"_links"})
public class PersonDTO extends RepresentationModel<PersonDTO> {
	
	private String id;
	
	@NotBlank(message = "First name cannot be null or empty")
	private String firstName;
	
	@NotBlank(message = "Last name cannot be null or empty")
	private String lastName;
	
	@NotNull(message = "Birth date cannot be null")
	@JsonFormat(pattern = "dd/MM/yyyy", timezone = "America/Sao_Paulo")
	private Date birthDate;
	
	@NotNull(message = "Gender cannot be null")
	private Gender gender;
	
}
