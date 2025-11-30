package app.model.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
public class ItemDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Product URL is required.")
    private String productUrl;

    @NotBlank(message = "Product name is required.")
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;

    @Min(value = 0, message = "Unit price must be positive.")
    private Double unitPrice;
}
