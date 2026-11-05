package app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceDeadlineReminderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(javaMailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailFrom", "noreply@example.com");
    }

    @Test
    void sendDeadlineReminderEmailInvokesJavaMailSender() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("deadline-reminder-email"), any())).thenReturn("<p>reminder</p>");

        emailService.sendDeadlineReminderEmail(
                "user@example.com",
                "TP-3001",
                LocalDate.of(2026, 7, 1),
                0,
                "http://localhost:8080/testpurchases/view/abc");

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendDeadlineReminderEmailSkipsWhenMailDisabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        emailService.sendDeadlineReminderEmail(
                "user@example.com",
                "TP-3001",
                LocalDate.of(2026, 7, 1),
                1,
                "http://localhost:8080/testpurchases/view/abc");

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}
