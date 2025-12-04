package app.model.dto;

import app.model.enums.Role;
import app.model.enums.Country;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserEditDTO {

    @NotNull
    private UUID id;

    private String username;
    private String email;
    private Country country;
    private Role role;
    private boolean active;

    private List<UUID> managedCustomerIds;
}
