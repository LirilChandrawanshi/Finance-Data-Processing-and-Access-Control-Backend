package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.dto.response.DashboardSummary;
import com.zorvyn.finance.dto.response.MonthlyTrend;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.projection.CategorySummaryProjection;
import com.zorvyn.finance.repository.projection.MonthlyTrendProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService unit tests")
class DashboardServiceTest {

    @Mock private FinancialRecordRepository recordRepository;

    @InjectMocks private DashboardService dashboardService;

    // ── Net balance calculation ────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardSummary: should correctly compute netBalance = income - expenses")
    void getDashboardSummary_correctNetBalanceCalculation() {
        when(recordRepository.sumAmountByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("15000.00"));
        when(recordRepository.sumAmountByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("9750.50"));
        when(recordRepository.findCategoryTotals()).thenReturn(Collections.emptyList());
        when(recordRepository.findRecentActivity(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(recordRepository.findMonthlyTrends(any(Pageable.class))).thenReturn(Collections.emptyList());

        DashboardSummary summary = dashboardService.getDashboardSummary();

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("15000.00");
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("9750.50");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("5249.50");
    }

    // ── Monthly trend pivoting ─────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardSummary: should pivot (month, type) rows into single MonthlyTrend per month")
    void getDashboardSummary_monthlyTrendPivot_groupsByMonth() {
        List<MonthlyTrendProjection> projections = List.of(
                buildTrendProjection("2024-03", TransactionType.INCOME,  new BigDecimal("5000")),
                buildTrendProjection("2024-03", TransactionType.EXPENSE, new BigDecimal("2000")),
                buildTrendProjection("2024-02", TransactionType.INCOME,  new BigDecimal("4500")),
                buildTrendProjection("2024-02", TransactionType.EXPENSE, new BigDecimal("3100"))
        );

        when(recordRepository.sumAmountByType(TransactionType.INCOME)).thenReturn(BigDecimal.ZERO);
        when(recordRepository.sumAmountByType(TransactionType.EXPENSE)).thenReturn(BigDecimal.ZERO);
        when(recordRepository.findCategoryTotals()).thenReturn(Collections.emptyList());
        when(recordRepository.findRecentActivity(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(recordRepository.findMonthlyTrends(any(Pageable.class))).thenReturn(projections);

        DashboardSummary summary = dashboardService.getDashboardSummary();
        List<MonthlyTrend> trends = summary.getMonthlyTrends();

        // 4 projection rows → 2 MonthlyTrend objects (one per month)
        assertThat(trends).hasSize(2);

        MonthlyTrend march = trends.stream().filter(t -> "2024-03".equals(t.getMonth())).findFirst().orElseThrow();
        assertThat(march.getIncome()).isEqualByComparingTo("5000");
        assertThat(march.getExpense()).isEqualByComparingTo("2000");

        MonthlyTrend february = trends.stream().filter(t -> "2024-02".equals(t.getMonth())).findFirst().orElseThrow();
        assertThat(february.getIncome()).isEqualByComparingTo("4500");
        assertThat(february.getExpense()).isEqualByComparingTo("3100");
    }

    @Test
    @DisplayName("getDashboardSummary: should return zero totals when no records exist")
    void getDashboardSummary_emptyDatabase_returnsZeroTotals() {
        when(recordRepository.sumAmountByType(any())).thenReturn(BigDecimal.ZERO);
        when(recordRepository.findCategoryTotals()).thenReturn(Collections.emptyList());
        when(recordRepository.findRecentActivity(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(recordRepository.findMonthlyTrends(any(Pageable.class))).thenReturn(Collections.emptyList());

        DashboardSummary summary = dashboardService.getDashboardSummary();

        assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getCategoryTotals()).isEmpty();
        assertThat(summary.getMonthlyTrends()).isEmpty();
        assertThat(summary.getRecentActivity()).isEmpty();
    }

    // ── Helper: anonymous projection implementation ────────────────────────────

    private MonthlyTrendProjection buildTrendProjection(String month, TransactionType type, BigDecimal total) {
        return new MonthlyTrendProjection() {
            @Override public String getMonth()          { return month; }
            @Override public TransactionType getType()  { return type;  }
            @Override public BigDecimal getTotal()      { return total; }
        };
    }

    private CategorySummaryProjection buildCategoryProjection(String category, TransactionType type, BigDecimal total) {
        return new CategorySummaryProjection() {
            @Override public String getCategory()       { return category; }
            @Override public TransactionType getType()  { return type;     }
            @Override public BigDecimal getTotal()      { return total;    }
        };
    }
}
