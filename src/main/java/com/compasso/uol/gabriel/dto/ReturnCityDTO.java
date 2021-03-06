package com.compasso.uol.gabriel.dto;

import java.io.Serializable;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReturnCityDTO implements Serializable {
	private static final long serialVersionUID = -1887788586324751684L;

	@Size(min = 1, max = 70, message = "O campo 'Nome' deve conter entre 1 e 70 caracteres.")
	private String name;

	@Size(min = 1, max = 20, message = "O campo 'Estado' deve conter entre 1 e 20 caracteres.")
	private String state;

	@Override
	public String toString() {
		return "ReturnCityDTO [name=" + name + ", state=" + state + "]";
	}
}
