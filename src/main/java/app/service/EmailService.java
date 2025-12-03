package app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendStatusChangeEmail(
            String toEmail,
            String tpNumber,
            String oldStatus,
            String newStatus) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Status Update for Test Purchase " + tpNumber);
        message.setText("The status has changed from " + oldStatus + " to " + newStatus + ".");

        System.out.printf("Status change email to {%s}: {%s}", toEmail, message.getText());
        javaMailSender.send(message);
    }

    public void sendResetPasswordEmail(
            String toEmail,
            String username,
            String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset password for " + username);
        message.setText("Password reset link:\n" + resetLink);

        System.out.printf("Password reset for user {%s} with email to {%s}:\n{%s}", username, toEmail, resetLink);
        javaMailSender.send(message);
    }

}
