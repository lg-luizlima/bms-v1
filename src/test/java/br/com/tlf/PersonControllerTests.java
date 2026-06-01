package br.com.tlf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.api.person.rest.dto.Gender;
import br.com.tlf.api.person.rest.dto.PersonDTO;
import br.com.tlf.domain.entity.PersonEntity;
import br.com.tlf.persistence.repository.PersonRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PersonControllerTests {

    @MockitoBean
    private PersonRepository repository;
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetPersonThatNotExistsWithFirstAndLastNameMustReturnNotFound() throws Exception {
    	mockMvc.perform(get("/person")
    				   .queryParam("firstName", "Fulano")
    				   .queryParam("lastName", "de Tal")
    				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
    		   .andReturn();
    }
    
    @Test
    void testGetPersonThatExistsWithFirstAndLastNameMustReturnOk() throws Exception {
    	Optional<PersonEntity> personEntity = getPersonEntity();
    	when(repository.findByFirstNameAndLastName(anyString(), anyString())).thenReturn(personEntity);
    	
    	MvcResult mvcResult = mockMvc.perform(get("/person")
    				   .queryParam("firstName", "Fulano")
    				   .queryParam("lastName", "de Tal")
    				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.OK.value()))
    		   .andReturn();
    	
    	PersonDTO responsePerson = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), PersonDTO[].class)[0];
    	
    	assertEquals(personEntity.get().getId(), responsePerson.getId());
    	assertEquals(personEntity.get().getFirstName(), responsePerson.getFirstName());
    	assertEquals(personEntity.get().getLastName(), responsePerson.getLastName());
    	assertEquals(personEntity.get().getBirthDate(), responsePerson.getBirthDate());
    	assertEquals(personEntity.get().getGender(), responsePerson.getGender().toString());
    }

    @Test
    void testGetPersonThatNotExistsWithIDMustReturnNotFound() throws Exception {
    	mockMvc.perform(get("/person/{id}", UUID.randomUUID().toString()))
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
    		   .andReturn();
    }
    
    @Test
    void testGetPersonThatExistsWithIDMustReturnOk() throws Exception {
    	Optional<PersonEntity> personEntity = getPersonEntity();
    	
    	when(repository.findById(anyString())).thenReturn(personEntity);
    	
    	MvcResult mvcResult = mockMvc.perform(get("/person/{id}", UUID.randomUUID().toString()))
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.OK.value()))
    		   .andReturn();
    	
    	PersonDTO responsePerson = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), PersonDTO.class);
    	
    	assertEquals(personEntity.get().getId(), responsePerson.getId());
    	assertEquals(personEntity.get().getFirstName(), responsePerson.getFirstName());
    	assertEquals(personEntity.get().getLastName(), responsePerson.getLastName());
    	assertEquals(personEntity.get().getBirthDate(), responsePerson.getBirthDate());
    	assertEquals(personEntity.get().getGender(), responsePerson.getGender().toString());
    }    
    
    @Test
    void testCreatePersonThatIsNotOfAgeMustReturnUnprocessableEntity() throws Exception {
    	PersonDTO personToCreate = getPersonDTO();
    	personToCreate.setBirthDate(new Date());
    	
    	mockMvc.perform(post("/person")
    				   .content(new ObjectMapper().writeValueAsString(personToCreate))
    				   .contentType(MediaType.APPLICATION_JSON)
    				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
    		   .andReturn();
    }
    
    @Test
    void testCreatePersonThatIsOfAgeMustReturnCreated() throws Exception {
    	PersonEntity personEntity = getPersonEntity().get();
    	when(repository.save(any())).thenReturn(personEntity);
    	
    	PersonDTO personToCreate = getPersonDTO();
    	
    	MvcResult mvcResult = mockMvc.perform(post("/person")
    				   .content(new ObjectMapper().writeValueAsString(personToCreate))
    				   .contentType(MediaType.APPLICATION_JSON)
    				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.CREATED.value()))
    		   .andReturn();
    	
    	assertNotNull(mvcResult.getResponse().getHeader("Location"));
    	assertEquals(mvcResult.getResponse().getHeader("Location"), "http://localhost/person/" + personEntity.getId());
    }
    
    @Test
    void testDeletePersonThatNotExistsMustReturnNotFound() throws Exception {
    	mockMvc.perform(delete("/person/{id}", UUID.randomUUID().toString()))
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
    		   .andReturn();
    }
    
    @Test
    void testDeletePersonThatExistsMustReturnOk() throws Exception {
    	when(repository.findById(anyString())).thenReturn(getPersonEntity());
    	
    	mockMvc.perform(delete("/person/{id}", UUID.randomUUID().toString()))
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.OK.value()))
    		   .andReturn();
    }
    
    @Test
    void testUpdatePersonThatNotExistsMustReturnOk() throws Exception {
    	when(repository.save(any())).thenReturn(getPersonEntity().get());
    	PersonDTO personToUpdate = getPersonDTO();
    	
    	mockMvc.perform(put("/person")
				   .content(new ObjectMapper().writeValueAsString(personToUpdate))
				   .contentType(MediaType.APPLICATION_JSON)
				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.OK.value()))
    		   .andReturn();
    }
    
    @Test
    void testUpdatePersonThatExistsMustReturnOk() throws Exception {
    	when(repository.findById(anyString())).thenReturn(getPersonEntity());
    	when(repository.save(any())).thenReturn(getPersonEntity().get());
    	PersonDTO personToUpdate = getPersonDTO();
    	
    	mockMvc.perform(put("/person")
				   .content(new ObjectMapper().writeValueAsString(personToUpdate))
				   .contentType(MediaType.APPLICATION_JSON)
				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.OK.value()))
    		   .andReturn();
    }
    
    @Test
    void testUpdatePersonThatIsOfAgeMustReturnBadRequest() throws Exception {
    	when(repository.findById(anyString())).thenReturn(getPersonEntity());
    	
    	PersonDTO personToUpdate = getPersonDTO();
    	personToUpdate.setBirthDate(new Date());
    	
    	mockMvc.perform(put("/person")
				   .content(new ObjectMapper().writeValueAsString(personToUpdate))
				   .contentType(MediaType.APPLICATION_JSON)
				   )
    		   .andDo(print())
    		   .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
    		   .andReturn();
    }
    
    private Optional<PersonEntity> getPersonEntity() throws ParseException{
        PersonEntity personEntity = new PersonEntity();
        personEntity.setId("1");
        personEntity.setGender("MALE");
        personEntity.setFirstName("Fulano");
        personEntity.setLastName("de Tal");
        personEntity.setBirthDate(new SimpleDateFormat("dd-M-yyyy").parse("16-11-2000"));
        return Optional.of(personEntity);
    }
    
    private PersonDTO getPersonDTO () throws ParseException {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setId("1");
        personDTO.setGender(Gender.MALE);
        personDTO.setFirstName("fulano");
        personDTO.setLastName("de tal");
        personDTO.setBirthDate(new SimpleDateFormat("dd-M-yyyy").parse("16-11-2000"));
        return personDTO;
    }
}
