package com.zorvyn.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {

    // ── Top-level KPIs ─────────────────────────────────────────────────────────
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    // ── Breakdowns ─────────────────────────────────────────────────────────────
    private List<CategorySummary> categoryTotals;
    private List<MonthlyTrend> monthlyTrends;

    // ── Feed ───────────────────────────────────────────────────────────────────
    private List<FinancialRecordResponse> recentActivity;
}
