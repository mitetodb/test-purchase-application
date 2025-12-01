package app.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        //todo email service
        System.out.printf("Password reset email to {%s}: {%s}", toEmail, resetLink);
    }
}
