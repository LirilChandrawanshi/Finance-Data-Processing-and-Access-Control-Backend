package com.zorvyn.finance.dto.request;

import com.zorvyn.finance.domain.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial-update DTO — all fields are optional.
 * Only non-null fields will be applied to the existing record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFinancialRecordRequest {

    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    private TransactionType type;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
