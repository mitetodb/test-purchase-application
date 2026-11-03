package app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Materializes deferred CSRF tokens (and the HTTP session backing them) before view
 * rendering. Large Thymeleaf templates can flush bytes to the client before they reach
 * a form's {@code _csrf} hidden input; lazy token generation then fails with
 * {@code IllegalStateException: Cannot create a session after the response has been committed}.
 */
public class CsrfTokenEagerMaterializationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldMaterializeBeforeView(request)) {
            CsrfToken csrfToken = resolveCsrfToken(request);
            if (csrfToken != null) {
                request.getSession(true);
                csrfToken.getToken();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Skip static assets and non-HTML fetches so we do not create a session per stylesheet or script.
     */
    private static boolean shouldMaterializeBeforeView(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.startsWith("/avatars/") || path.startsWith("/video/") || path.startsWith("/fonts/")
                || path.startsWith("/actuator/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
                || "/favicon.ico".equals(path)) {
            return false;
        }
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (accept == null || accept.isBlank()) {
            return true;
        }
        return accept.contains("text/html") || accept.contains("*/*");
    }

    @Nullable
    private static CsrfToken resolveCsrfToken(HttpServletRequest request) {
        Object attr = request.getAttribute(CsrfToken.class.getName());
        if (attr instanceof CsrfToken token) {
            return token;
        }
        attr = request.getAttribute("_csrf");
        if (attr instanceof CsrfToken token) {
            return token;
        }
        return null;
    }
}
