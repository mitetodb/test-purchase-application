package app.client;

import app.model.dto.CustomerBaseFeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "pricingCustomerClient",
        url = "${pricing.api.base-url}",
        configuration = app.config.FeignConfig.class
)
public interface PricingCustomerClient {

    @PostMapping("/api/customers/{customerId}/base-fee")
    void createBaseFee(@PathVariable("customerId") UUID customerId,
                       @RequestBody CustomerBaseFeeDTO dto);

    @PutMapping("/api/customers/{customerId}/base-fee")
    void updateBaseFee(@PathVariable("customerId") UUID customerId,
                       @RequestBody CustomerBaseFeeDTO dto);

    @DeleteMapping("/api/customers/{customerId}/base-fee")
    void deleteBaseFee(@PathVariable("customerId") UUID customerId);
}
