package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.dto.response.CategorySummary;
import com.zorvyn.finance.dto.response.DashboardSummary;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.dto.response.MonthlyTrend;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.projection.MonthlyTrendProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregation service for the finance dashboard.
 *
 * <h3>Aggregation strategy</h3>
 * <p>All {@code SUM} and {@code GROUP BY} operations are executed at the database
 * level via JPQL queries in {@code FinancialRecordRepository}. This service only
 * performs lightweight in-memory pivoting (e.g., collapsing (month, INCOME) and
 * (month, EXPENSE) rows into a single {@link MonthlyTrend} object) — it never
 * fetches all records into Java just to reduce them with Streams.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final int MONTHLY_TREND_MONTHS  = 12;

    private final FinancialRecordRepository recordRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        log.debug("Building dashboard summary");

        BigDecimal totalIncome   = recordRepository.sumAmountByType(TransactionType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumAmountByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

        List<CategorySummary> categoryTotals = recordRepository.findCategoryTotals().stream()
                .map(p -> CategorySummary.builder()
                        .category(p.getCategory())
                        .type(p.getType())
                        .total(p.getTotal())
                        .build())
                .toList();

        List<FinancialRecord> recentRecords = recordRepository.findRecentActivity(
                PageRequest.of(0, RECENT_ACTIVITY_LIMIT));

        List<FinancialRecordResponse> recentActivity = recentRecords.stream()
                .map(this::toResponse)
                .toList();

        List<MonthlyTrend> monthlyTrends = buildMonthlyTrends(
                recordRepository.findMonthlyTrends(PageRequest.of(0, MONTHLY_TREND_MONTHS * 2)));

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .recentActivity(recentActivity)
                .monthlyTrends(monthlyTrends)
                .build();
    }

    /**
     * Pivots a flat list of (month, type, total) projections into one
     * {@link MonthlyTrend} per month containing separate income and expense totals.
     *
     * <p>Uses a {@link LinkedHashMap} to preserve the descending month order
     * returned by the repository query.
     */
    private List<MonthlyTrend> buildMonthlyTrends(List<MonthlyTrendProjection> projections) {
        Map<String, MonthlyTrend> trendMap = new LinkedHashMap<>();

        for (MonthlyTrendProjection p : projections) {
            trendMap.computeIfAbsent(p.getMonth(), m -> MonthlyTrend.builder().month(m).build());
            MonthlyTrend trend = trendMap.get(p.getMonth());

            if (p.getType() == TransactionType.INCOME) {
                trend.setIncome(p.getTotal());
            } else {
                trend.setExpense(p.getTotal());
            }
        }

        return trendMap.values().stream().toList();
    }

    private FinancialRecordResponse toResponse(FinancialRecord r) {
        return FinancialRecordResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .transactionDate(r.getTransactionDate())
                .notes(r.getNotes())
                .createdBy(r.getCreatedBy().getFullName())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
