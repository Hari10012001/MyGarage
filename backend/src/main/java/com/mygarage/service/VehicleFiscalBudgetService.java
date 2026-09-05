package com.mygarage.service;

import com.mygarage.dto.response.GarageFiscalBudgetMatrixDTO;
import com.mygarage.dto.response.VehicleFiscalBudgetReportDTO;

/**
 * M20: Service interface for Vehicle Operational Budgeting,
 * Predictive Cash-Flow Forecast & Maintenance Expense Burn-Rate Engine.
 */
public interface VehicleFiscalBudgetService {

    /**
     * Generate comprehensive fiscal budget report for a single owned vehicle.
     *
     * @param vehicleId vehicle primary key
     * @param userEmail authenticated user email
     * @return VehicleFiscalBudgetReportDTO
     */
    VehicleFiscalBudgetReportDTO getVehicleFiscalBudgetReport(Long vehicleId, String userEmail);

    /**
     * Generate consolidated garage fleet fiscal budget matrix for all owned vehicles.
     *
     * @param userEmail authenticated user email
     * @return GarageFiscalBudgetMatrixDTO
     */
    GarageFiscalBudgetMatrixDTO getGarageFiscalBudgetMatrix(String userEmail);
}
