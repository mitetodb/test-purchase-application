package app.security;

import app.service.LoginSecurityService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginSecurityService loginSecurityService;

    @PostConstruct
    void init() {
        setDefaultFailureUrl("/login?error");
        setAllowSessionCreation(false);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String clientIp = ClientIpResolver.resolve(request);
        loginSecurityService.handleFailedLogin(clientIp, username);

        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }
}
