package com.datacom.audit.domain;

import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.StatusTransition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_entry")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private ProductStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, updatable = false)
    private ProductStatus toStatus;

    @Column(length = 1000, updatable = false)
    private String comment;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEntry() {
        // JPA
    }

    public AuditEntry(
            Long productId,
            Long userId,
            AuditAction action,
            StatusTransition transition,
            String comment,
            Instant occurredAt) {
        this.productId = productId;
        this.userId = userId;
        this.action = action;
        this.fromStatus = transition.from();
        this.toStatus = transition.to();
        this.comment = comment;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public AuditAction getAction() {
        return action;
    }

    public ProductStatus getFromStatus() {
        return fromStatus;
    }

    public ProductStatus getToStatus() {
        return toStatus;
    }

    public String getComment() {
        return comment;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
