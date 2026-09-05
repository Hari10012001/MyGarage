package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * M21: Details of an individual detected analytical rework incident.
 */
public record WorkshopReworkEventDTO(
        Long originalServiceId,
        LocalDate originalServiceDate,
        String originalServiceType,
        Long reworkServiceId,
        LocalDate reworkServiceDate,
        String reworkServiceType,
        int daysBetweenServices,
        Integer kmBetweenServices,
        BigDecimal reworkCost,
        String vehiclePlate
) {}
