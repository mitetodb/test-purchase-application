package app.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    @NotBlank
    private String token;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;

}
