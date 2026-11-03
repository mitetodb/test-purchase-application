package app.service;

import app.model.dto.CustomerDTO;
import app.model.entity.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<Customer> findAll();

    List<Customer> findAllForCurrentUser();

    List<Customer> findAllForCurrentUser(boolean includeDeactivated);

    Customer findById(UUID id);

    Customer findByIdForCurrentUser(UUID id);

    Customer create(CustomerDTO dto);

    Customer update(UUID id, CustomerDTO dto);

    void deactivateCustomer(UUID id);

    void reactivateCustomer(UUID id);
}
