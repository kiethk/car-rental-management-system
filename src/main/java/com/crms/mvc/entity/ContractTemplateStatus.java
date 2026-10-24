package com.crms.mvc.entity;

/**
 * Lifecycle of a contract template version: DRAFT (editable) -> ACTIVE (used for new bookings) -> ARCHIVED.
 */
public enum ContractTemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
