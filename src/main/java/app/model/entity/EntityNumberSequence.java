package app.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entity_number_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityNumberSequence {

    @Id
    @Column(name = "sequence_name", length = 64, nullable = false)
    private String sequenceName;

    @Column(name = "last_used_value", nullable = false)
    private long lastUsedValue;
}
