package br.com.tlf.domain.service;

import br.com.tlf.adcp.lib.exception.spring.domain.exception.BusinessException;
import br.com.tlf.domain.vo.PersonVO;

/**
 * Service that manages person operations to the domain.
 * @author Joyle Silva
 */
public interface PersonService {
	
	/**
	 * Creates a PersonDTO based on the values of the input VO.
	 * @param person The Value Object containing the PersonDTO data.
	 * @return The created PersonDTO.
	 */
	PersonVO create(PersonVO person);
	
	/**
	 * Deletes the PersonDTO
	 * @param id The PersonDTO's ID.
	 * @return The PersonDTO data that was deleted.
	 */
	PersonVO delete(String id);
	
	/**
	 * Find PersonDTO by ID
	 * @param id The PersonDTO's ID
	 * @return The PersonDTO that was found.
	 * @throws BusinessException If the PersonDTO was not found.
	 */
	PersonVO findById(String id);
	
	/**
	 * Find the person by the full name.
	 * @param firstName First name
	 * @param lastName Last name
	 * @return The person that was found.
	 */
	PersonVO findByFullName(String firstName, String lastName);
	
	/**
	 * Updates the PersonDTO
	 * @param person PersonDTO to be updated
	 * @return The PersonDTO that was updated.
	 */
	PersonVO update(PersonVO person);
	
}
