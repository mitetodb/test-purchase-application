package app.repository.spec;

import app.model.entity.Customer;
import app.model.entity.User;
import app.model.filter.CustomerListFilter;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> listQuery(User currentUser, CustomerListFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean includeDeactivated = filter != null
                    && filter.isShowDeactivated()
                    && currentUser.getRole() == app.model.enums.Role.ADMIN;
            if (!includeDeactivated) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            predicates.add(roleScope(currentUser, root, query, cb));

            if (filter != null) {
                if (filter.getName() != null && !filter.getName().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("name")),
                            "%" + filter.getName().trim().toLowerCase() + "%"));
                }
                if (filter.getCountry() != null) {
                    predicates.add(cb.equal(root.get("country"), filter.getCountry()));
                }
                if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("category")),
                            "%" + filter.getCategory().trim().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate roleScope(User currentUser,
                                       jakarta.persistence.criteria.Root<Customer> root,
                                       jakarta.persistence.criteria.CriteriaQuery<?> query,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
        return switch (currentUser.getRole()) {
            case ADMIN -> cb.conjunction();
            case SALES_MANAGER -> cb.equal(root.get("salesManager").get("id"), currentUser.getId());
            case ACCOUNT_MANAGER -> accountManagerScope(currentUser.getId(), root, query, cb);
            default -> cb.disjunction();
        };
    }

    private static Predicate accountManagerScope(UUID accountManagerId,
                                                 jakarta.persistence.criteria.Root<Customer> root,
                                                 jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                 jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<UUID> sq = query.subquery(UUID.class);
        var userRoot = sq.from(User.class);
        var managedCustomer = userRoot.join("managedCustomers");
        sq.select(managedCustomer.get("id"))
                .where(
                        cb.equal(userRoot.get("id"), accountManagerId),
                        cb.isTrue(managedCustomer.get("active"))
                );
        return root.get("id").in(sq);
    }
}
