package com.mygarage.model.enums;

/**
 * Maintenance record status values.
 * Computed dynamically based on scheduledDate and completedDate.
 *
 * Logic:
 *   if completedDate != null          -> COMPLETED
 *   else if scheduledDate < today     -> OVERDUE
 *   else if scheduledDate == today    -> DUE_TODAY
 *   else                              -> UPCOMING
 */
public enum MaintenanceStatus {
    UPCOMING,
    DUE_TODAY,
    OVERDUE,
    COMPLETED
}
