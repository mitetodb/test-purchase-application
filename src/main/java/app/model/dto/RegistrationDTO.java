package app.model.dto;

import app.model.enums.Country;
import app.validation.ValidPassword;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationDTO {

    @NotBlank
    @Size(min = 4, max = 20)
    private String username;

    @NotBlank
    @ValidPassword
    private String password;

    @NotBlank
    private String confirmPassword;

    @Email
    @NotBlank
    private String email;

    @Enumerated(EnumType.STRING)
    private Country country;

    // Invite token from URL query parameter
    private String inviteToken;
}
