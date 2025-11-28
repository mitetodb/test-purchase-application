package app.model.dto;

import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class TestPurchaseCreateDTO {

    @NotNull(message = "Customer is required.")
    private UUID customerId;

    @NotNull(message = "Shop is required.")
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Enumerated(EnumType.STRING)
    private TestPurchaseCategory category;

    @Enumerated(EnumType.STRING)
    private TestPurchaseType type;

    @Valid
    private List<ItemDTO> items = new ArrayList<>();
}
