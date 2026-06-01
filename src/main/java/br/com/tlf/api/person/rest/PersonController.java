package br.com.tlf.api.person.rest;

import br.com.tlf.api.person.rest.dto.PersonDTO;
import br.com.tlf.application.facade.PersonFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping("/person")
public class PersonController implements PersonAPI {
	
	@Autowired
	private PersonFacade personFacade;
	
	public ResponseEntity<PersonDTO> get(String id) {
		return ResponseEntity.ok(personFacade.addLinksHateoas(personFacade.searchById(id)));
	}

	public ResponseEntity<Collection<PersonDTO>> getByName(String firstName, String lastName) {
		return ResponseEntity.ok(Collections.singletonList(personFacade.addLinksHateoas(personFacade.searchByFullName(firstName, lastName))));
	}
	
	public ResponseEntity<String> create(PersonDTO person) {

		PersonDTO createdPerson = personFacade.create(person);
		
		var uri = fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(createdPerson.getId())
				.toUri();
		
		return ResponseEntity.created(uri).build();
	}
	
	public ResponseEntity<PersonDTO> update(PersonDTO person) {
		return ResponseEntity.ok(personFacade.addLinksHateoas(personFacade.update(person)));
	}
	
	public ResponseEntity<PersonDTO> delete(String id) {
		return ResponseEntity.ok(personFacade.delete(id));
	}
	
}
