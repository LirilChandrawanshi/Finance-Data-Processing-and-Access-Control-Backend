package com.zorvyn.finance.dto.response;

import com.zorvyn.finance.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummary {

    private String category;
    private TransactionType type;
    private BigDecimal total;
}
