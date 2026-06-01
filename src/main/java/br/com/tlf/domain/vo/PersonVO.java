package br.com.tlf.domain.vo;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonVO {
	
	private String id;
	private String firstName;
	private String lastName;
	private Date birthDate;
	private String gender;
	
}
