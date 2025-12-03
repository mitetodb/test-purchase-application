package app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendStatusChangeEmail(String toEmail,
                                      String tpNumber,
                                      String oldStatus,
                                      String newStatus,
                                      String username,
                                      String comment) {

        sendHtmlEmail(
                toEmail,
                "Status Update for Test Purchase " + tpNumber,
                "status-change-email",
                Map.of(
                        "tpNumber", tpNumber,
                        "oldStatus", oldStatus,
                        "newStatus", newStatus,
                        "username", username,
                        "comment", comment
                )
        );
    }

    @Async
    public void sendResetPasswordEmail(String toEmail,
                                       String username,
                                       String resetLink) {

        sendHtmlEmail(
                toEmail,
                "Reset password for " + username,
                "password-reset-email",
                Map.of(
                        "username", username,
                        "resetLink", resetLink
                )
        );
    }

    private void sendHtmlEmail(String to,
                               String subject,
                               String templateName,
                               Map<String, Object> variables) {

        Context ctx = new Context();
        variables.forEach(ctx::setVariable);

        String htmlContent = templateEngine.process(templateName, ctx);

        MimeMessage mime = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mime);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

}
