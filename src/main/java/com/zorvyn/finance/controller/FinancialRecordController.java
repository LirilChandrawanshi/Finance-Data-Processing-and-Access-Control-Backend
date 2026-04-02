package com.zorvyn.finance.controller;

import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.request.UpdateFinancialRecordRequest;
import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.dto.response.PageResponse;
import com.zorvyn.finance.service.FinancialRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Financial records CRUD with role-based access control.
 *
 * <h3>Access matrix</h3>
 * <table border="1">
 *   <tr><th>Action</th>        <th>VIEWER</th><th>ANALYST</th><th>ADMIN</th></tr>
 *   <tr><td>List / Get</td>    <td>✗</td>      <td>✓</td>      <td>✓</td></tr>
 *   <tr><td>Create</td>        <td>✗</td>      <td>✗</td>      <td>✓</td></tr>
 *   <tr><td>Update / Delete</td><td>✗</td>     <td>✗</td>      <td>✓</td></tr>
 * </table>
 *
 * <p>VIEWERs may only access the dashboard summary endpoint — they cannot see
 * individual transaction records by design.
 */
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Slf4j
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    /**
     * Create a new financial record.
     *
     * <p>The creator is resolved from the JWT token — no need to pass a user ID
     * in the request body, preventing impersonation.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> createRecord(
            @Valid @RequestBody CreateFinancialRecordRequest request,
            Authentication authentication) {

        log.info("Creating record: type={}, amount={}, by={}", request.getType(), request.getAmount(), authentication.getName());
        FinancialRecordResponse response = recordService.createRecord(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * List financial records with optional filters and pagination.
     *
     * <p>All filter parameters are optional and combinable. The default page size
     * is 20 records, sorted by {@code transactionDate} descending.
     *
     * @param type     filter by {@code INCOME} or {@code EXPENSE}
     * @param category filter by exact category name (case-insensitive)
     * @param from     lower bound of transaction date (inclusive, ISO-8601)
     * @param to       upper bound of transaction date (inclusive, ISO-8601)
     * @param pageable pagination and sort parameters via query strings
     *                 ({@code ?page=0&size=20&sort=transactionDate,desc})
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<PageResponse<FinancialRecordResponse>>> getRecords(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<FinancialRecordResponse> result = recordService.getRecords(type, category, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Fetch a single record by ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recordService.getRecordById(id)));
    }

    /**
     * Partially update a financial record.
     *
     * <p>Only fields included in the request body are updated.
     * Omitted fields retain their current values.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFinancialRecordRequest request) {

        log.info("Updating record id={}", id);
        return ResponseEntity.ok(ApiResponse.success(recordService.updateRecord(id, request)));
    }

    /**
     * Soft-delete a financial record.
     *
     * <p>The record is not physically removed from the database; it is hidden by
     * setting {@code deleted_at} to the current timestamp. This preserves an audit
     * trail while keeping the record out of all subsequent queries.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        log.info("Soft-deleting record id={}", id);
        recordService.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
