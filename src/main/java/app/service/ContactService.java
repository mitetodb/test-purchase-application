package app.service;

import app.config.ContactProperties;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContactService {

    private static final int MAX_NAME = 100;
    private static final int MAX_COMPANY = 200;
    private static final int MAX_EMAIL = 254;
    private static final int MAX_MESSAGE = 5000;

    private final ContactProperties contactProperties;
    private final ContactRateLimitService contactRateLimitService;
    private final EmailService emailService;

    public long minSubmitDelayMs() {
        return contactProperties.minSubmitDelayMs();
    }

    public boolean isRateLimitExceeded(String clientIp) {
        return contactRateLimitService.isRateLimitExceeded(clientIp);
    }

    public void recordSubmission(String clientIp) {
        contactRateLimitService.recordSubmission(clientIp);
    }

    public boolean isHoneypotFilled(String website) {
        return StringUtils.hasText(website);
    }

    public boolean isTooSoon(long formTimestampMs) {
        if (formTimestampMs <= 0) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - formTimestampMs;
        return elapsed < contactProperties.minSubmitDelayMs();
    }

    public boolean isValidSubmission(String name, String company, String email, String message, String privacy) {
        if (!StringUtils.hasText(name) || name.length() > MAX_NAME) {
            return false;
        }
        if (!StringUtils.hasText(company) || company.length() > MAX_COMPANY) {
            return false;
        }
        if (!StringUtils.hasText(email) || email.length() > MAX_EMAIL || !isValidEmail(email)) {
            return false;
        }
        if (!StringUtils.hasText(message) || message.length() > MAX_MESSAGE) {
            return false;
        }
        return "on".equals(privacy);
    }

    public boolean sendContactEmail(String name, String company, String email, String message) {
        return emailService.sendContactInquiry(
                contactProperties.recipient(),
                email,
                name,
                company,
                message);
    }

    private static boolean isValidEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
