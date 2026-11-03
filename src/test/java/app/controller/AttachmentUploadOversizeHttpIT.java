package app.controller;

import app.TestPurchaseApplication;
import app.client.PricingClient;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.TwoFactorService;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
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

@SpringBootTest(
        classes = TestPurchaseApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.servlet.multipart.max-file-size=2KB",
        "spring.servlet.multipart.max-request-size=2KB"
})
class AttachmentUploadOversizeHttpIT {

    private static final Pattern CSRF_TOKEN =
            Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @MockBean
    private PricingClient pricingClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private final List<UUID> createdPurchaseIds = new ArrayList<>();
    private final List<UUID> createdCustomerIds = new ArrayList<>();
    private final List<UUID> createdShopIds = new ArrayList<>();

    private String baseUrl;
    private User admin;
    private TestPurchase testPurchase;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        transactionTemplate.executeWithoutResult(status -> {
            Customer customer = customerRepository.save(Customer.builder()
                    .name("Http Upload Customer " + suffix)
                    .number("U" + suffix.substring(0, 6))
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdCustomerIds.add(customer.getId());

            Shop shop = shopRepository.save(Shop.builder()
                    .name("Http Upload Shop " + suffix)
                    .number("S" + suffix.substring(0, 6))
                    .country(Country.GERMANY)
                    .build());
            createdShopIds.add(shop.getId());

            admin = userRepository.save(User.builder()
                    .username("http_upload_admin_" + suffix)
                    .password(passwordEncoder.encode("UploadAdminPass1!"))
                    .email("http_upload_" + suffix + "@example.com")
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(admin.getId());

            testPurchase = testPurchaseRepository.save(TestPurchase.builder()
                    .number("P" + suffix.substring(0, 6))
                    .customer(customer)
                    .shop(shop)
                    .country(Country.GERMANY)
                    .category(TestPurchaseCategory.S)
                    .type(TestPurchaseType.FORWARDING_TO_CLIENT)
                    .status(TestPurchaseStatus.INITIALISED)
                    .active(true)
                    .productPrice(10.0)
                    .serviceFee(1.0)
                    .postageFee(1.0)
                    .build());
            createdPurchaseIds.add(testPurchase.getId());

            String secret = twoFactorService.generateSecret();
            String enrollCode = twoFactorService.generateCodeForTimeStep(
                    secret, twoFactorService.currentTimeStep());
            User enrolledAdmin = userRepository.findById(admin.getId()).orElseThrow();
            twoFactorService.completeEnrollmentAndIssueRecoveryCodes(enrolledAdmin, secret, enrollCode);
        });
    }

    @AfterEach
    void tearDown() {
        testPurchaseTestDataCleaner.deletePurchasesByIds(createdPurchaseIds);
        transactionTemplate.executeWithoutResult(status -> {
            createdUserIds.forEach(userRepository::deleteById);
            createdCustomerIds.forEach(customerRepository::deleteById);
            createdShopIds.forEach(shopRepository::deleteById);
            createdPurchaseIds.clear();
            createdUserIds.clear();
            createdCustomerIds.clear();
            createdShopIds.clear();
        });
    }

    @Test
    void oversizeAttachmentUploadUsesRenderedFormActionAndShowsLocalizedFlash() throws Exception {
        HttpClient client = newHttpClient();

        loginViaForm(client, admin.getUsername(), "UploadAdminPass1!");

        HttpResponse<String> viewPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/testpurchases/view/" + testPurchase.getId()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(viewPage.statusCode()).isEqualTo(200);

        String html = viewPage.body();
        String uploadAction = parseFormAction(html, "attachment-upload-form");
        assertThat(uploadAction).contains("_csrf=");
        assertThat(html).contains("data-max-upload-bytes=\"2048\"");

        String csrf = parseCsrfTokenInForm(html, "attachment-upload-form");
        byte[] payload = new byte[4 * 1024];
        String boundary = "----UploadBoundary" + UUID.randomUUID();
        byte[] body = buildMultipartWithCsrf(
                boundary, "file", "large.pdf", "application/pdf", payload, csrf);

        HttpResponse<String> uploadResponse = client.send(
                HttpRequest.newBuilder(resolveAction(baseUrl, uploadAction))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(uploadResponse.statusCode()).isEqualTo(302);
        assertThat(uploadResponse.headers().firstValue("Location").orElse(""))
                .endsWith("/testpurchases/view/" + testPurchase.getId());

        HttpResponse<String> afterRedirect = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/testpurchases/view/" + testPurchase.getId()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(afterRedirect.statusCode()).isEqualTo(200);
        assertThat(afterRedirect.body()).contains("too large");
        assertThat(afterRedirect.body()).contains("DOCX");
    }

    @Test
    void oversizeAvatarUploadUsesRenderedFormActionAndShowsLocalizedMessage() throws Exception {
        HttpClient client = newHttpClient();

        loginViaForm(client, admin.getUsername(), "UploadAdminPass1!");

        HttpResponse<String> profilePage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/profile")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(profilePage.statusCode()).isEqualTo(200);

        String html = profilePage.body();
        String profileAction = parseFormAction(html, "profile-upload-form");
        assertThat(profileAction).contains("_csrf=");
        assertThat(html).contains("data-max-upload-bytes=\"2048\"");

        String csrf = parseCsrfTokenInForm(html, "profile-upload-form");
        byte[] payload = new byte[4 * 1024];
        String boundary = "----AvatarBoundary" + UUID.randomUUID();
        byte[] body = buildProfileMultipart(
                boundary,
                csrf,
                payload,
                admin.getUsername(),
                admin.getEmail(),
                admin.getCountry().name(),
                admin.getRole().name());

        HttpResponse<String> uploadResponse = client.send(
                HttpRequest.newBuilder(resolveAction(baseUrl, profileAction))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(uploadResponse.statusCode()).isEqualTo(200);
        assertThat(uploadResponse.body()).contains("too large");
        assertThat(uploadResponse.body()).contains("JPEG");
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private void loginViaForm(HttpClient client, String username, String password) throws Exception {
        HttpResponse<String> loginPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        String csrf = parseCsrfToken(loginPage.body());
        String form = "username=" + urlEncode(username)
                + "&password=" + urlEncode(password)
                + "&_csrf=" + urlEncode(csrf);
        HttpResponse<String> loginResponse = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(302);
        String location = loginResponse.headers().firstValue("Location").orElse("");
        if (location.contains("/2fa/verify")) {
            completeTotpVerify(client, username);
        } else {
            assertThat(location).contains("/home");
        }
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

    private static String parseFormAction(String html, String formId) {
        Pattern actionAfterId = Pattern.compile(
                "id=\"" + Pattern.quote(formId) + "\"[^>]*action=\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = actionAfterId.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        Pattern actionBeforeId = Pattern.compile(
                "action=\"([^\"]+)\"[^>]*id=\"" + Pattern.quote(formId) + "\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = actionBeforeId.matcher(html);
        assertThat(m.find()).as("form action for id=%s", formId).isTrue();
        return m.group(1);
    }

    private static String parseCsrfTokenInForm(String html, String formId) {
        int formStart = html.indexOf("id=\"" + formId + "\"");
        assertThat(formStart).isGreaterThanOrEqualTo(0);
        int formEnd = html.indexOf("</form>", formStart);
        assertThat(formEnd).isGreaterThan(formStart);
        String formHtml = html.substring(formStart, formEnd);
        return parseCsrfToken(formHtml);
    }

    private static URI resolveAction(String baseUrl, String action) {
        if (action.startsWith("http://") || action.startsWith("https://")) {
            return URI.create(action);
        }
        if (action.startsWith("/")) {
            return URI.create(baseUrl + action);
        }
        return URI.create(baseUrl + "/" + action);
    }

    private static byte[] buildMultipartWithCsrf(
            String boundary,
            String fileFieldName,
            String filename,
            String contentType,
            byte[] fileBytes,
            String csrfToken) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeField(out, boundary, "_csrf", csrfToken);
        writeFile(out, boundary, fileFieldName, filename, contentType, fileBytes);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static byte[] buildProfileMultipart(
            String boundary,
            String csrfToken,
            byte[] imageBytes,
            String username,
            String email,
            String country,
            String role) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeField(out, boundary, "_csrf", csrfToken);
        writeField(out, boundary, "username", username);
        writeField(out, boundary, "email", email);
        writeField(out, boundary, "country", country);
        writeField(out, boundary, "role", role);
        writeFile(out, boundary, "imageFile", "large.jpg", "image/jpeg", imageBytes);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static void writeField(
            ByteArrayOutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFile(
            ByteArrayOutputStream out,
            String boundary,
            String fieldName,
            String filename,
            String contentType,
            byte[] bytes) throws Exception {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String parseCsrfToken(String html) {
        Matcher matcher = CSRF_TOKEN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
