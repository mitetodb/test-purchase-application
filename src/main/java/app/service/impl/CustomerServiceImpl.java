package app.service.impl;

import app.config.SecurityUtils;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.UserRepository;
import app.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> findAllForCurrentUser() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN, SALES_MANAGER -> customerRepository.findAll();
            case ACCOUNT_MANAGER -> customerRepository.findForAccountManager(currentUser.getId());
            default -> List.of();
        };
    }

    @Override
    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    @Override
    public Customer findByIdForCurrentUser(UUID id) {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN, SALES_MANAGER -> findById(id);

            case ACCOUNT_MANAGER -> customerRepository
                    .findByIdForAccountManager(id, currentUser.getId())
                    .orElseThrow(() ->
                            new AccessDeniedException("You are not allowed to access this customer"));

            default -> throw new AccessDeniedException("You are not allowed to access this customer");
        };
    }

    @Override
    @Transactional
    public Customer create(CustomerDTO dto) {
        Customer customer = Customer.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .country(dto.getCountry())
                .email(dto.getEmail())
                .baseServiceFee(dto.getBaseServiceFee())
                .build();

        Long lastSeq = customerRepository.findLastSequence();
        long nextSeq = lastSeq + 1;

        String number = String.format("%04d", nextSeq);
        customer.setNumber(number);

        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        Customer saved = customerRepository.save(customer);

        User currentUser = getCurrentUserWithManagedCustomers();
        if (currentUser.getRole() == Role.ACCOUNT_MANAGER) {
            currentUser.getManagedCustomers().add(saved);
            userRepository.save(currentUser);
        }

        return saved;
    }

    private User getCurrentUserWithManagedCustomers() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsernameWithManagedCustomers(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

    @Override
    public Customer update(UUID id, CustomerDTO dto) {
        Customer customer = findByIdForCurrentUser(id);
        customer.setName(dto.getName());
        customer.setCategory(dto.getCategory());
        customer.setEmail(dto.getEmail());
        customer.setCountry(dto.getCountry());
        customer.setBaseServiceFee(dto.getBaseServiceFee());
        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        customerRepository.deleteById(id);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsernameWithManagedCustomers(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }
}

