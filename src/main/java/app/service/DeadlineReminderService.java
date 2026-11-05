package app.service;

import app.config.DeadlineReminderProperties;
import app.model.entity.TestPurchase;
import app.model.entity.TestPurchaseDeadlineReminder;
import app.model.entity.User;
import app.model.enums.Role;
import app.repository.TestPurchaseDeadlineReminderRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {

    private final TestPurchaseRepository testPurchaseRepository;
    private final TestPurchaseDeadlineReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DeadlineReminderProperties properties;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public int processDueReminders() {
        return processDueReminders(LocalDate.now());
    }

    @Transactional
    public int processDueReminders(LocalDate today) {
        if (!properties.isEnabled()) {
            log.debug("Deadline reminders disabled (app.deadline-reminder.enabled=false)");
            return 0;
        }

        List<Integer> offsets = properties.getDaysBefore();
        if (offsets == null || offsets.isEmpty()) {
            return 0;
        }

        List<TestPurchase> purchases = testPurchaseRepository.findActiveWithDeadline();
        int sentCount = 0;

        for (TestPurchase purchase : purchases) {
            LocalDate deadline = purchase.getDeadlineAt();
            if (deadline == null) {
                continue;
            }
            for (int offset : offsets) {
                LocalDate triggerDate = deadline.minusDays(offset);
                if (today.isBefore(triggerDate)) {
                    continue;
                }
                if (reminderRepository.existsByTestPurchase_IdAndDaysBeforeOffset(purchase.getId(), offset)) {
                    continue;
                }

                Set<String> recipients = resolveRecipientEmails(purchase);
                if (recipients.isEmpty()) {
                    log.info("Skipping deadline reminder for {} offset {}: no recipients",
                            purchase.getNumber(), offset);
                    continue;
                }

                String purchaseUrl = appBaseUrl + "/testpurchases/view/" + purchase.getId();
                boolean anySent = false;
                for (String email : recipients) {
                    if (emailService.sendDeadlineReminderEmail(
                            email,
                            purchase.getNumber(),
                            deadline,
                            offset,
                            purchaseUrl)) {
                        anySent = true;
                    }
                }

                if (anySent) {
                    reminderRepository.save(TestPurchaseDeadlineReminder.builder()
                            .testPurchase(purchase)
                            .daysBeforeOffset(offset)
                            .sentAt(LocalDateTime.now())
                            .build());
                    sentCount++;
                }
            }
        }

        if (sentCount > 0) {
            log.info("Sent {} test purchase deadline reminder(s)", sentCount);
        }
        return sentCount;
    }

    Set<String> resolveRecipientEmails(TestPurchase purchase) {
        Set<String> emails = new LinkedHashSet<>();

        User mysteryShopper = purchase.getMysteryShopper();
        if (mysteryShopper != null && mysteryShopper.isActive()) {
            addEmail(emails, mysteryShopper.getEmail());
        }

        if (purchase.getCustomer() != null) {
            for (User accountManager : userRepository.findActiveAccountManagersForCustomer(
                    purchase.getCustomer().getId())) {
                addEmail(emails, accountManager.getEmail());
            }
        }

        for (User admin : userRepository.findByRoleAndActive(Role.ADMIN, true)) {
            addEmail(emails, admin.getEmail());
        }

        return emails;
    }

    private static void addEmail(Set<String> emails, String email) {
        if (email != null && !email.isBlank()) {
            emails.add(email.trim());
        }
    }
}
