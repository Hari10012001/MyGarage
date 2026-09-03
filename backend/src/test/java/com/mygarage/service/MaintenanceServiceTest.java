package com.mygarage.service;

import com.mygarage.model.enums.MaintenanceStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MaintenanceService status logic.
 * These tests verify the core business logic WITHOUT hitting the database.
 */
class MaintenanceServiceTest {

    @Test
    void testStatusIsCompleted_whenCompletedDateIsSet() {
        LocalDate scheduled = LocalDate.now().minusDays(5);
        LocalDate completed = LocalDate.now().minusDays(1);
        assertEquals(MaintenanceStatus.COMPLETED, MaintenanceService.computeStatus(scheduled, completed));
    }

    @Test
    void testStatusIsOverdue_whenScheduledBeforeToday() {
        LocalDate scheduled = LocalDate.now().minusDays(3);
        assertEquals(MaintenanceStatus.OVERDUE, MaintenanceService.computeStatus(scheduled, null));
    }

    @Test
    void testStatusIsDueToday_whenScheduledIsToday() {
        LocalDate scheduled = LocalDate.now();
        assertEquals(MaintenanceStatus.DUE_TODAY, MaintenanceService.computeStatus(scheduled, null));
    }

    @Test
    void testStatusIsUpcoming_whenScheduledAfterToday() {
        LocalDate scheduled = LocalDate.now().plusDays(7);
        assertEquals(MaintenanceStatus.UPCOMING, MaintenanceService.computeStatus(scheduled, null));
    }

    @Test
    void testCompletedTakesPriorityOverOverdue() {
        // Even if scheduled was in the past, completed date means COMPLETED
        LocalDate scheduled = LocalDate.now().minusDays(10);
        LocalDate completed = LocalDate.now().minusDays(2);
        assertEquals(MaintenanceStatus.COMPLETED, MaintenanceService.computeStatus(scheduled, completed));
    }
}
