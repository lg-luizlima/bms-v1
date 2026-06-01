package br.com.tlf.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.tlf.domain.entity.PersonEntity;

public interface PersonRepository extends JpaRepository<PersonEntity, String> {

	Optional<PersonEntity> findByFirstNameAndLastName(String firstName, String lastName);
	
}
