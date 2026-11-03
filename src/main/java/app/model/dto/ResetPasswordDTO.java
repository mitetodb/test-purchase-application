package app.model.dto;

import jakarta.validation.constraints.NotBlank;
import app.validation.ValidPassword;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    @NotBlank
    private String token;

    @NotBlank
    @ValidPassword
    private String password;

    @NotBlank
    private String confirmPassword;

}
