package app.model.dto;

import app.model.enums.Country;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Name is required.")
    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    private Country country;

    private String email;

    private Double baseServiceFee;

}
