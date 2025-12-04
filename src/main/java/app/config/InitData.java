package app.config;

import app.model.dto.RegistrationDTO;
import app.model.enums.Country;
import app.model.enums.Role;
import app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true", matchIfMissing = true)
public class InitData implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {

        if (userService.countUsers() == 0) {
            RegistrationDTO firstUser = new RegistrationDTO();
            firstUser.setUsername("admin");
            firstUser.setPassword("123123");
            firstUser.setConfirmPassword("123123");
            firstUser.setEmail("admin@example.com");
            firstUser.setRole(Role.ADMIN);
            firstUser.setCountry(Country.BULGARIA);

            userService.register(firstUser);

            log.info("Default admin account created: username='{}'", firstUser.getUsername());
        } else {
            log.info("Users already exist. Skipping default admin creation.");
        }
    }
}
