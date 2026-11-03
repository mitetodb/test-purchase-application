package app.service.impl;

import app.client.PricingCustomerClient;
import app.config.SecurityUtils;
import app.model.dto.CustomerBaseFeeDTO;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Role;
import app.model.filter.CustomerListFilter;
import app.repository.spec.CustomerSpecifications;
import app.repository.CustomerRepository;
import app.repository.UserRepository;
import app.service.AccessControlService;
import app.service.CustomerService;
import app.service.EntityNumberAllocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final EntityNumberAllocator entityNumberAllocator;

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
        return findAllForCurrentUser(false);
    }

    @Override
    public List<Customer> findAllForCurrentUser(boolean includeDeactivated) {
        User currentUser = accessControlService.getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> includeDeactivated
                    ? customerRepository.findAllOrdered()
                    : customerRepository.findAllActive();
            case SALES_MANAGER -> customerRepository.findForSalesManager(currentUser.getId());
            case ACCOUNT_MANAGER -> customerRepository.findForAccountManager(currentUser.getId());
            default -> List.of();
        };
    }

    @Override
    public Page<Customer> findPageForCurrentUser(CustomerListFilter filter, Pageable pageable) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        var spec = CustomerSpecifications.listQuery(currentUser, filter);
        return customerRepository.findAll(spec, pageable);
    }

    @Override
    public long countForList(CustomerListFilter filter) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        return customerRepository.count(CustomerSpecifications.listQuery(currentUser, filter));
    }

    @Override
    public List<Customer> findAllForListExport(CustomerListFilter filter, Pageable sortPageable, int maxRows) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        var spec = CustomerSpecifications.listQuery(currentUser, filter);
        Pageable page = PageRequest.of(0, maxRows, sortPageable.getSort());
        return customerRepository.findAll(spec, page).getContent();
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

        customer.setNumber(entityNumberAllocator.nextCustomerNumber());

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

        // Check pricing permission if baseServiceFee is being changed
        if (dto.getBaseServiceFee() != null && 
            (customer.getBaseServiceFee() == null || 
             !customer.getBaseServiceFee().equals(dto.getBaseServiceFee()))) {
            if (!accessControlService.canSetCustomerPricing()) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not allowed to set customer pricing");
            }
        }

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
    public void deactivateCustomer(UUID id) {
        Customer customer = findByIdForCurrentUser(id);
        customer.setActive(false);
        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void reactivateCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        accessControlService.checkCanViewCustomer(customer);
        customer.setActive(true);
        customer.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        customerRepository.save(customer);
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

}

