package com.zorvyn.finance.dto.response;

import com.zorvyn.finance.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecordResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String category;
    private LocalDate transactionDate;
    private String notes;

    /** Resolved from the creator's full name — we never expose User IDs in list contexts. */
    private String createdBy;
    private LocalDateTime createdAt;
}
