package app.config;

import app.model.dto.UserCreateDTO;
import app.model.enums.Country;
import app.model.enums.Role;
import app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true", matchIfMissing = true)
public class InitData implements CommandLineRunner {

    private final UserService userService;

    /** Initial admin password; must come from env ADMIN_PASSWORD. No default is shipped. */
    @Value("${app.init-data.admin-password:}")
    private String adminPassword;

    @Value("${app.init-data.admin-email:admin@example.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {

        if (userService.countUsers() == 0) {
            if (!StringUtils.hasText(adminPassword)) {
                log.warn("No users exist and ADMIN_PASSWORD is not set - skipping default admin creation.");
                return;
            }
            UserCreateDTO firstUser = new UserCreateDTO();
            firstUser.setUsername("admin");
            firstUser.setPassword(adminPassword);
            firstUser.setConfirmPassword(adminPassword);
            firstUser.setEmail(adminEmail);
            firstUser.setRole(Role.ADMIN);
            firstUser.setCountry(Country.BULGARIA);
            firstUser.setActive(true);

            userService.createUser(firstUser);

            log.info("Default admin account created: username='{}'", firstUser.getUsername());
        } else {
            log.info("Users already exist. Skipping default admin creation.");
        }
    }
}
