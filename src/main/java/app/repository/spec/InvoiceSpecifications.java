package app.repository.spec;

import app.model.entity.Invoice;
import app.model.entity.User;
import app.model.enums.InvoiceStatus;
import app.model.enums.Role;
import app.model.filter.InvoiceListFilter;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    public static Specification<Invoice> listQuery(User currentUser, InvoiceListFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(roleScope(currentUser, root, query, cb));

            if (filter != null) {
                if (filter.getCustomerId() != null) {
                    predicates.add(cb.equal(root.get("customer").get("id"), filter.getCustomerId()));
                }
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getPeriodYear() != null) {
                    predicates.add(cb.equal(root.get("periodYear"), filter.getPeriodYear()));
                }
                if (filter.getPeriodMonth() != null) {
                    predicates.add(cb.equal(root.get("periodMonth"), filter.getPeriodMonth()));
                }
                if (!filter.isShowCancelled()) {
                    predicates.add(cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED));
                }
            } else {
                predicates.add(cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate roleScope(User currentUser,
                                       jakarta.persistence.criteria.Root<Invoice> root,
                                       jakarta.persistence.criteria.CriteriaQuery<?> query,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
        return switch (currentUser.getRole()) {
            case ADMIN -> cb.conjunction();
            case ACCOUNT_MANAGER -> accountManagerScope(currentUser.getId(), root, query, cb);
            case SALES_MANAGER -> cb.equal(root.get("customer").get("salesManager").get("id"), currentUser.getId());
            case CUSTOMER -> {
                if (currentUser.getCustomer() == null) {
                    yield cb.disjunction();
                }
                yield cb.equal(root.get("customer").get("id"), currentUser.getCustomer().getId());
            }
            default -> cb.disjunction();
        };
    }

    private static Predicate accountManagerScope(UUID accountManagerId,
                                                 jakarta.persistence.criteria.Root<Invoice> root,
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
        return root.get("customer").get("id").in(sq);
    }
}
