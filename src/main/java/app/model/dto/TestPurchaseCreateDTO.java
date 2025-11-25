package app.model.dto;

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

    @Valid
    private List<ItemDTO> items = new ArrayList<>();
}
