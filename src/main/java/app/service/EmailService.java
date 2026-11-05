package app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.mail.visit-appointment.enabled:false}")
    private boolean visitAppointmentMailEnabled;

    @Async
    public void sendVisitAppointmentEmail(String toEmail,
                                        String tpNumber,
                                        LocalDateTime scheduledAt,
                                        String location,
                                        String notes,
                                        boolean confirmed) {
        if (!visitAppointmentMailEnabled) {
            log.info("Visit appointment mail disabled (app.mail.visit-appointment.enabled=false); skipping");
            return;
        }
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false); skipping visit appointment email");
            return;
        }
        String state = confirmed ? "confirmed" : "proposed";
        sendHtmlEmail(
                toEmail,
                "Visit appointment " + state + " for test purchase " + tpNumber,
                "visit-appointment-email",
                Map.of(
                        "tpNumber", tpNumber,
                        "scheduledAt", scheduledAt.toString(),
                        "location", location != null ? location : "",
                        "notes", notes != null ? notes : "",
                        "confirmed", confirmed
                )
        );
    }

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

    public boolean sendDeadlineReminderEmail(String toEmail,
                                             String tpNumber,
                                             java.time.LocalDate deadline,
                                             int daysBeforeOffset,
                                             String purchaseUrl) {
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false); skipping deadline reminder for {}", tpNumber);
            return false;
        }
        String subject = daysBeforeOffset == 0
                ? "Test purchase " + tpNumber + " deadline is today"
                : "Reminder: test purchase " + tpNumber + " deadline in " + daysBeforeOffset + " day(s)";
        return sendHtmlEmailSync(
                toEmail,
                subject,
                "deadline-reminder-email",
                Map.of(
                        "tpNumber", tpNumber,
                        "deadline", deadline.toString(),
                        "daysBeforeOffset", daysBeforeOffset,
                        "purchaseUrl", purchaseUrl
                )
        );
    }

    public boolean sendMonitoringAlertEmail(String toEmail, String summary, java.util.List<String> issues) {
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false); skipping monitoring alert");
            return false;
        }
        String body = "Testkauf monitoring alert\n\nSummary: " + summary + "\n\nIssues:\n- "
                + String.join("\n- ", issues) + "\n";
        MimeMessage mime = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setTo(toEmail);
            if (!mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setSubject("testkauf monitoring alert");
            helper.setText(body, false);
            javaMailSender.send(mime);
            log.info("Monitoring alert mail sent to {}", toEmail);
            return true;
        } catch (org.springframework.mail.MailException e) {
            log.warn("Monitoring alert mail send failed: {}", e.getMostSpecificCause().getMessage());
            return false;
        } catch (MessagingException e) {
            log.warn("Monitoring alert mail build failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendContactInquiry(String recipient,
                                      String replyToEmail,
                                      String name,
                                      String company,
                                      String message) {
        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false); skipping contact inquiry");
            return false;
        }
        String body = """
                New testkauf contact form submission

                Name: %s
                Company: %s
                Email: %s

                Message:
                %s
                """.formatted(name.trim(), company.trim(), replyToEmail.trim(), message.trim());

        MimeMessage mime = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setTo(recipient);
            if (!mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setReplyTo(replyToEmail.trim());
            helper.setSubject("testkauf contact: " + name.trim());
            helper.setText(body, false);
            javaMailSender.send(mime);
            log.info("Contact inquiry mail sent to {}", recipient);
            return true;
        } catch (org.springframework.mail.MailException e) {
            log.warn("Contact mail send failed: {}", e.getMostSpecificCause().getMessage());
            return false;
        } catch (MessagingException e) {
            log.warn("Contact mail build failed: {}", e.getMessage());
            return false;
        }
    }

    private void sendHtmlEmail(String to,
                               String subject,
                               String templateName,
                               Map<String, Object> variables) {

        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false); skipping '{}' email", templateName);
            return;
        }
        sendHtmlEmailSync(to, subject, templateName, variables);
    }

    private boolean sendHtmlEmailSync(String to,
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
            if (!mailFrom.isBlank()) helper.setFrom(mailFrom);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mime);
            log.info("Mail sent: template='{}'", templateName);
            return true;

        } catch (org.springframework.mail.MailException e) {
            log.warn("Mail send failed: template='{}', cause={}", templateName, e.getClass().getSimpleName() + ": " + e.getMostSpecificCause().getMessage());
            throw e;
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

}
