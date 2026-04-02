package com.zorvyn.finance.repository;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.repository.projection.CategorySummaryProjection;
import com.zorvyn.finance.repository.projection.MonthlyTrendProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for {@link FinancialRecord} providing both standard CRUD via
 * {@link JpaRepository} and dynamic filtering via {@link JpaSpecificationExecutor}.
 *
 * <h3>Key Design Decisions</h3>
 * <ul>
 *   <li>All aggregation queries ({@code SUM}, {@code GROUP BY}) are executed at the database
 *       level via JPQL — never by fetching all records into Java and using Streams.</li>
 *   <li>{@code COALESCE(..., 0)} guards against {@code NULL} when the table has no rows
 *       matching the filter, so service code never needs null-checks on sums.</li>
 *   <li>The {@code @SQLRestriction("deleted_at IS NULL")} on the entity class is
 *       automatically applied to all JPQL queries in this repository.</li>
 * </ul>
 */
@Repository
public interface FinancialRecordRepository
        extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {

    // ── Aggregation: totals ────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FinancialRecord f WHERE f.type = :type")
    BigDecimal sumAmountByType(@Param("type") TransactionType type);

    // ── Aggregation: category breakdown ───────────────────────────────────────

    @Query("""
        SELECT f.category AS category,
               f.type     AS type,
               COALESCE(SUM(f.amount), 0) AS total
        FROM   FinancialRecord f
        GROUP  BY f.category, f.type
        ORDER  BY total DESC
        """)
    List<CategorySummaryProjection> findCategoryTotals();

    // ── Aggregation: monthly trends (last 12 months) ──────────────────────────

    @Query("""
        SELECT FUNCTION('to_char', f.transactionDate, 'YYYY-MM') AS month,
               f.type AS type,
               COALESCE(SUM(f.amount), 0) AS total
        FROM   FinancialRecord f
        GROUP  BY FUNCTION('to_char', f.transactionDate, 'YYYY-MM'), f.type
        ORDER  BY month DESC
        """)
    List<MonthlyTrendProjection> findMonthlyTrends(Pageable pageable);

    // ── Recent activity ────────────────────────────────────────────────────────

    @Query("SELECT f FROM FinancialRecord f ORDER BY f.createdAt DESC")
    List<FinancialRecord> findRecentActivity(Pageable pageable);
}
