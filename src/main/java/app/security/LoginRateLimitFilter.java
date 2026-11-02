package app.security;

import app.service.LoginSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginSecurityService loginSecurityService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isLoginPost(request)) {
            String clientIp = ClientIpResolver.resolve(request);
            String username = request.getParameter("username");
            if (loginSecurityService.isRateLimitExceeded(clientIp, username)) {
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isLoginPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith("/login");
    }
}
