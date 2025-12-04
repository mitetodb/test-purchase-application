package app.config;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

@Slf4j
@ControllerAdvice(basePackages = "app.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(EntityNotFoundException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Entity not found: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Access denied: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/access-denied";
    }

    @ExceptionHandler({IllegalArgumentException.class, BindException.class})
    public String handleBadRequest(Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Bad request: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/bad-request";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Method not supported: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "Unsupported HTTP method.");
        return "error/bad-request";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Unexpected error", errorId, ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "Възникна неочаквана грешка. Моля, опитайте отново или се свържете с поддръжка.");
        return "error/general-error";
    }
}
