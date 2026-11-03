package app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import app.repository.LoginRateLimitRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.mail.enabled=true",
        "app.mail.from=test@example.com",
        "app.contact.recipient=mail@getinweb.de"
})
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private app.client.PricingClient pricingClient;

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private LoginRateLimitRepository loginRateLimitRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearRateLimits() {
        transactionTemplate.executeWithoutResult(status -> loginRateLimitRepository.deleteAll());
        when(javaMailSender.createMimeMessage()).thenAnswer(invocation -> new jakarta.mail.internet.MimeMessage(
                (jakarta.mail.Session) null));
    }

    @Test
    void honeypotSubmissionIsSilentlyAccepted() throws Exception {
        long ts = System.currentTimeMillis() - 5000L;

        mockMvc.perform(post("/contact")
                        .with(csrf())
                        .param("name", "Bot")
                        .param("company", "Spam Inc")
                        .param("email", "bot@example.com")
                        .param("message", "Hello")
                        .param("privacy", "on")
                        .param("website", "http://spam.example")
                        .param("ts", String.valueOf(ts)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?sent=1#kontakt"));

        verify(javaMailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void validSubmissionSendsMail() throws Exception {
        long ts = System.currentTimeMillis() - 5000L;

        mockMvc.perform(post("/contact")
                        .with(csrf())
                        .param("name", "Jane Doe")
                        .param("company", "Demo GmbH")
                        .param("email", "jane@example.com")
                        .param("message", "Please enable testkauf access.")
                        .param("privacy", "on")
                        .param("ts", String.valueOf(ts)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?sent=1#kontakt"));

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void missingCsrfIsRejected() throws Exception {
        long ts = System.currentTimeMillis() - 5000L;

        mockMvc.perform(post("/contact")
                        .param("name", "Jane Doe")
                        .param("company", "Demo GmbH")
                        .param("email", "jane@example.com")
                        .param("message", "Hello")
                        .param("privacy", "on")
                        .param("ts", String.valueOf(ts)))
                .andExpect(status().isForbidden());

        verify(javaMailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}
