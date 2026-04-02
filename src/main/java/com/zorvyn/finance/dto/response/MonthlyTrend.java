package com.zorvyn.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Pivot of monthly trend data. The repository returns one row per (month, type)
 * combination; the service layer merges those rows so the frontend receives one
 * object per month with both income and expense totals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyTrend {

    /** ISO year-month format, e.g. {@code "2024-03"}. */
    private String month;

    @Builder.Default
    private BigDecimal income = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal expense = BigDecimal.ZERO;
}
