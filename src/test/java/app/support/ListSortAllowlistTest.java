package app.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class ListSortAllowlistTest {

    private static final Sort TP_DEFAULT = Sort.by(Sort.Direction.DESC, "createdOn");

    @Test
    void allowsVisibleColumnSortProperty() {
        Sort in = Sort.by(Sort.Direction.ASC, "number");
        Sort out = ListSortAllowlist.sanitize(in, ListSortAllowlist.ListKind.TEST_PURCHASE, TP_DEFAULT);
        assertThat(out.getOrderFor("number").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void hiddenPasswordPropertyFallsBackToDefault() {
        Sort in = Sort.by(Sort.Direction.ASC, "password");
        Sort out = ListSortAllowlist.sanitize(in, ListSortAllowlist.ListKind.USER, TP_DEFAULT);
        assertThat(out).isEqualTo(TP_DEFAULT);
    }

    @Test
    void garbagePropertyFallsBackToDefault() {
        Sort in = Sort.by("notARealColumn");
        Sort out = ListSortAllowlist.sanitize(in, ListSortAllowlist.ListKind.CUSTOMER, TP_DEFAULT);
        assertThat(out).isEqualTo(TP_DEFAULT);
    }
}
