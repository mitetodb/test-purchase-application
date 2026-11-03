package app.repository;

import app.model.entity.EntityNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EntityNumberSequenceRepository extends JpaRepository<EntityNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EntityNumberSequence s WHERE s.sequenceName = :name")
    Optional<EntityNumberSequence> findBySequenceNameForUpdate(@Param("name") String name);
}
