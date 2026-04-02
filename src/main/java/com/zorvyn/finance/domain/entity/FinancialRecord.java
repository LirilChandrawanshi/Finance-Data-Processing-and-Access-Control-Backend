package com.zorvyn.finance.domain.entity;

import com.zorvyn.finance.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core financial record entity representing a single income or expense entry.
 *
 * <h3>Soft Delete Strategy</h3>
 * <p>{@code @SQLRestriction("deleted_at IS NULL")} instructs Hibernate to append
 * {@code AND deleted_at IS NULL} to every generated SQL query for this entity.
 * Deleting a record therefore means setting {@code deletedAt} to the current timestamp
 * and saving — the record is then invisible to all repository queries automatically.
 *
 * <h3>Indexing</h3>
 * <p>Composite indexes cover the most common dashboard filter patterns:
 * filtering by type, category, and date range, which are the primary axes for
 * aggregation queries in {@code FinancialRecordRepository}.
 */
@Entity
@Table(
    name = "financial_records",
    indexes = {
        @Index(name = "idx_fr_type",     columnList = "type"),
        @Index(name = "idx_fr_category", columnList = "category"),
        @Index(name = "idx_fr_date",     columnList = "transaction_date"),
        @Index(name = "idx_fr_user",     columnList = "user_id"),
        @Index(name = "idx_fr_deleted",  columnList = "deleted_at")
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "createdBy")   // exclude to avoid triggering lazy-load in toString
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Stored with up to 4 decimal places to support micro-currency precision. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** The user who created this record. LAZY-loaded to avoid N+1 on list queries. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Null means the record is active. Non-null means it has been soft-deleted. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
