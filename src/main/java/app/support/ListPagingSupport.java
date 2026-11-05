package app.support;

import app.config.AppListPaginationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListPagingSupport {

    private final AppListPaginationProperties properties;

    private static final Sort TEST_PURCHASE_DEFAULT = Sort.by(Sort.Direction.DESC, "createdOn");
    private static final Sort CUSTOMER_DEFAULT = Sort.by(Sort.Direction.ASC, "name");
    private static final Sort USER_DEFAULT = Sort.by(Sort.Direction.DESC, "createdOn");
    private static final Sort INVOICE_DEFAULT = Sort.by(Sort.Direction.DESC, "createdOn");

    public Pageable forTestPurchases(Pageable pageable) {
        return clamp(pageable, ListSortAllowlist.ListKind.TEST_PURCHASE, TEST_PURCHASE_DEFAULT);
    }

    public Pageable forCustomers(Pageable pageable) {
        return clamp(pageable, ListSortAllowlist.ListKind.CUSTOMER, CUSTOMER_DEFAULT);
    }

    public Pageable forUsers(Pageable pageable) {
        return clamp(pageable, ListSortAllowlist.ListKind.USER, USER_DEFAULT);
    }

    public Pageable forInvoices(Pageable pageable) {
        return clamp(pageable, ListSortAllowlist.ListKind.INVOICE, INVOICE_DEFAULT);
    }

    /** Sort only (page 0, size 1) for export queries. */
    public Pageable sortOnlyForTestPurchases(Pageable pageable) {
        Sort sort = ListSortAllowlist.sanitize(
                pageable.getSort(), ListSortAllowlist.ListKind.TEST_PURCHASE, TEST_PURCHASE_DEFAULT);
        return PageRequest.of(0, 1, sort);
    }

    public Pageable sortOnlyForCustomers(Pageable pageable) {
        Sort sort = ListSortAllowlist.sanitize(
                pageable.getSort(), ListSortAllowlist.ListKind.CUSTOMER, CUSTOMER_DEFAULT);
        return PageRequest.of(0, 1, sort);
    }

    public Pageable sortOnlyForUsers(Pageable pageable) {
        Sort sort = ListSortAllowlist.sanitize(
                pageable.getSort(), ListSortAllowlist.ListKind.USER, USER_DEFAULT);
        return PageRequest.of(0, 1, sort);
    }

    private Pageable clamp(Pageable pageable, ListSortAllowlist.ListKind kind, Sort defaultSort) {
        int size = pageable.getPageSize();
        if (size < 1) {
            size = properties.getDefaultPageSize();
        }
        if (size > properties.getMaxPageSize()) {
            size = properties.getMaxPageSize();
        }
        int page = Math.max(0, pageable.getPageNumber());
        Sort sort = ListSortAllowlist.sanitize(pageable.getSort(), kind, defaultSort);
        return PageRequest.of(page, size, sort);
    }
}
