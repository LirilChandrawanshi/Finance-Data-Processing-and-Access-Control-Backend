package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.TransactionType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.request.UpdateFinancialRecordRequest;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialRecordService unit tests")
class FinancialRecordServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FinancialRecordService recordService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@zorvyn.com")
                .fullName("Zorvyn Admin")
                .role(RoleType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ── createRecord ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRecord: should persist and return FinancialRecordResponse for valid input")
    void createRecord_validInput_returnsResponse() {
        CreateFinancialRecordRequest request = CreateFinancialRecordRequest.builder()
                .amount(new BigDecimal("2500.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .notes("March salary")
                .build();

        FinancialRecord persisted = FinancialRecord.builder()
                .id(10L)
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .createdBy(adminUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(persisted);

        FinancialRecordResponse response = recordService.createRecord(request, adminUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("2500.00");
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(response.getCategory()).isEqualTo("Salary");
        assertThat(response.getCreatedBy()).isEqualTo("Zorvyn Admin");

        verify(recordRepository, times(1)).save(any(FinancialRecord.class));
    }

    @Test
    @DisplayName("createRecord: should throw ResourceNotFoundException when creator email not found")
    void createRecord_unknownCreatorEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

        CreateFinancialRecordRequest request = CreateFinancialRecordRequest.builder()
                .amount(BigDecimal.TEN)
                .type(TransactionType.EXPENSE)
                .category("Food")
                .transactionDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> recordService.createRecord(request, "ghost@nowhere.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost@nowhere.com");

        verify(recordRepository, never()).save(any());
    }

    // ── deleteRecord (soft delete) ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteRecord: should set deletedAt timestamp (soft delete) and call save")
    void deleteRecord_existingRecord_setsDeletedAt() {
        FinancialRecord record = FinancialRecord.builder()
                .id(5L)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category("Travel")
                .transactionDate(LocalDate.now())
                .createdBy(adminUser)
                .deletedAt(null)
                .build();

        when(recordRepository.findById(5L)).thenReturn(Optional.of(record));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(record);

        recordService.deleteRecord(5L);

        assertThat(record.getDeletedAt())
                .as("deletedAt should be set after soft delete")
                .isNotNull()
                .isBeforeOrEqualTo(LocalDateTime.now());

        verify(recordRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("deleteRecord: should throw ResourceNotFoundException for non-existent ID")
    void deleteRecord_nonExistentId_throwsResourceNotFoundException() {
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteRecord(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recordRepository, never()).save(any());
    }

    // ── getRecordById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecordById: should throw ResourceNotFoundException when record not found")
    void getRecordById_notFound_throwsException() {
        when(recordRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getRecordById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateRecord ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRecord: should apply only non-null fields from the request")
    void updateRecord_partialUpdate_appliesOnlyNonNullFields() {
        FinancialRecord existing = FinancialRecord.builder()
                .id(7L)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.INCOME)
                .category("Freelance")
                .transactionDate(LocalDate.of(2024, 1, 10))
                .notes("Old note")
                .createdBy(adminUser)
                .build();

        UpdateFinancialRecordRequest request = UpdateFinancialRecordRequest.builder()
                .amount(new BigDecimal("250.00"))
                // type, category, transactionDate, notes intentionally null
                .build();

        when(recordRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        FinancialRecordResponse response = recordService.updateRecord(7L, request);

        assertThat(response.getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);      // unchanged
        assertThat(response.getCategory()).isEqualTo("Freelance");             // unchanged
        assertThat(response.getNotes()).isEqualTo("Old note");                 // unchanged
    }
}
