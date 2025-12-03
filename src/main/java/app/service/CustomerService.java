package app.service;

import app.config.SecurityUtils;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    public Customer create(CustomerDTO dto) {
        Customer customer = Customer.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .build();

        Long lastSeq = customerRepository.findLastSequence();
        long nextSeq = lastSeq + 1;

        String number = String.format("%04d", nextSeq);
        customer.setNumber(number);

        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return customerRepository.save(customer);
    }

    public Customer update(UUID id, CustomerDTO dto) {
        Customer customer = findById(id);

        customer.setName(dto.getName());
        customer.setCategory(dto.getCategory());
        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return customerRepository.save(customer);
    }

    public void delete(UUID id) {
        customerRepository.deleteById(id);
    }
}
