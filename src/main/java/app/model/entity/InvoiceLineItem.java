package app.model.entity;

import app.model.enums.InvoiceLineType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invoice_line_items")
@Entity
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_purchase_id")
    private TestPurchase testPurchase;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 32)
    private InvoiceLineType lineType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_amount", nullable = false)
    private double unitAmount;

    @Column(name = "line_total", nullable = false)
    private double lineTotal;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
