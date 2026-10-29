package app.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel(@Value("${pricing.api.log-level:BASIC}") String level) {
        return Logger.Level.valueOf(level.toUpperCase());
    }

    /** Sends the shared secret expected by price-calculation-microservice. Never logged. */
    @Bean
    RequestInterceptor pricingApiKeyInterceptor(@Value("${pricing.api.key:}") String apiKey) {
        return template -> {
            if (StringUtils.hasText(apiKey)) {
                template.header("X-API-Key", apiKey);
            }
        };
    }
}
