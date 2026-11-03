package app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class TwoFactorSessionSupport {

    public void clearPendingTwoFactorState(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(TwoFactorSessionKeys.PASSWORD_LOGIN);
        session.removeAttribute(TwoFactorSessionKeys.VERIFIED);
        session.removeAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET);
        session.removeAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS);
    }

    public void invalidatePendingTwoFactorSession(
            HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        clearPendingTwoFactorState(session);
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    public int incrementFailedAttempts(HttpSession session) {
        Integer current = (Integer) session.getAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS);
        int next = current == null ? 1 : current + 1;
        session.setAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS, next);
        return next;
    }
}
