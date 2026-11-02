package app.repository.spec;

import app.model.entity.User;
import app.model.filter.UserListFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> listQuery(UserListFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getRole() != null) {
                    predicates.add(cb.equal(root.get("role"), filter.getRole()));
                }
                if (filter.getActive() != null) {
                    predicates.add(cb.equal(root.get("active"), filter.getActive()));
                }
                if (filter.getCountry() != null) {
                    predicates.add(cb.equal(root.get("country"), filter.getCountry()));
                }
                if (filter.getUsername() != null && !filter.getUsername().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("username")),
                            "%" + filter.getUsername().trim().toLowerCase() + "%"));
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
