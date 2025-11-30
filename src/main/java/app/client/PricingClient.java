package app.client;

import app.model.dto.PriceCalculationRequestDTO;
import app.model.dto.PriceCalculationResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class PricingClient {

    @Value("${rest.microservice.url}")
    private String restMicroserviceUrl;

    private final RestTemplate restTemplate;

    public PricingClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PriceCalculationResponseDTO calculatePrice(PriceCalculationRequestDTO request) {
        return restTemplate.postForObject(restMicroserviceUrl, request, PriceCalculationResponseDTO.class);
    }
}
