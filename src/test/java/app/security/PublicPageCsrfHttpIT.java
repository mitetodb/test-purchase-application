package app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the embedded Tomcat stack (not MockMvc) so Thymeleaf can stream partial
 * output before CSRF hidden fields — reproducing production session/commit ordering bugs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublicPageCsrfHttpIT {

    private static final Pattern CSRF_HIDDEN =
            Pattern.compile("name=\"_csrf\"\\s+value=\"[^\"]+\"", Pattern.CASE_INSENSITIVE);
    /** Present on both the landing page and login footer. */
    private static final String FOOTER_MARKER = "© 2026 Getinweb";

    @LocalServerPort
    private int port;

    @MockBean
    private app.client.PricingClient pricingClient;

    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        baseUrl = "http://localhost:" + port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?lang=bg", "?lang=en", "?lang=de"})
    void getHomeReturnsCompleteHtmlWithCsrfAndFooter(String query) throws Exception {
        String url = query.isEmpty() ? baseUrl + "/" : baseUrl + "/" + query;
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Accept", "text/html")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).contains(FOOTER_MARKER);
        assertThat(CSRF_HIDDEN.matcher(body).find())
                .as("home page must include a materialized CSRF hidden input")
                .isTrue();
        assertThat(body.toLowerCase()).doesNotContain("illegalstateexception");
        assertThat(body.toLowerCase()).doesNotContain("whitelabel error page");
    }

    @Test
    void getLoginReturnsCompleteHtmlWithCsrfAndFooter() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                        .header("Accept", "text/html")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).contains(FOOTER_MARKER);
        assertThat(CSRF_HIDDEN.matcher(body).find()).isTrue();
        assertThat(body.toLowerCase()).doesNotContain("illegalstateexception");
    }
}
