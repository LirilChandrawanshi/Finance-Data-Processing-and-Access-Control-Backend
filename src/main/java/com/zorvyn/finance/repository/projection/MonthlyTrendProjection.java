package com.zorvyn.finance.repository.projection;

import com.zorvyn.finance.domain.enums.TransactionType;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for monthly trend aggregation.
 *
 * <p>Each row represents one (month, type) combination.
 * {@code DashboardService} pivots multiple rows for the same month into a
 * single {@code MonthlyTrend} response object with separate income/expense fields.
 */
public interface MonthlyTrendProjection {
    /** ISO year-month string, e.g. {@code "2024-03"}. */
    String getMonth();
    TransactionType getType();
    BigDecimal getTotal();
}
