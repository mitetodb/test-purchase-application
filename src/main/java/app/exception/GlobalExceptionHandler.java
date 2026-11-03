package app.exception;

import app.model.enums.Country;
import app.service.UploadErrorMessages;
import app.service.UserService;
import jakarta.servlet.RequestDispatcher;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Pattern ATTACHMENT_UPLOAD_PATH =
            Pattern.compile("^/attachments/upload/([0-9a-fA-F-]{36})$");

    private static final String ATTACHMENT_UPLOAD_PREFIX = "/attachments/upload/";

    private final UploadErrorMessages uploadErrorMessages;
    private final UserService userService;

    @ExceptionHandler(EntityNotFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFound(EntityNotFoundException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Entity not found: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Resource not found: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public String handleNotFound(HttpServletRequest request, Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        String requestedUrl = request.getRequestURI();
        
        log.warn("[{}] Page not found: {}", errorId, requestedUrl);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "The page you are looking for does not exist.");
        model.addAttribute("requestedUrl", requestedUrl);
        return "error/not-found";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Access denied: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/access-denied";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Upload too large: {}", errorId, request.getRequestURI(), ex);

        var locale = LocaleContextHolder.getLocale();
        String uri = resolveRequestUri(request);

        Optional<String> purchaseId = extractAttachmentUploadPurchaseId(uri);
        if (purchaseId.isPresent()) {
            redirectAttributes.addFlashAttribute("uploadError", uploadErrorMessages.attachmentTooLarge(locale));
            return "redirect:/testpurchases/view/" + purchaseId.get();
        }

        if ("/profile".equals(uri) && authentication != null) {
            model.addAttribute("uploadError", uploadErrorMessages.avatarTooLarge(locale));
            model.addAttribute("profile", userService.getProfile(authentication.getName()));
            model.addAttribute("countries", Country.values());
            return "profile";
        }

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", uploadErrorMessages.attachmentTooLarge(locale));
        return "error/bad-request";
    }

    @ExceptionHandler({IllegalArgumentException.class, BindException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Bad request: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", ex.getMessage());
        return "error/bad-request";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[{}] Method not supported: {}", errorId, ex.getMessage(), ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "Unsupported HTTP method.");
        return "error/bad-request";
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Unexpected error", errorId, ex);

        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "An unexpected error occurred. Please try again or contact support.");
        return "error/general-error";
    }

    private static String resolveRequestUri(HttpServletRequest request) {
        Object errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorUri instanceof String errorRequestUri && !errorRequestUri.isBlank()) {
            return errorRequestUri;
        }
        return request.getRequestURI();
    }

    private static Optional<String> extractAttachmentUploadPurchaseId(String uri) {
        if (uri == null) {
            return Optional.empty();
        }
        Matcher matcher = ATTACHMENT_UPLOAD_PATH.matcher(uri);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }
        if (uri.startsWith(ATTACHMENT_UPLOAD_PREFIX)) {
            String tail = uri.substring(ATTACHMENT_UPLOAD_PREFIX.length());
            if (tail.matches("[0-9a-fA-F-]{36}")) {
                return Optional.of(tail);
            }
        }
        return Optional.empty();
    }
}
