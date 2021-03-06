package com.compasso.uol.gabriel.controller;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.compasso.uol.gabriel.dto.RegisterClientDTO;
import com.compasso.uol.gabriel.dto.ReturnClientDTO;
import com.compasso.uol.gabriel.dto.UpdateClientDTO;
import com.compasso.uol.gabriel.entity.Authentication;
import com.compasso.uol.gabriel.entity.City;
import com.compasso.uol.gabriel.entity.Client;
import com.compasso.uol.gabriel.enumerator.message.AuthenticationMessage;
import com.compasso.uol.gabriel.enumerator.message.CityMessage;
import com.compasso.uol.gabriel.enumerator.message.ClientMessage;
import com.compasso.uol.gabriel.response.Response;
import com.compasso.uol.gabriel.service.AuthenticationService;
import com.compasso.uol.gabriel.service.CityService;
import com.compasso.uol.gabriel.service.ClientService;
import com.compasso.uol.gabriel.utils.Messages;

import lombok.NoArgsConstructor;

@RestController
@NoArgsConstructor
@RequestMapping("/client")
public class ClientController {
	private static final Logger log = LoggerFactory.getLogger(ClientController.class);

	@Autowired
	private ModelMapper mapper;

	@Autowired
	private CityService cityService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private AuthenticationService authenticationService;

	@GetMapping
	@Cacheable("client")
	public ResponseEntity<Response<List<ReturnClientDTO>>> findAll() throws NoSuchAlgorithmException {
		log.info("Buscando todas as cidades.");
		Response<List<ReturnClientDTO>> response = new Response<List<ReturnClientDTO>>();

		List<ReturnClientDTO> clients = clientService.findAll();
		response.setData(clients);

		return ResponseEntity.ok(response);
	}

	@Cacheable("client")
	@RequestMapping(params = "name", method = RequestMethod.GET)
	public ResponseEntity<Response<ReturnClientDTO>> findName(@RequestParam String name)
			throws NoSuchAlgorithmException {
		log.info("Buscando o cliente com o nome: {}", name);
		Response<ReturnClientDTO> response = new Response<ReturnClientDTO>();

		Optional<Client> clientOpt = clientService.findByName(name);
		if (!clientOpt.isPresent()) {
			log.error("Erro ao validar o nome: {}", name);
			response.addError(Messages.getClient(ClientMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		ReturnClientDTO client = mapper.map(clientOpt.get(), ReturnClientDTO.class);
		response.setData(client);

		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<Response<RegisterClientDTO>> include(@Valid @RequestBody RegisterClientDTO registerDTO,
			BindingResult result) throws NoSuchAlgorithmException {
		log.info("Incluindo o cliente: {}", registerDTO.toString());
		Response<RegisterClientDTO> response = new Response<RegisterClientDTO>();

		if (result.hasErrors()) {
			log.error("Erro validando dados para cadastro do cliente: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.addFieldError(error.getDefaultMessage()));

			return ResponseEntity.badRequest().body(response);
		}

		Optional<Authentication> authOpt = this.authenticationService
				.findByEmail(registerDTO.getAuthentication().getEmail());
		if (authOpt.isPresent()) {
			log.error("Erro ao validar o e-mail: {}", registerDTO.getAuthentication().getEmail());
			response.addError(Messages.getAuthentication(AuthenticationMessage.ALREADYEXISTSEMAIL.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		Optional<City> cityOpt = this.cityService.findById(registerDTO.getClient().getCity().getId());
		if (!cityOpt.isPresent()) {
			log.error("Erro ao validar o Id: {}", registerDTO.getClient().getCity().getId());
			response.addError(Messages.getCity(CityMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		City city = mapper.map(cityOpt.get(), City.class);
		Client client = mapper.map(registerDTO.getClient(), Client.class);
		Authentication auth = mapper.map(registerDTO.getAuthentication(), Authentication.class);

		String encodedPassword = new BCryptPasswordEncoder().encode(registerDTO.getAuthentication().getPassword());
		auth.setPassword(encodedPassword);

		client.setCity(city);
		auth.setClient(client);

		this.authenticationService.persist(auth);
		response.setData(registerDTO);
		return ResponseEntity.ok(response);
	}

	@PutMapping
	public ResponseEntity<Response<Client>> edit(@Valid @RequestBody UpdateClientDTO updateDTO, BindingResult result)
			throws NoSuchAlgorithmException {
		log.info("Editando o cliente: {}", updateDTO.getClient().toString());
		Response<Client> response = new Response<Client>();

		if (result.hasErrors()) {
			log.error("Erro validando dados para edição do cliente: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.addFieldError(error.getDefaultMessage()));

			return ResponseEntity.badRequest().body(response);
		}

		Optional<Authentication> authOpt = this.authenticationService.findById(updateDTO.getAuthentication().getId());
		if (!authOpt.isPresent()) {
			log.error("Erro ao validar o Id da autenticação: {}", updateDTO.getAuthentication().getId());
			response.addError(Messages.getAuthentication(AuthenticationMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		Optional<Client> clientOpt = this.clientService.findById(updateDTO.getClient().getId());
		if (!clientOpt.isPresent()) {
			log.error("Erro ao validar o Id do cliente: {}", updateDTO.getClient().getId());
			response.addError(Messages.getClient(ClientMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		Optional<City> cityOpt = this.cityService.findById(updateDTO.getClient().getCity().getId());
		if (!cityOpt.isPresent()) {
			log.error("Erro ao validar o Id da cidade: {}", updateDTO.getClient().getCity().getId());
			response.addError(Messages.getCity(CityMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		City city = mapper.map(cityOpt.get(), City.class);
		Authentication auth = mapper.map(authOpt.get(), Authentication.class);

		Client client = mapper.map(updateDTO.getClient(), Client.class);
		client.setCity(city);
		client.setId(clientOpt.get().getId());

		auth.setClient(client);
		this.authenticationService.persist(auth);
		client.getCity().setClients(null);

		response.setData(client);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping
	public ResponseEntity<Response<Client>> remove(@RequestParam("id") Long id) throws NoSuchAlgorithmException {
		log.info("Removendo o cliente: {}", id);
		Response<Client> response = new Response<Client>();

		Optional<Client> clientOpt = this.clientService.findById(id);
		if (!clientOpt.isPresent()) {
			log.info("O cliente não foi encontrado para o Id: {}", id);
			response.addError(Messages.getClient(ClientMessage.NONEXISTENT.toString()));

			return ResponseEntity.badRequest().body(response);
		}

		this.authenticationService.deleteById(clientOpt.get().getAuthentication().getId());
		this.clientService.deleteById(id);

		clientOpt.get().setCity(null);
		clientOpt.get().setAuthentication(null);

		response.setData(clientOpt.get());
		return ResponseEntity.ok(response);
	}
}
