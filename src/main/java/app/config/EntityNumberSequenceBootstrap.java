package app.config;

import app.model.entity.EntityNumberSequence;
import app.model.enums.EntityNumberSequenceName;
import app.repository.EntityNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds counter rows when Flyway is disabled (H2 unit/integration tests with ddl-auto=create-drop).
 */
@Component
@RequiredArgsConstructor
public class EntityNumberSequenceBootstrap {

    private final EntityNumberSequenceRepository entityNumberSequenceRepository;
    private final Environment environment;

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void seedWhenFlywayDisabled() {
        if (environment.getProperty("spring.flyway.enabled", Boolean.class, true)) {
            return;
        }
        seedIfAbsent(EntityNumberSequenceName.CUSTOMER, 0L);
        seedIfAbsent(EntityNumberSequenceName.SHOP, 0L);
        seedIfAbsent(EntityNumberSequenceName.TEST_PURCHASE, 1000L);
    }

    private void seedIfAbsent(EntityNumberSequenceName name, long defaultLastUsedValue) {
        if (entityNumberSequenceRepository.existsById(name.name())) {
            return;
        }
        entityNumberSequenceRepository.save(
                new EntityNumberSequence(name.name(), defaultLastUsedValue));
    }
}
