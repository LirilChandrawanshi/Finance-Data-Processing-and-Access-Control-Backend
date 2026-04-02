package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.DashboardSummary;
import com.zorvyn.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard analytics endpoint — accessible by all authenticated roles.
 *
 * <p>This is the only endpoint that VIEWERs can access beyond the auth endpoints.
 * They receive aggregated, anonymised summaries rather than individual transaction records.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Returns a complete dashboard summary including:
     * <ul>
     *   <li>Total income, total expenses, and net balance (KPIs)</li>
     *   <li>Category-wise totals broken down by type</li>
     *   <li>Monthly income vs expense trends for the last 12 months</li>
     *   <li>10 most recent transaction entries</li>
     * </ul>
     *
     * <p>All numeric aggregations are computed at the database level.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard() {
        log.debug("Dashboard summary requested");
        DashboardSummary summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
