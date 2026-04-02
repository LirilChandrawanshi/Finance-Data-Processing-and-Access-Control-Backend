package com.zorvyn.finance.domain.enums;

/**
 * Defines the three RBAC roles in the system.
 *
 * <ul>
 *   <li>{@code VIEWER}  — Read-only access to dashboard summaries.</li>
 *   <li>{@code ANALYST} — Can read financial records and access analytics.</li>
 *   <li>{@code ADMIN}   — Full management access: records, users, and configuration.</li>
 * </ul>
 *
 * Spring Security prefixes these with {@code ROLE_} (e.g. {@code ROLE_ADMIN})
 * when used inside {@code @PreAuthorize} expressions.
 */
public enum RoleType {
    VIEWER,
    ANALYST,
    ADMIN
}
