package app.service;

import app.model.entity.EntityNumberSequence;
import app.model.enums.EntityNumberSequenceName;
import app.repository.EntityNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntityNumberAllocator {

    private final EntityNumberSequenceRepository entityNumberSequenceRepository;

    @Transactional
    public String nextCustomerNumber() {
        long seq = nextSequenceValue(EntityNumberSequenceName.CUSTOMER);
        return String.format("%04d", seq);
    }

    @Transactional
    public String nextShopNumber() {
        long seq = nextSequenceValue(EntityNumberSequenceName.SHOP);
        return String.format("%04d", seq);
    }

    @Transactional
    public String nextTestPurchaseNumber() {
        long seq = nextSequenceValue(EntityNumberSequenceName.TEST_PURCHASE);
        return "TP-" + String.format("%04d", seq);
    }

    long nextSequenceValue(EntityNumberSequenceName name) {
        EntityNumberSequence row = entityNumberSequenceRepository
                .findBySequenceNameForUpdate(name.name())
                .orElseThrow(() -> new IllegalStateException("Missing entity number sequence: " + name));
        long next = row.getLastValue() + 1;
        row.setLastValue(next);
        entityNumberSequenceRepository.save(row);
        return next;
    }
}
