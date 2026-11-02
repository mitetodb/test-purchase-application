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

    public Pageable forTestPurchases(Pageable pageable) {
        return clamp(pageable, "createdOn");
    }

    public Pageable forCustomers(Pageable pageable) {
        return clamp(pageable, "name");
    }

    public Pageable forUsers(Pageable pageable) {
        return clamp(pageable, "createdOn");
    }

    private Pageable clamp(Pageable pageable, String defaultSortProperty) {
        int size = pageable.getPageSize();
        if (size < 1) {
            size = properties.getDefaultPageSize();
        }
        if (size > properties.getMaxPageSize()) {
            size = properties.getMaxPageSize();
        }
        int page = Math.max(0, pageable.getPageNumber());
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, defaultSortProperty);
        return PageRequest.of(page, size, sort);
    }
}
