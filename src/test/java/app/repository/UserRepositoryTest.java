package app.repository;

import app.model.entity.User;
import app.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

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
