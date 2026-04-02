package com.zorvyn.finance.repository.projection;

import com.zorvyn.finance.domain.enums.TransactionType;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection interface for category-level aggregation results.
 *
 * <p>The property names must exactly match the JPQL aliases defined in
 * {@code FinancialRecordRepository#findCategoryTotals()}.
 */
public interface CategorySummaryProjection {
    String getCategory();
    TransactionType getType();
    BigDecimal getTotal();
}
