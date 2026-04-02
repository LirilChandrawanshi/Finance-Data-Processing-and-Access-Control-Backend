package com.zorvyn.finance.specification;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Factory class for {@link Specification} predicates used in dynamic filtering of
 * {@link FinancialRecord} queries.
 *
 * <p>Each static method returns a {@code Specification} that either applies a
 * predicate (when the filter value is non-null) or returns a no-op predicate
 * ({@code null}), which Spring Data JPA silently ignores when combined with
 * {@code Specification.where(...).and(...)}.
 *
 * <p>This approach is preferred over building JPQL strings dynamically or adding
 * multiple {@code Optional} parameters to repository methods — it composes cleanly
 * and is fully type-safe.
 */
public final class FinancialRecordSpecification {

    private FinancialRecordSpecification() { /* utility class */ }

    public static Specification<FinancialRecord> hasType(TransactionType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<FinancialRecord> hasCategory(String category) {
        return (root, query, cb) ->
                (category == null || category.isBlank())
                        ? null
                        : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<FinancialRecord> fromDate(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("transactionDate"), from);
    }

    public static Specification<FinancialRecord> toDate(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("transactionDate"), to);
    }
}
