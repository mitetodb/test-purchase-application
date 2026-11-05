package app.service;

import app.config.DeadlineReminderProperties;
import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Role;
import app.model.enums.TestPurchaseStatus;
import app.repository.TestPurchaseDeadlineReminderRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderServiceTest {

    @Mock
    private TestPurchaseRepository testPurchaseRepository;

    @Mock
    private TestPurchaseDeadlineReminderRepository reminderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private DeadlineReminderProperties properties;

    private DeadlineReminderService deadlineReminderService;

    private UUID purchaseId;
    private Customer customer;
    private TestPurchase purchase;

    @BeforeEach
    void setUp() {
        properties = new DeadlineReminderProperties();
        properties.setEnabled(true);
        properties.setDaysBefore(List.of(1, 0));
        deadlineReminderService = new DeadlineReminderService(
                testPurchaseRepository,
                reminderRepository,
                userRepository,
                emailService,
                properties
        );
        ReflectionTestUtils.setField(deadlineReminderService, "appBaseUrl", "http://localhost:8080");

        purchaseId = UUID.randomUUID();
        customer = Customer.builder().id(UUID.randomUUID()).name("Demo").build();
        purchase = TestPurchase.builder()
                .id(purchaseId)
                .number("TP-2001")
                .status(TestPurchaseStatus.IN_PROGRESS)
                .customer(customer)
                .deadlineAt(LocalDate.of(2026, 6, 10))
                .build();
    }

    @Test
    void processDueRemindersSkipsWhenTriggerDateNotReached() {
        when(testPurchaseRepository.findActiveWithDeadline()).thenReturn(List.of(purchase));

        int sent = deadlineReminderService.processDueReminders(LocalDate.of(2026, 6, 1));

        assertThat(sent).isZero();
        verify(emailService, never()).sendDeadlineReminderEmail(any(), any(), any(), anyInt(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void processDueRemindersSendsAndRecordsReminder() {
        when(testPurchaseRepository.findActiveWithDeadline()).thenReturn(List.of(purchase));
        when(reminderRepository.existsByTestPurchase_IdAndDaysBeforeOffset(purchaseId, 1)).thenReturn(false);
        when(userRepository.findActiveAccountManagersForCustomer(customer.getId())).thenReturn(List.of());
        when(userRepository.findByRoleAndActive(Role.ADMIN, true)).thenReturn(List.of(
                User.builder().email("admin@example.com").active(true).role(Role.ADMIN).build()
        ));
        when(emailService.sendDeadlineReminderEmail(
                eq("admin@example.com"), eq("TP-2001"), eq(LocalDate.of(2026, 6, 10)), eq(1), anyString()))
                .thenReturn(true);

        int sent = deadlineReminderService.processDueReminders(LocalDate.of(2026, 6, 9));

        assertThat(sent).isEqualTo(1);
        verify(reminderRepository).save(any());
    }

    @Test
    void processDueRemindersDoesNotResendWhenAlreadyRecorded() {
        when(testPurchaseRepository.findActiveWithDeadline()).thenReturn(List.of(purchase));
        when(reminderRepository.existsByTestPurchase_IdAndDaysBeforeOffset(purchaseId, 1)).thenReturn(true);

        int sent = deadlineReminderService.processDueReminders(LocalDate.of(2026, 6, 9));

        assertThat(sent).isZero();
        verify(emailService, never()).sendDeadlineReminderEmail(any(), any(), any(), anyInt(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void resolveRecipientEmailsIncludesMysteryShopperAccountManagerAndAdmin() {
        User shopper = User.builder().email("shopper@example.com").active(true).role(Role.MYSTERY_SHOPPER).build();
        purchase.setMysteryShopper(shopper);

        when(userRepository.findActiveAccountManagersForCustomer(customer.getId()))
                .thenReturn(List.of(User.builder().email("am@example.com").active(true).role(Role.ACCOUNT_MANAGER).build()));
        when(userRepository.findByRoleAndActive(Role.ADMIN, true))
                .thenReturn(List.of(User.builder().email("admin@example.com").active(true).role(Role.ADMIN).build()));

        Set<String> emails = deadlineReminderService.resolveRecipientEmails(purchase);

        assertThat(emails).containsExactlyInAnyOrder(
                "shopper@example.com", "am@example.com", "admin@example.com");
    }

    @Test
    void viewDtoMarksOverdueForOpenPurchasesPastDeadline() {
        TestPurchase tp = TestPurchase.builder()
                .status(TestPurchaseStatus.IN_PROGRESS)
                .deadlineAt(LocalDate.now().minusDays(1))
                .productPrice(10.0)
                .serviceFee(0.0)
                .postageFee(0.0)
                .build();

        TestPurchaseViewDTO dto = TestPurchaseViewDTO.from(tp, false);

        assertThat(dto.isOverdue()).isTrue();
    }
}
