package app.config;

import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.InviteTokenRepository;
import app.repository.UserRepository;
import app.service.InviteTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class I18nPagesTest {
    @Autowired MockMvc mvc;
    @MockBean app.client.PricingClient pricingClient;

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID adminUserId;
    private String validInviteToken;

    @BeforeEach
    void seedInviteToken() {
        transactionTemplate.executeWithoutResult(status -> {
            User admin = userRepository.save(User.builder()
                    .username("i18n_admin_" + UUID.randomUUID())
                    .email("i18n_admin_" + UUID.randomUUID() + "@example.com")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            adminUserId = admin.getId();
        });
        authenticateAdmin();
        validInviteToken = inviteTokenService.createInviteToken();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanupInviteFixtures() {
        transactionTemplate.executeWithoutResult(status -> {
            inviteTokenRepository.deleteAll();
            if (adminUserId != null) {
                userRepository.deleteById(adminUserId);
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
            "/login,bg,Потребителско име", "/login,de,Benutzername", "/login,en,Username",
            "/register,bg,Необходима е покана", "/register,de,Einladung erforderlich",
            "/,bg,Защо да изберете нас?", "/,de,Warum unsere Plattform?", "/,en,Why Choose Our Platform?",
            "/forgot-password,bg,Забравена парола", "/forgot-password,de,Passwort vergessen"})
    void pageIsTranslated(String path, String lang, String expected) throws Exception {
        mvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expected)));
    }

    @ParameterizedTest
    @CsvSource({
            "bg,Регистрация",
            "de,Registrieren"})
    void registerFormIsTranslatedWithValidInviteToken(String lang, String expected) throws Exception {
        mvc.perform(get("/register")
                        .param("token", validInviteToken)
                        .param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expected)))
                .andExpect(content().string(containsString("<form")));
    }

    private void authenticateAdmin() {
        User admin = userRepository.findById(adminUserId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
