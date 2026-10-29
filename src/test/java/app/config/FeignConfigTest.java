package app.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeignConfigTest {
    @Test
    void addsApiKeyHeaderWhenConfigured() {
        RequestTemplate t = new RequestTemplate();
        new FeignConfig().pricingApiKeyInterceptor("k-123").apply(t);
        assertThat(t.headers().get("X-API-Key")).containsExactly("k-123");
    }

    @Test
    void omitsHeaderWhenBlank() {
        RequestTemplate t = new RequestTemplate();
        new FeignConfig().pricingApiKeyInterceptor("").apply(t);
        assertThat(t.headers()).doesNotContainKey("X-API-Key");
    }
}
