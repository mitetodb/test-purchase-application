package app.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PriceCalculationClient {

    private final RestClient restClient = RestClient.create("http://localhost:8081");

    public Double calculateTotalPrice(Map<String, Object> request) {
        return restClient.post()
                .uri("/api/price/calculate")
                .body(request)
                .retrieve()
                .body(Double.class);
    }
}
