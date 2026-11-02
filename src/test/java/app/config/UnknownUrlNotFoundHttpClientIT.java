package app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UnknownUrlNotFoundHttpClientIT {

    private static final Pattern CSRF_TOKEN = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @MockBean
    private app.client.PricingClient pricingClient;

    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieManager)
                .build();
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void anonymousGetHomeRedirectsToLoginWithRealHttpClient() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/home"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/login"));
    }

    @Test
    void anonymousDeleteHomeDoesNotReturn405WithRealHttpClient() throws Exception {
        HttpResponse<String> loginPage = fetchLoginPage();
        String csrfToken = parseCsrfToken(loginPage.body());
        String cookieHeader = buildCookieHeader(loginPage);

        String deleteUrl = baseUrl + "/home?_csrf=" + java.net.URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(deleteUrl))
                        .method("DELETE", HttpRequest.BodyPublishers.noBody())
                        .header("Cookie", cookieHeader)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isNotEqualTo(405);
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/login"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "CAPTURE_404", matches = "true")
    void retake404PageScreenshotAfterFadeIn() throws Exception {
        String url = baseUrl + "/unknown-url-for-issue-30-screenshot";
        Path screenshot = Path.of("docs/screenshots/pr35/404-page.png");
        screenshot.getParent().toFile().mkdirs();

        ProcessBuilder chrome = new ProcessBuilder(
                "google-chrome",
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                "--window-size=1280,900",
                "--host-resolver-rules=MAP cdnjs.cloudflare.com 127.0.0.1, MAP cdn.jsdelivr.net 127.0.0.1, MAP cdn.datatables.net 127.0.0.1",
                "--virtual-time-budget=3000",
                "--run-all-compositor-stages-before-draw",
                "--screenshot=" + screenshot.toAbsolutePath(),
                url);
        Process process = chrome.start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
        assertThat(screenshot).exists();
        assertThat(screenshot.toFile().length()).isGreaterThan(10_000);
    }

    private HttpResponse<String> fetchLoginPage() throws Exception {
        HttpResponse<String> loginPage = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        return loginPage;
    }

    private String parseCsrfToken(String loginHtml) {
        Matcher matcher = CSRF_TOKEN.matcher(loginHtml);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String buildCookieHeader(HttpResponse<?> response) {
        StringBuilder cookies = new StringBuilder();
        for (String setCookie : response.headers().allValues("set-cookie")) {
            String nameValue = setCookie.split(";", 2)[0].trim();
            if (cookies.length() > 0) {
                cookies.append("; ");
            }
            cookies.append(nameValue);
        }
        assertThat(cookies.length()).isGreaterThan(0);
        return cookies.toString();
    }
}
