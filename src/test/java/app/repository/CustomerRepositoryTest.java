package app.repository;

import app.model.entity.Customer;
import app.model.enums.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testSaveAndFindCustomer() {
        long base = customerRepository.findLastSequence();

        Customer customer = Customer.builder()
                .name("Test Customer")
                .email("test@example.com")
                .country(Country.BULGARIA)
                .number(String.format("%04d", base + 1))
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(customerRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindLastSequence() {
        long base = customerRepository.findLastSequence();

        Customer customer1 = Customer.builder()
                .name("Customer 1")
                .country(Country.BULGARIA)
                .number(String.format("%04d", base + 1))
                .build();

        Customer customer2 = Customer.builder()
                .name("Customer 2")
                .country(Country.BULGARIA)
                .number(String.format("%04d", base + 2))
                .build();

        customerRepository.saveAndFlush(customer1);
        customerRepository.saveAndFlush(customer2);

        Long lastSeq = customerRepository.findLastSequence();
        assertThat(lastSeq).isEqualTo(base + 2);
    }
}
