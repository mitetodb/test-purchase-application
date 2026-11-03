package app.repository.spec;

import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Role;
import app.model.enums.TestPurchaseStatus;
import app.model.filter.TestPurchaseListFilter;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TestPurchaseSpecifications {

    private TestPurchaseSpecifications() {
    }

    public static Specification<TestPurchase> listQuery(User currentUser, TestPurchaseListFilter filter) {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("customer", JoinType.INNER);
                root.fetch("shop", JoinType.INNER);
                root.fetch("mysteryShopper", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(roleScope(currentUser, root, query, cb));

            boolean includeArchived = filter != null && filter.isShowArchived()
                    && currentUser.getRole() == Role.ADMIN;
            if (!includeArchived) {
                predicates.add(cb.notEqual(root.get("status"), TestPurchaseStatus.ARCHIVED));
            }

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getCustomerId() != null) {
                    predicates.add(cb.equal(root.get("customer").get("id"), filter.getCustomerId()));
                }
                if (filter.getCreatedFrom() != null) {
                    LocalDateTime from = filter.getCreatedFrom().atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdOn"), from));
                }
                if (filter.getCreatedTo() != null) {
                    LocalDateTime to = filter.getCreatedTo().atTime(LocalTime.MAX);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdOn"), to));
                }
                if (filter.getMysteryShopperId() != null) {
                    predicates.add(cb.equal(root.get("mysteryShopper").get("id"), filter.getMysteryShopperId()));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate roleScope(User currentUser,
                                       jakarta.persistence.criteria.Root<TestPurchase> root,
                                       jakarta.persistence.criteria.CriteriaQuery<?> query,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
        return switch (currentUser.getRole()) {
            case ADMIN -> cb.conjunction();
            case MYSTERY_SHOPPER -> cb.equal(root.get("mysteryShopper").get("id"), currentUser.getId());
            case SALES_MANAGER -> cb.and(
                    cb.equal(root.get("customer").get("salesManager").get("id"), currentUser.getId()),
                    cb.isTrue(root.get("customer").get("active")));
            case ACCOUNT_MANAGER -> accountManagerScope(currentUser.getId(), root, query, cb);
            case CUSTOMER -> {
                if (currentUser.getCustomer() == null) {
                    yield cb.disjunction();
                }
                yield cb.equal(root.get("customer").get("id"), currentUser.getCustomer().getId());
            }
        };
    }

    private static Predicate accountManagerScope(UUID accountManagerId,
                                                 jakarta.persistence.criteria.Root<TestPurchase> root,
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
