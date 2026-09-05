package com.mygarage.service;

import com.mygarage.dto.response.GarageWorkshopEcosystemMatrixDTO;
import com.mygarage.dto.response.WorkshopAnalyticsReportDTO;
import com.mygarage.model.User;

/**
 * M21: Service interface for Service Center Ecosystem,
 * Workshop Benchmarking & Labor/Parts Cost Intelligence Engine.
 */
public interface WorkshopAnalyticsService {

    /**
     * Generate portfolio-wide workshop ecosystem matrix and vendor concentration report.
     *
     * @param user authenticated user
     * @return GarageWorkshopEcosystemMatrixDTO
     */
    GarageWorkshopEcosystemMatrixDTO getGarageWorkshopEcosystem(User user);

    /**
     * Overload for getting workshop ecosystem by email.
     *
     * @param userEmail authenticated user email
     * @return GarageWorkshopEcosystemMatrixDTO
     */
    GarageWorkshopEcosystemMatrixDTO getGarageWorkshopEcosystem(String userEmail);

    /**
     * Generate detailed single-workshop intelligence report.
     *
     * @param user authenticated user
     * @param workshopKey normalized workshop key
     * @return WorkshopAnalyticsReportDTO
     */
    WorkshopAnalyticsReportDTO getWorkshopDetail(User user, String workshopKey);

    /**
     * Overload for getting workshop detail by email.
     *
     * @param userEmail authenticated user email
     * @param workshopKey normalized workshop key
     * @return WorkshopAnalyticsReportDTO
     */
    WorkshopAnalyticsReportDTO getWorkshopDetail(String userEmail, String workshopKey);
}
