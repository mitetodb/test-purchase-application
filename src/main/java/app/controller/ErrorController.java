package app.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Slf4j
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorId = UUID.randomUUID().toString();
        String requestedUrl = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                log.warn("[{}] 404 error for URL: {}", errorId, requestedUrl);
                model.addAttribute("errorId", errorId);
                model.addAttribute("message", "The page you are looking for does not exist.");
                model.addAttribute("requestedUrl", requestedUrl);
                return "error/not-found";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error("[{}] 500 error for URL: {}", errorId, requestedUrl);
                model.addAttribute("errorId", errorId);
                model.addAttribute("message", "An unexpected error occurred. Please try again or contact support.");
                return "error/general-error";
            }
        }
        
        log.warn("[{}] Error for URL: {}", errorId, requestedUrl);
        model.addAttribute("errorId", errorId);
        model.addAttribute("message", "An error occurred.");
        return "error/general-error";
    }
}
