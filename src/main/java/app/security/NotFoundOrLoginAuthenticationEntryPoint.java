package app.security;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotFoundOrLoginAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String LOGIN_URL = "/login";

    private final MvcHandlerPresenceChecker mvcHandlerPresenceChecker;
    private final LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint(LOGIN_URL);

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (!mvcHandlerPresenceChecker.hasNonResourceHandler(request)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value());
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
            request.getRequestDispatcher("/error").forward(request, response);
            return;
        }
        loginEntryPoint.commence(request, response, authException);
    }
}
