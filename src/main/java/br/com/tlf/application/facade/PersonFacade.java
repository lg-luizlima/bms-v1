package br.com.tlf.application.facade;

import br.com.tlf.api.person.rest.dto.PersonDTO;

/**
 * Facade that manages person operations between REST API and the domain
 * @author Joyle Silva
 */
public interface PersonFacade {
	
	PersonDTO addLinksHateoas(PersonDTO person);
	
	PersonDTO create(PersonDTO person);
	
	PersonDTO delete(String id);
	
	PersonDTO searchById(String id);
	
	PersonDTO searchByFullName(String firstName, String lastName);
	
	PersonDTO update(PersonDTO person);
	
}
