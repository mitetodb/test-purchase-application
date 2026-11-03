package app.security;

import app.model.entity.User;
import app.repository.UserRepository;
import app.security.TwoFactorSessionKeys;
import app.service.LoginSecurityService;
import app.service.TwoFactorService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LoginSecurityService loginSecurityService;
    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;

    @PostConstruct
    void init() {
        setDefaultTargetUrl("/home");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication != null && authentication.getName() != null) {
            loginSecurityService.handleSuccessfulLogin(authentication.getName());
        }

        if (authentication != null && authentication.getName() != null) {
            var session = request.getSession(true);
            session.setAttribute(TwoFactorSessionKeys.PASSWORD_LOGIN, Boolean.TRUE);
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null && twoFactorService.requiresTwoFactor(user)) {
                session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.FALSE);
                setDefaultTargetUrl(user.isTotpEnabled() ? "/2fa/verify" : "/2fa/enroll");
            } else {
                session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.TRUE);
                setDefaultTargetUrl("/home");
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
