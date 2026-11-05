package app.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class HttpServerErrorTrackingFilter extends OncePerRequestFilter {

    private final HttpServerErrorTracker tracker;

    public HttpServerErrorTrackingFilter(HttpServerErrorTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (response.getStatus() >= 500) {
            tracker.recordServerError();
        }
    }
}
