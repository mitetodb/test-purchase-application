package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that admin Deactivate expires the target user's live HTTP session via
 * {@link SessionRegistry} ({@link SessionInformation#expireNow()}), not only the inactive-user filter.
 */
@SpringBootTest(
        classes = TestPurchaseApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminDeactivateLiveSessionIT {

    private static final Pattern CSRF_TOKEN =
            Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwoFactorServiceImpl twoFactorService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();

    private String baseUrl;
    private String suffix;
    private User targetUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        suffix = UUID.randomUUID().toString().substring(0, 8);
        transactionTemplate.executeWithoutResult(status -> {
            targetUser = userRepository.save(User.builder()
                    .username("demo_user_" + suffix)
                    .email("demo_user_" + suffix + "@deact-live.test")
                    .password(passwordEncoder.encode("DemoUserPass1!"))
                    .role(Role.MYSTERY_SHOPPER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(targetUser.getId());

            adminUser = userRepository.save(User.builder()
                    .username("demo_admin_" + suffix)
                    .email("demo_admin_" + suffix + "@deact-live.test")
                    .password(passwordEncoder.encode("DemoAdminPass1!"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(adminUser.getId());
            String secret = twoFactorService.generateSecret();
            String code = twoFactorService.generateCurrentCode(secret);
            twoFactorService.completeEnrollmentAndIssueRecoveryCodes(adminUser, secret, code);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            createdUserIds.forEach(userRepository::deleteById);
            createdUserIds.clear();
        });
    }

    @Test
    void adminDeactivateExpiresLiveSessionViaSessionRegistry() throws Exception {
        HttpClient userClient = newHttpClient();
        HttpClient adminClient = newHttpClient();

        loginViaForm(userClient, targetUser.getUsername(), "DemoUserPass1!");

        HttpResponse<String> homeBefore = userClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/home")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(homeBefore.statusCode()).isEqualTo(200);

        String userSessionCookie = jsessionIdFromClient(userClient);
        assertThat(userSessionCookie).isNotBlank();

        Object targetPrincipal = findPrincipalForUsername(targetUser.getUsername());
        assertThat(targetPrincipal).isNotNull();
        List<SessionInformation> sessionsBefore =
                sessionRegistry.getAllSessions(targetPrincipal, false);
        assertThat(sessionsBefore).isNotEmpty();
        assertThat(sessionsBefore).allMatch(session -> !session.isExpired());

        loginViaForm(adminClient, adminUser.getUsername(), "DemoAdminPass1!");

        HttpResponse<String> usersPage = adminClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/users")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(usersPage.statusCode()).isEqualTo(200);
        String adminCsrf = parseCsrfToken(usersPage.body());

        HttpResponse<String> deactivateResponse = adminClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/users/deactivate/" + targetUser.getId()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + urlEncode(adminCsrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(deactivateResponse.statusCode()).isEqualTo(302);
        assertThat(deactivateResponse.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/users"));

        List<SessionInformation> sessionsAfter =
                sessionRegistry.getAllSessions(targetPrincipal, true);
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(SessionInformation::isExpired);
        assertThat(sessionRegistry.getAllSessions(targetPrincipal, false)).isEmpty();

        HttpResponse<String> homeAfter = userClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/home"))
                        .header("Cookie", "JSESSIONID=" + userSessionCookie)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(homeAfter.statusCode()).isEqualTo(302);
        assertThat(homeAfter.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/login"));

        HttpResponse<String> homeRetry = userClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/home"))
                        .header("Cookie", "JSESSIONID=" + userSessionCookie)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(homeRetry.statusCode()).isEqualTo(302);
        assertThat(homeRetry.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/login"));
    }

    private static HttpClient newHttpClient() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieManager)
                .build();
    }

    private void loginViaForm(HttpClient client, String username, String password) throws Exception {
        HttpResponse<String> loginPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);

        String csrf = parseCsrfToken(loginPage.body());
        String body = "username=" + urlEncode(username)
                + "&password=" + urlEncode(password)
                + "&_csrf=" + urlEncode(csrf);

        HttpResponse<String> loginResponse = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(302);
        String location = loginResponse.headers().firstValue("Location").orElse("");
        if (location.contains("/2fa/verify")) {
            completeTotpVerify(client, username);
            return;
        }
        assertThat(location).contains("/home");
    }

    private void completeTotpVerify(HttpClient client, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow();
        long step = user.getTotpLastTimeStep() == null ? twoFactorService.currentTimeStep()
                : user.getTotpLastTimeStep() + 1;
        String code = twoFactorService.generateCodeForUserAtTimeStep(user, step);
        HttpResponse<String> verifyPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/2fa/verify")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String csrf = parseCsrfToken(verifyPage.body());
        HttpResponse<String> verifyResponse = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/2fa/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "code=" + urlEncode(code) + "&_csrf=" + urlEncode(csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(verifyResponse.statusCode()).isEqualTo(302);
        assertThat(verifyResponse.headers().firstValue("Location").orElse("")).contains("/home");
    }

    private Object findPrincipalForUsername(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails userDetails
                    && username.equals(userDetails.getUsername())) {
                return principal;
            }
            if (username.equals(String.valueOf(principal))) {
                return principal;
            }
        }
        return null;
    }

    private static String parseCsrfToken(String html) {
        Matcher matcher = CSRF_TOKEN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String jsessionIdFromClient(HttpClient client) {
        CookieManager cookies = (CookieManager) client.cookieHandler().orElseThrow();
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> "JSESSIONID".equalsIgnoreCase(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse("");
    }
}
