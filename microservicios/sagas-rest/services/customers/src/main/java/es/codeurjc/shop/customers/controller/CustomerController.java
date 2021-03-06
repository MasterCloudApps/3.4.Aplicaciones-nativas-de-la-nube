package es.codeurjc.shop.customers.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc.shop.customers.dto.NotificationDto;
import es.codeurjc.shop.customers.dto.OperationEnum;
import es.codeurjc.shop.customers.entity.Customer;
import es.codeurjc.shop.customers.exception.ResourceNotFoundException;
import es.codeurjc.shop.customers.notifications.NotificationsMicroserviceClient;
import es.codeurjc.shop.customers.repository.CustomerRepository;

@RestController
class CustomerController {

	@Autowired
	private CustomerRepository repository;

	@Autowired
	private NotificationsMicroserviceClient notificationsMsClient;

	@GetMapping("/customers")
	List<Customer> getAll() {
		return repository.findAll();
	}

	@PostMapping("/customers")
	ResponseEntity<Customer> createCustomer(@RequestBody Customer newCustomer) {
		return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(newCustomer));
	}

	@GetMapping("/customers/{id}")
	ResponseEntity<Customer> getOne(@PathVariable Long id) {
		final Customer customer = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException());
		return ResponseEntity.ok(customer);
	}

	@PutMapping("/customers/{id}")
	ResponseEntity<Customer> updateCustomer(@RequestBody Customer updatedCustomer, @PathVariable Long id) {
		final Customer customer = repository.findById(id).map(c -> {
			c.setName(updatedCustomer.getName());
			c.setCredit(updatedCustomer.getCredit());
			return repository.save(c);
		}).orElseThrow(() -> new ResourceNotFoundException());
		return ResponseEntity.ok(customer);
	}

	@RequestMapping(value = "/customers/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> partialUpdateGeneric(@RequestBody Map<String, String> creditUpdate,
			@PathVariable("id") Long id) {
		try {
			final Customer customer = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException());
			final double creditQuantity = Double.valueOf(creditUpdate.get("amount"));
			final OperationEnum operation = OperationEnum.valueOf(creditUpdate.get("operation"));
			if (operation == OperationEnum.ADD) {
				customer.addCredit(creditQuantity);
				/*
				 * notify customer some credit was added; in the real world we would pass an
				 * email, not the customer name, but that data was not included in the model
				 */
				final NotificationDto notification = new NotificationDto(customer.getName(), creditQuantity);
				
				notificationsMsClient.sendNotification(notification);
				
			} else {
				customer.deductCredit(creditQuantity);
			}
			return ResponseEntity.ok(repository.save(customer));
		} catch (final IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("wrong operation");
		}
	}

	@DeleteMapping("/customers/{id}")
	void deleteCustomer(@PathVariable Long id) {
		repository.deleteById(id);
	}
}