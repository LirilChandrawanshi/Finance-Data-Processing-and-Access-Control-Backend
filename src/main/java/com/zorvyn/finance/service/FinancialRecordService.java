package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.request.UpdateFinancialRecordRequest;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.dto.response.PageResponse;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.specification.FinancialRecordSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;

    // ── Create ─────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialRecordResponse createRecord(CreateFinancialRecordRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorEmail));

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().strip())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .createdBy(creator)
                .build();

        FinancialRecord saved = recordRepository.save(record);
        log.info("Financial record created: id={}, type={}, amount={}", saved.getId(), saved.getType(), saved.getAmount());
        return toResponse(saved);
    }

    // ── Read (paginated + filtered) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<FinancialRecordResponse> getRecords(
            TransactionType type,
            String category,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        Specification<FinancialRecord> spec = Specification
                .where(FinancialRecordSpecification.hasType(type))
                .and(FinancialRecordSpecification.hasCategory(category))
                .and(FinancialRecordSpecification.fromDate(from))
                .and(FinancialRecordSpecification.toDate(to));

        Page<FinancialRecordResponse> page = recordRepository
                .findAll(spec, pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    // ── Read (single) ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        return recordRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, UpdateFinancialRecordRequest request) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));

        // Apply only non-null fields (partial update / PATCH semantics via PUT)
        if (request.getAmount() != null)          record.setAmount(request.getAmount());
        if (request.getType() != null)             record.setType(request.getType());
        if (request.getCategory() != null)         record.setCategory(request.getCategory().strip());
        if (request.getTransactionDate() != null)  record.setTransactionDate(request.getTransactionDate());
        if (request.getNotes() != null)            record.setNotes(request.getNotes());

        log.info("Financial record updated: id={}", id);
        return toResponse(recordRepository.save(record));
    }

    // ── Soft Delete ────────────────────────────────────────────────────────────

    /**
     * Soft-deletes the record by setting {@code deletedAt}.
     *
     * <p>After this call, the record will be invisible to all subsequent queries
     * because the {@code @SQLRestriction("deleted_at IS NULL")} on the entity
     * automatically filters it out. The data is preserved in the database for
     * audit purposes.
     */
    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));

        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);
        log.info("Financial record soft-deleted: id={}", id);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

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
