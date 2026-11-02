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
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            createdPurchaseIds.forEach(testPurchaseRepository::deleteById);
            createdPurchaseIds.clear();
            createdUserIds.forEach(userRepository::deleteById);
            createdUserIds.clear();
            createdCustomerIds.forEach(customerRepository::deleteById);
            createdCustomerIds.clear();
            createdShopIds.forEach(shopRepository::deleteById);
            createdShopIds.clear();
        });
    }

    @Test
    void oversizeUploadOnTomcatRedirectsToPurchaseViewWithLocalizedMessage() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        loginViaForm(client, admin.getUsername(), "UploadAdminPass1!");

        HttpResponse<String> viewPage = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/testpurchases/view/" + testPurchase.getId()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(viewPage.statusCode()).isEqualTo(200);
        String csrf = parseCsrfToken(viewPage.body());

        byte[] payload = new byte[4 * 1024];
        String boundary = "----UploadBoundary" + UUID.randomUUID();
        byte[] body = buildFileOnlyMultipart(boundary, "large.pdf", "application/pdf", payload);

        HttpResponse<String> uploadResponse = client.send(
                HttpRequest.newBuilder(URI.create(
                                baseUrl + "/attachments/upload/" + testPurchase.getId()
                                        + "?_csrf=" + urlEncode(csrf)))
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
    }

    private static byte[] buildFileOnlyMultipart(
            String boundary, String filename, String contentType, byte[] fileBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFile(out, boundary, "file", filename, contentType, fileBytes);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
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
