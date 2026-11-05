package app.model.entity;

import app.model.enums.TestPurchaseStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TestPurchaseDeadlineEntityTest {

    @Test
    void deadlineAtCanBeSetOnEntity() {
        TestPurchase purchase = TestPurchase.builder()
                .number("TP-1001")
                .status(TestPurchaseStatus.INITIALISED)
                .deadlineAt(LocalDate.of(2026, 12, 1))
                .build();

        assertThat(purchase.getDeadlineAt()).isEqualTo(LocalDate.of(2026, 12, 1));
    }
}
