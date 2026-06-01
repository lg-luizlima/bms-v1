package br.com.tlf.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.com.tlf.adcp.lib.exception.spring.domain.exception.BusinessException;
import br.com.tlf.adcp.lib.exception.spring.domain.exception.NotFoundException;
import br.com.tlf.domain.entity.PersonEntity;
import br.com.tlf.domain.vo.PersonVO;
import br.com.tlf.persistence.repository.PersonRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class PersonServiceImpl implements PersonService {

	private final PersonRepository repository;
	private final ModelMapper mapper;

	private String msgNotFound = "Person not found";
	
	@Override
	public PersonVO create(PersonVO person) {
		PersonEntity personToSave = mapper.map(person, PersonEntity.class);
		
		if (personToSave.isOfAge()) {
			personToSave.setId(UUID.randomUUID().toString());
			
			PersonEntity persistedEntity = repository.save(personToSave);
			return mapper.map(persistedEntity, PersonVO.class);
		} else {
			throw new BusinessException("Pessoa menor de idade.", HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}
	
	@Override
	public PersonVO delete(String id) {
		Optional<PersonEntity> person = repository.findById(id);
		
		if (!person.isPresent())
			throw new NotFoundException(msgNotFound, HttpStatus.NOT_FOUND);

		
		repository.delete(person.get());
		return mapper.map(person.get(), PersonVO.class);
	}

	@Override
	public PersonVO findById(String id) {
		
		Optional<PersonEntity> person = repository.findById(id);
		
		if (!person.isPresent())
			throw new NotFoundException(msgNotFound, HttpStatus.NOT_FOUND);

		
		return mapper.map(person.get(), PersonVO.class);
	}

	@Override
	public PersonVO findByFullName(String firstName, String lastName) {
		
		Optional<PersonEntity> person = repository.findByFirstNameAndLastName(firstName, lastName);
		
		if (!person.isPresent())
			throw new NotFoundException(msgNotFound, HttpStatus.NOT_FOUND);
		
		return mapper.map(person.get(), PersonVO.class);
	}

	@Override
	public PersonVO update(PersonVO person) {
		PersonEntity inputEntity = mapper.map(person, PersonEntity.class);
		Optional<PersonEntity> existingEntity = repository.findById(person.getId());
		
		if (!existingEntity.isPresent()) {
			return this.create(person);
		}
		
		if (!inputEntity.isOfAge())
			throw new BusinessException("Nova data de nascimento informada é inválida: é necessário ser maior de idade.", HttpStatus.BAD_REQUEST);


		repository.save(inputEntity);
		
		return person;
	}	
}
