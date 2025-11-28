package app.model.dto;

import app.model.enums.Country;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ShopDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Shop name is required.")
    private String name;

    @Enumerated(EnumType.STRING)
    private Country country;

    private String notes;

    private String description;
}
