package app.client;

import app.config.FeignConfig;
import app.model.dto.PriceCalculationRequestDTO;
import app.model.dto.PriceCalculationResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "pricingClient",
        url = "${pricing.api.base-url}",
        configuration = FeignConfig.class
)
public interface PricingClient {

    @PostMapping("/api/pricing/calculate")
    PriceCalculationResponseDTO calculatePrice(@RequestBody PriceCalculationRequestDTO request);
}
