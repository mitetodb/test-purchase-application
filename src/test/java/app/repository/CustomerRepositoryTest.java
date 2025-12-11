package app.repository;

import app.model.entity.Customer;
import app.model.enums.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

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

