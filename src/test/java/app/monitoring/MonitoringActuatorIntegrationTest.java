package app.monitoring;

import app.TestPurchaseApplication;
import app.client.PricingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringActuatorIntegrationTest {

    private static Path markerFile;

    @MockBean
    private PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpServerErrorTracker serverErrorTracker;

    @org.junit.jupiter.api.BeforeEach
    void resetErrorTracker() {
        serverErrorTracker.clearForTests();
    }

    @DynamicPropertySource
    static void monitoringProperties(DynamicPropertyRegistry registry) throws Exception {
        markerFile = Files.createTempFile("backup-marker-", ".txt");
        Files.writeString(markerFile, Instant.now().minus(Duration.ofHours(2)).toString());
        registry.add("app.monitoring.backup-check-enabled", () -> "true");
        registry.add("app.monitoring.backup-marker-path", () -> markerFile.toString());
        registry.add("management.endpoint.health.show-details", () -> "always");
        registry.add("app.monitoring.server-errors.threshold", () -> "2");
        registry.add("app.monitoring.server-errors.window-seconds", () -> "300");
    }

    @Test
    void actuatorHealthIncludesBackupAndServerErrorsComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.backup.status").value("UP"))
                .andExpect(jsonPath("$.components.serverErrors.status").value("UP"));
    }

    @Test
    void elevated5xxRateMarksServerErrorsDown() throws Exception {
        serverErrorTracker.recordServerError();
        serverErrorTracker.recordServerError();

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.components.serverErrors.status").value("DOWN"));
    }
}
