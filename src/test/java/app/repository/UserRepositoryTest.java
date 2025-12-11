package app.repository;

import app.model.entity.User;
import app.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.context.SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Force schema creation by accessing the database
        entityManager.getMetamodel();
    }

    @Test
    void testSaveAndFindByUsername() {
        User user = new User();
        user.setUsername("Vik");
        user.setEmail("vik@example.com");
        user.setPassword("123123");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);

        userRepository.save(user);

        var found = userRepository.findByUsername("Vik");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("vik@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.CUSTOMER);
    }
}
