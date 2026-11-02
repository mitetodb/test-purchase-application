package app.config;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestPurchaseApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.init-data.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@ActiveProfiles("test")
class UserListDeactivateScreenshotIT {

    private static final String ADMIN_USERNAME = "screenshot_admin";
    private static final String ADMIN_PASSWORD = "ScreenshotAdmin1!";

    @LocalServerPort
    private int port;

    @MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .username(ADMIN_USERNAME)
                .email("screenshot_admin@test.local")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .country(Country.GERMANY)
                .active(true)
                .build());
        userRepository.save(User.builder()
                .username("screenshot_active_ms")
                .email("active_ms@test.local")
                .password(passwordEncoder.encode("MsPass1234!"))
                .role(Role.MYSTERY_SHOPPER)
                .country(Country.GERMANY)
                .active(true)
                .build());
        userRepository.save(User.builder()
                .username("screenshot_inactive_ms")
                .email("inactive_ms@test.local")
                .password(passwordEncoder.encode("MsPass1234!"))
                .role(Role.MYSTERY_SHOPPER)
                .country(Country.GERMANY)
                .active(false)
                .build());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "CAPTURE_USER_LIST", matches = "true")
    void captureUsersListDeactivateAndReactivateButtons() throws Exception {
        Path dir = Path.of("docs/screenshots/pr-issue34");
        dir.toFile().mkdirs();
        Path deactivateShot = dir.resolve("users-list-deactivate-button.png").toAbsolutePath();
        Path reactivateShot = dir.resolve("users-list-reactivate-button.png").toAbsolutePath();

        Process npm = new ProcessBuilder("npm", "install", "puppeteer-core", "--no-save", "--prefix", "target/puppeteer")
                .redirectErrorStream(true)
                .start();
        if (!npm.waitFor(120, TimeUnit.SECONDS)) {
            npm.destroyForcibly();
            throw new IllegalStateException("npm install timed out");
        }
        if (npm.exitValue() != 0) {
            throw new IllegalStateException("npm install failed: " + new String(npm.getInputStream().readAllBytes()));
        }

        ProcessBuilder node = new ProcessBuilder(
                "node",
                Path.of("scripts/capture-users-list-screenshots.mjs").toAbsolutePath().toString());
        node.environment().putAll(Map.of(
                "APP_PORT", String.valueOf(port),
                "ADMIN_USER", ADMIN_USERNAME,
                "ADMIN_PASS", ADMIN_PASSWORD,
                "DEACTIVATE_SHOT", deactivateShot.toString(),
                "REACTIVATE_SHOT", reactivateShot.toString(),
                "NODE_PATH", Path.of("target/puppeteer/node_modules").toAbsolutePath().toString()));
        node.redirectErrorStream(true);
        Process process = node.start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("capture timed out: " + output);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("capture failed: " + output);
        }

        assertThat(deactivateShot).exists();
        assertThat(deactivateShot.toFile().length()).isGreaterThan(10_000);
        assertThat(reactivateShot).exists();
        assertThat(reactivateShot.toFile().length()).isGreaterThan(10_000);
    }
}
