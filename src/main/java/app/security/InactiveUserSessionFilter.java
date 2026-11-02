package app.security;

import app.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ends the request as unauthenticated when the logged-in user's account was deactivated in the DB
 * (works across app restarts; complements {@link UserSessionInvalidationService}).
 * <p>
 * Performs one {@code findByUsername} per authenticated request (fail-closed if missing or lookup fails).
 */
public class InactiveUserSessionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public InactiveUserSessionFilter(UserRepository userRepository, TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            if (isSessionUserDeactivated(authentication.getName())) {
                SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
                logoutHandler.setInvalidateHttpSession(true);
                logoutHandler.setClearAuthentication(true);
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSessionUserDeactivated(String username) {
        try {
            Boolean deactivated = transactionTemplate.execute(status -> userRepository.findByUsername(username)
                    .map(user -> !user.isActive())
                    .orElse(true));
            return deactivated == null || deactivated;
        } catch (RuntimeException ex) {
            return true;
        }
    }
}
