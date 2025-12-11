package app.service;

import app.client.PricingCustomerClient;
import app.config.SecurityUtils;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.UserRepository;
import app.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PricingCustomerClient pricingCustomerClient;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private UUID customerId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void testFindAll() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<Customer> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(customer);
        verify(customerRepository).findAll();
    }

    @Test
    void testFindById() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Customer result = customerService.findById(customerId);

        assertThat(result).isEqualTo(customer);
        verify(customerRepository).findById(customerId);
    }

    @Test
    void testFindByIdNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void testCreate() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName("New Customer");
        dto.setCountry(Country.BULGARIA);

        when(customerRepository.findLastSequence()).thenReturn(0L);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsernameWithManagedCustomers(anyString())).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        doNothing().when(pricingCustomerClient).updateBaseFee(any(UUID.class), any());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            Customer result = customerService.create(dto);

            assertThat(result.getName()).isEqualTo("New Customer");
            assertThat(result.getNumber()).isEqualTo("0001");
            verify(customerRepository).save(any(Customer.class));
        }
    }

    @Test
    void testDelete() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findByUsernameWithManagedCustomers(anyString()))
                .thenReturn(Optional.of(adminUser));
        doNothing().when(pricingCustomerClient).deleteBaseFee(any(UUID.class));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            customerService.delete(customerId);

            verify(customerRepository).delete(customer);
        }
    }
}

