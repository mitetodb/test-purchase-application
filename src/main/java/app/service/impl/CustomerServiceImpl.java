package app.service.impl;

import app.client.PricingCustomerClient;
import app.config.SecurityUtils;
import app.model.dto.CustomerBaseFeeDTO;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.UserRepository;
import app.service.AccessControlService;
import app.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PricingCustomerClient pricingCustomerClient;
    private final AccessControlService accessControlService;

    private static final Set<Role> CAN_SYNC_BASE_FEE = Set.of(
            Role.ADMIN,
            Role.ACCOUNT_MANAGER,
            Role.SALES_MANAGER
    );

    @Override
    @Cacheable(value = "customers", key = "'all'")
    public List<Customer> findAll() {
        return customerRepository.findAllActive();
    }

    @Override
    public List<Customer> findAllForCurrentUser() {
        User currentUser = accessControlService.getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> customerRepository.findAllActive();
            case SALES_MANAGER -> customerRepository.findForSalesManager(currentUser.getId());
            case ACCOUNT_MANAGER -> customerRepository.findForAccountManager(currentUser.getId());
            default -> List.of();
        };
    }

    @Override
    @Cacheable(value = "customers", key = "#id")
    public Customer findById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer not found");
        }
        
        return customer;
    }

    @Override
    public Customer findByIdForCurrentUser(UUID id) {
        Customer customer = findById(id);
        accessControlService.checkCanViewCustomer(customer);
        return customer;
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer create(CustomerDTO dto) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        
        // Only ADMIN, ACCOUNT_MANAGER, and SALES_MANAGER can create customers
        if (currentUser.getRole() != Role.ADMIN && 
            currentUser.getRole() != Role.ACCOUNT_MANAGER && 
            currentUser.getRole() != Role.SALES_MANAGER) {
            throw new AccessDeniedException("You are not allowed to create customers");
        }

        Customer customer = Customer.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .country(dto.getCountry())
                .email(dto.getEmail())
                .baseServiceFee(dto.getBaseServiceFee())
                .active(true)
                .build();

        Long lastSeq = customerRepository.findLastSequence();
        long nextSeq = lastSeq + 1;

        String number = String.format("%04d", nextSeq);
        customer.setNumber(number);

        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        if (currentUser.getRole() == Role.SALES_MANAGER) {
            customer.setSalesManager(currentUser);
        }

        Customer saved = customerRepository.save(customer);

        if (currentUser.getRole() == Role.ACCOUNT_MANAGER) {
            currentUser.getManagedCustomers().add(saved);
            userRepository.save(currentUser);
        }

        syncBaseFeeWithPricingApi(saved);

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer update(UUID id, CustomerDTO dto) {
        Customer customer = findByIdForCurrentUser(id);
        accessControlService.checkCanEditCustomer(customer);

        customer.setName(dto.getName());
        customer.setCategory(dto.getCategory());
        customer.setEmail(dto.getEmail());
        customer.setCountry(dto.getCountry());
        customer.setBaseServiceFee(dto.getBaseServiceFee());
        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        Customer saved = customerRepository.save(customer);

        syncBaseFeeWithPricingApi(saved);

        return customerRepository.save(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void delete(UUID id) {
        Customer customer = findByIdForCurrentUser(id);

        if (accessControlService.canHardDelete()) {
            deleteBaseFeeInPricingApi(customer);
            customerRepository.delete(customer);
        } else {
            customer.setActive(false);
            customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());
            customerRepository.save(customer);
        }
    }

    private void syncBaseFeeWithPricingApi(Customer customer) {
        User currentUser = accessControlService.getCurrentUser();

        if (!CAN_SYNC_BASE_FEE.contains(currentUser.getRole())) {
            log.debug("User [{}] with role [{}] is not allowed to sync baseServiceFee to pricing API",
                    currentUser.getUsername(), currentUser.getRole());
            return;
        }

        if (customer.getBaseServiceFee() == null) {
            log.debug("Customer [{}] baseServiceFee is null, skipping sync", customer.getId());
            return;
        }

        CustomerBaseFeeDTO dto = new CustomerBaseFeeDTO();
        dto.setCustomerId(customer.getId());
        dto.setBaseServiceFee(customer.getBaseServiceFee());
        dto.setChangedAt(LocalDateTime.now());

        try {
            pricingCustomerClient.updateBaseFee(customer.getId(), dto);
            log.info("Synced baseServiceFee [{}] for customer [{}] to pricing API",
                    dto.getBaseServiceFee(), dto.getCustomerId());
        } catch (Exception ex) {
            log.error("Failed to sync baseServiceFee for customer [{}] to pricing API: {}",
                    customer.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to sync base fee with pricing API", ex);
        }
    }

    private void deleteBaseFeeInPricingApi(Customer customer) {
        User currentUser = accessControlService.getCurrentUser();

        if (!CAN_SYNC_BASE_FEE.contains(currentUser.getRole())) {
            return;
        }

        try {
            pricingCustomerClient.deleteBaseFee(customer.getId());
            log.info("Deleted baseServiceFee for customer [{}] in pricing API", customer.getId());
        } catch (Exception ex) {
            log.error("Failed to delete baseServiceFee for customer [{}] in pricing API: {}",
                    customer.getId(), ex.getMessage(), ex);
        }
    }
}

