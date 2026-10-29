package app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI testPurchaseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Test Purchase Application API")
                .version("v1")
                .description("Endpoints of the test purchase web application. Swagger UI is disabled in prod unless SWAGGER_ENABLED=true."));
    }
}
