package app.model.filter;

import app.model.enums.Country;
import app.model.enums.Role;
import lombok.Data;

@Data
public class UserListFilter {

    private Role role;

    private Boolean active;

    private Country country;

    private String username;
}
