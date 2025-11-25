package app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "items")
@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String productName;

    private Integer quantity;

    private Double unitPrice;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private TestPurchase purchase;
}
