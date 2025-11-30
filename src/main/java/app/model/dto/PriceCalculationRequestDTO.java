package app.model.dto;

import java.util.UUID;

public class PriceCalculationRequestDTO {

    private UUID customerId;
    private String country;   // "GERMANY""
    private String category;  // S, M, L, XL
    private String type;      // FORWARDING_TO_CLIENT, RETURN_BACK_TO_SELLER
    private double productTotal;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getProductTotal() { return productTotal; }
    public void setProductTotal(double productTotal) { this.productTotal = productTotal; }
}
