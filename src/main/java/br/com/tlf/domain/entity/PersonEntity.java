package br.com.tlf.domain.entity;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PERSON")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PersonEntity {

	@Transient
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private static final int IS_OF_AGE_VALUE = 18;
	
	@Id
	@Column(name = "ID")
	@EqualsAndHashCode.Include
	private String id;
	
	@Column(name = "FIRST_NAME")
	private String firstName;
	
	@Column(name = "LAST_NAME")
	private String lastName;
	
	@Column(name = "BIRTH_DATE")
	private Date birthDate;
	
	@Column(name = "GENDER")
	private String gender;
	
	/**
	 * Indicates if this PersonDTO is of age or not.
	 * @return true if the PersonDTO is of age or false if not.
	 */
	public boolean isOfAge() {
		if (this.birthDate == null) return false;
		
		var birth = this.birthDate
				.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
		
		int years = Period
				.between(birth, LocalDate.now(ZoneId.systemDefault()))
				.getYears();

		return years >= IS_OF_AGE_VALUE;

	}
	
}
