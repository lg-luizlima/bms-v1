package br.com.tlf.application.facade;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.tlf.api.person.rest.PersonController;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import br.com.tlf.api.person.rest.dto.PersonDTO;
import br.com.tlf.domain.service.PersonService;
import br.com.tlf.domain.vo.PersonVO;

@Component
class PersonFacadeImpl implements PersonFacade {

	Logger logger = LoggerFactory.getLogger(PersonFacadeImpl.class);
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private ModelMapper mapper;
	
	@Override
	public PersonDTO addLinksHateoas(PersonDTO person) {
		person.add(linkTo(PersonController.class).slash(person.getId()).withSelfRel());
		return person;
	}

	@Override
	public PersonDTO create(PersonDTO person) {
		PersonVO createdPerson = personService.create(mapper.map(person, PersonVO.class));		
		return mapper.map(createdPerson, PersonDTO.class);
	}

	@Override
	public PersonDTO delete(String id) {
		PersonVO deletedPerson = personService.delete(id);
		return mapper.map(deletedPerson, PersonDTO.class);
	}

	@Override
	public PersonDTO searchById(String id) {
		PersonVO foundPerson = personService.findById(id);
		return mapper.map(foundPerson, PersonDTO.class);
	}

	@Override
	public PersonDTO searchByFullName(String firstName, String lastName) {
		PersonVO foundPerson = personService.findByFullName(firstName, lastName);
		return mapper.map(foundPerson, PersonDTO.class);
	}

	@Override
	public PersonDTO update(PersonDTO person) {
		PersonVO updatedPerson = personService.update(mapper.map(person, PersonVO.class));
		return mapper.map(updatedPerson, PersonDTO.class);
	}
	
}
