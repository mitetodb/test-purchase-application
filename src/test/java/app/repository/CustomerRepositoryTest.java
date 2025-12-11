package app.repository;

import app.model.entity.Customer;
import app.model.enums.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Force schema creation by accessing the database
        entityManager.getMetamodel();
    }

    @Test
    void testSaveAndFindCustomer() {
        Customer customer = Customer.builder()
                .name("Test Customer")
                .email("test@example.com")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(customerRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindLastSequence() {
        Customer customer1 = Customer.builder()
                .name("Customer 1")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        Customer customer2 = Customer.builder()
                .name("Customer 2")
                .country(Country.BULGARIA)
                .number("0002")
                .build();

        customerRepository.save(customer1);
        customerRepository.save(customer2);

        Long lastSeq = customerRepository.findLastSequence();
        assertThat(lastSeq).isNotNull();
    }
}

