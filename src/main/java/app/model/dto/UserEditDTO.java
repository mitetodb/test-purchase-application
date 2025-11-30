package app.model.dto;

import app.model.enums.Country;
import app.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UserEditDTO {

    @NotNull
    private UUID id;

    @NotBlank(message = "Username is required.")
    private String username;

    @Email(message = "Email must be valid.")
    @NotBlank(message = "Email is required.")
    private String email;

    @NotNull(message = "Role is required.")
    private Role role;

    @NotNull(message = "Country is required.")
    private Country country;

    private boolean active = true;
}
