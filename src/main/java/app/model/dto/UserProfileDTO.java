package app.model.dto;

import app.model.enums.Country;
import app.model.enums.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileDTO {

    @NotBlank
    private String username;   // read-only
    private Role role; // read-only
    private LocalDateTime createdOn; // read-only

    @Email
    @NotBlank
    private String email;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Country country;

}
