package app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PageMetaModelAdvice {

    @ModelAttribute("pageCanonicalUrl")
    public String pageCanonicalUrl(HttpServletRequest request, @Value("${app.base-url}") String baseUrl) {
        return baseUrl + request.getRequestURI();
    }
}
