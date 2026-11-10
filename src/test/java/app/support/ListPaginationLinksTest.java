package app.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ListPaginationLinksTest {

    private final ListPaginationLinks links = new ListPaginationLinks();

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void keepsSortShowArchivedAndOtherFiltersWhenChangingPage() {
        MockHttpServletRequest request = request(
                "/testpurchases",
                "status=INITIALISED&showArchived=true&sort=number,asc&page=0&size=20&customerId=abc");

        String href = links.href("/testpurchases", 1, 20, "createdOn,desc");

        assertThat(href).startsWith("/testpurchases?");
        String query = decode(href.substring(href.indexOf('?') + 1));
        assertThat(query).contains("page=1");
        assertThat(query).doesNotContain("page=0");
        assertThat(query).contains("size=20");
        assertThat(query).contains("sort=number,asc");
        assertThat(query).contains("showArchived=true");
        assertThat(query).contains("status=INITIALISED");
        assertThat(query).contains("customerId=abc");
        assertThat(query).doesNotContain("createdOn,desc");
    }

    @Test
    void addsEffectiveSortWhenTheRequestHasNone() {
        MockHttpServletRequest request = request("/users", "username=ada&page=2&size=20");

        String href = links.href("/users", 1, 20, "createdOn,desc");

        assertThat(decode(href)).contains("sort=createdOn,desc");
        assertThat(decode(href)).contains("username=ada");
        assertThat(decode(href)).contains("page=1");
    }

    @Test
    void keepsUnknownFilterParams() {
        MockHttpServletRequest request = request("/invoices", "showCancelled=true&periodYear=2026&extra=kept");

        String href = links.href("/invoices", 3, 10, null);

        String query = decode(href);
        assertThat(query).contains("showCancelled=true");
        assertThat(query).contains("periodYear=2026");
        assertThat(query).contains("extra=kept");
        assertThat(query).contains("page=3");
        assertThat(query).contains("size=10");
        assertThat(query).doesNotContain("sort=");
    }

    private static MockHttpServletRequest request(String path, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setQueryString(query);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
