package app.model.filter;

import app.model.enums.Country;
import lombok.Data;

@Data
public class CustomerListFilter {

    private String name;

    private Country country;

    private String category;

    /** Admin only: include deactivated customers in list/export (default active-only). */
    private boolean showDeactivated;
}
