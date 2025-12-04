package app.service;

import app.config.SecurityUtils;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<Customer> findAll();

    List<Customer> findAllForCurrentUser();

    Customer findById(UUID id);

    Customer findByIdForCurrentUser(UUID id);

    Customer create(CustomerDTO dto);

    Customer update(UUID id, CustomerDTO dto);

    void delete(UUID id);
}
