package app.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.util.List;

/**
 * Builds list pagination links from the current request so every active query
 * parameter (sort, showArchived, filters) survives a page change.
 */
@Component("listPaginationLinks")
public class ListPaginationLinks {

    /**
     * @param listPath  list path used when the request has no path
     * @param sortIfAbsent effective sort ({@code property,direction}) when the request has no {@code sort}
     */
    public String href(String listPath, int page, int size, String sortIfAbsent) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);
        boolean hasSort = hasSort(builder.build());
        builder.replaceQueryParam("page", Math.max(page, 0));
        builder.replaceQueryParam("size", size);
        if (!hasSort && sortIfAbsent != null && !sortIfAbsent.isBlank()) {
            builder.replaceQueryParam("sort", sortIfAbsent);
        }
        UriComponents uri = builder.build();
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = listPath == null ? "" : listPath;
        }
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return path;
        }
        return path + "?" + query;
    }

    private static boolean hasSort(UriComponents uri) {
        List<String> sorts = uri.getQueryParams().get("sort");
        if (sorts == null) {
            return false;
        }
        for (String sort : sorts) {
            if (sort != null && !sort.isBlank()) {
                return true;
            }
        }
        return false;
    }
}
