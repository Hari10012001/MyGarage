package com.mygarage.service;

import com.mygarage.dto.response.FleetComparisonReportDTO;
import com.mygarage.dto.response.FleetExpenseBreakdownDTO;
import com.mygarage.dto.response.VehicleComparisonDTO;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.repository.FuelRecordRepository;
import com.mygarage.repository.MaintenanceRecordRepository;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleComparisonServiceImpl implements VehicleComparisonService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public FleetComparisonReportDTO compareVehicles(List<Long> vehicleIds, Long userId) {
        if (vehicleIds == null || vehicleIds.size() < 2) {
            throw new IllegalArgumentException("Please select at least 2 vehicles to compare.");
        }
        if (vehicleIds.size() > 4) {
            throw new IllegalArgumentException("You can compare a maximum of 4 vehicles at a time.");
        }

        // Deduplicate while maintaining order
        List<Long> distinctIds = vehicleIds.stream().distinct().collect(Collectors.toList());
        if (distinctIds.size() < 2) {
            throw new IllegalArgumentException("Please select distinct vehicles to compare.");
        }

        List<VehicleComparisonDTO> comparedVehicles = new ArrayList<>();
        for (Long vId : distinctIds) {
            Vehicle vehicle = vehicleRepository.findById(vId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vId));

            // Strict two-tier ownership enforcement
            if (!vehicle.getUser().getUserId().equals(userId)) {
                log.warn("Unauthorized comparison attempt: user {} attempted to access vehicle {}", userId, vId);
                throw new AccessDeniedException("You do not have permission to view or compare vehicle ID: " + vId);
            }

            comparedVehicles.add(buildVehicleComparisonMetrics(vehicle));
        }

        // Compute Automated Performance Badges
        assignPerformanceBadges(comparedVehicles);

        // Compute Fleet Aggregates for the compared set
        BigDecimal fleetTotalSpend = BigDecimal.ZERO;
        double mileageSum = 0.0;
        int mileageCount = 0;
        BigDecimal totalRunningCostPerKm = BigDecimal.ZERO;
        int runningCostCount = 0;

        String mostEfficientPlate = null;
        String mostEconomicalPlate = null;
        String lowestMaintenancePlate = null;
        String workhorsePlate = null;

        for (VehicleComparisonDTO dto : comparedVehicles) {
            fleetTotalSpend = fleetTotalSpend.add(dto.getTotalOwnershipCost());

            if (dto.getAvgMileageKmpl() != null && dto.getAvgMileageKmpl() > 0) {
                mileageSum += dto.getAvgMileageKmpl();
                mileageCount++;
            }

            if (dto.getRunningCostPerKm() != null && dto.getRunningCostPerKm().compareTo(BigDecimal.ZERO) > 0) {
                totalRunningCostPerKm = totalRunningCostPerKm.add(dto.getRunningCostPerKm());
                runningCostCount++;
            }

            if (dto.isMostFuelEfficient()) mostEfficientPlate = dto.getPlateNumber();
            if (dto.isLowestCostPerKm()) mostEconomicalPlate = dto.getPlateNumber();
            if (dto.isLowestMaintenanceCost()) lowestMaintenancePlate = dto.getPlateNumber();
            if (dto.isFleetWorkhorse()) workhorsePlate = dto.getPlateNumber();
        }

        Double fleetAvgMileage = mileageCount > 0 ? Math.round((mileageSum / mileageCount) * 100.0) / 100.0 : null;
        BigDecimal fleetAvgRunningCost = runningCostCount > 0
                ? totalRunningCostPerKm.divide(BigDecimal.valueOf(runningCostCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return FleetComparisonReportDTO.builder()
                .comparedVehicleCount(comparedVehicles.size())
                .vehicles(comparedVehicles)
                .fleetTotalSpend(fleetTotalSpend)
                .fleetAverageMileage(fleetAvgMileage)
                .fleetAverageRunningCostPerKm(fleetAvgRunningCost)
                .mostEfficientVehiclePlate(mostEfficientPlate)
                .mostEconomicalVehiclePlate(mostEconomicalPlate)
                .lowestMaintenanceVehiclePlate(lowestMaintenancePlate)
                .fleetWorkhorsePlate(workhorsePlate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleComparisonDTO getVehicleComparisonMetrics(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (!vehicle.getUser().getUserId().equals(userId)) {
            log.warn("Unauthorized access: user {} attempted to access vehicle {}", userId, vehicleId);
            throw new AccessDeniedException("You do not have permission to view vehicle ID: " + vehicleId);
        }

        return buildVehicleComparisonMetrics(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public FleetExpenseBreakdownDTO getFleetExpenseBreakdown(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        BigDecimal totalService = serviceRecordRepository.sumCostByUserId(userId);
        if (totalService == null) totalService = BigDecimal.ZERO;

        BigDecimal totalFuel = fuelRecordRepository.sumTotalCostByUserId(userId);
        if (totalFuel == null) totalFuel = BigDecimal.ZERO;

        BigDecimal totalMaintenance = maintenanceRecordRepository.sumCostByUserId(userId);
        if (totalMaintenance == null) totalMaintenance = BigDecimal.ZERO;

        BigDecimal totalFleetSpend = totalService.add(totalFuel).add(totalMaintenance);

        Double servicePct = 0.0;
        Double fuelPct = 0.0;
        Double maintenancePct = 0.0;

        if (totalFleetSpend.compareTo(BigDecimal.ZERO) > 0) {
            servicePct = Math.round(totalService.doubleValue() / totalFleetSpend.doubleValue() * 1000.0) / 10.0;
            fuelPct = Math.round(totalFuel.doubleValue() / totalFleetSpend.doubleValue() * 1000.0) / 10.0;
            maintenancePct = Math.round(totalMaintenance.doubleValue() / totalFleetSpend.doubleValue() * 1000.0) / 10.0;
        }

        List<FleetExpenseBreakdownDTO.VehicleShareItemDTO> shares = new ArrayList<>();
        for (Vehicle v : vehicles) {
            BigDecimal vSvc = serviceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            if (vSvc == null) vSvc = BigDecimal.ZERO;
            BigDecimal vFuel = fuelRecordRepository.sumTotalCostByVehicleId(v.getVehicleId());
            if (vFuel == null) vFuel = BigDecimal.ZERO;
            BigDecimal vMaint = maintenanceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            if (vMaint == null) vMaint = BigDecimal.ZERO;

            BigDecimal vTotal = vSvc.add(vFuel).add(vMaint);
            Double sharePct = 0.0;
            if (totalFleetSpend.compareTo(BigDecimal.ZERO) > 0) {
                sharePct = Math.round(vTotal.doubleValue() / totalFleetSpend.doubleValue() * 1000.0) / 10.0;
            }

            shares.add(FleetExpenseBreakdownDTO.VehicleShareItemDTO.builder()
                    .vehicleId(v.getVehicleId())
                    .plateNumber(v.getPlateNumber())
                    .make(v.getMake())
                    .model(v.getModel())
                    .totalCost(vTotal)
                    .percentageOfFleetSpend(sharePct)
                    .build());
        }

        return FleetExpenseBreakdownDTO.builder()
                .totalVehicles(vehicles.size())
                .totalFleetSpend(totalFleetSpend)
                .totalServiceCost(totalService)
                .totalFuelCost(totalFuel)
                .totalMaintenanceCost(totalMaintenance)
                .serviceCostPercentage(servicePct)
                .fuelCostPercentage(fuelPct)
                .maintenanceCostPercentage(maintenancePct)
                .vehicleShares(shares)
                .build();
    }

    private VehicleComparisonDTO buildVehicleComparisonMetrics(Vehicle vehicle) {
        Long vId = vehicle.getVehicleId();

        BigDecimal serviceCost = serviceRecordRepository.sumCostByVehicleId(vId);
        if (serviceCost == null) serviceCost = BigDecimal.ZERO;

        BigDecimal fuelCost = fuelRecordRepository.sumTotalCostByVehicleId(vId);
        if (fuelCost == null) fuelCost = BigDecimal.ZERO;

        BigDecimal maintenanceCost = maintenanceRecordRepository.sumCostByVehicleId(vId);
        if (maintenanceCost == null) maintenanceCost = BigDecimal.ZERO;

        BigDecimal totalOwnershipCost = serviceCost.add(fuelCost).add(maintenanceCost);

        Double avgMileage = fuelRecordRepository.avgEstimatedMileageByVehicleId(vId);
        if (avgMileage != null) {
            avgMileage = Math.round(avgMileage * 100.0) / 100.0;
        }

        BigDecimal totalFuelLitres = fuelRecordRepository.sumQuantityLitresByVehicleId(vId);
        if (totalFuelLitres == null) totalFuelLitres = BigDecimal.ZERO;

        Integer odometer = vehicle.getCurrentOdometer();
        BigDecimal runningCostPerKm = BigDecimal.ZERO;
        BigDecimal fuelCostPerKm = BigDecimal.ZERO;

        if (odometer != null && odometer > 0) {
            runningCostPerKm = totalOwnershipCost.divide(BigDecimal.valueOf(odometer), 2, RoundingMode.HALF_UP);
            fuelCostPerKm = fuelCost.divide(BigDecimal.valueOf(odometer), 2, RoundingMode.HALF_UP);
        }

        long totalServices = serviceRecordRepository.countByVehicleVehicleId(vId);
        long totalFuelLogs = fuelRecordRepository.countByVehicleVehicleId(vId);
        long totalMaintenance = maintenanceRecordRepository.countByVehicleVehicleId(vId);
        long completedMaint = maintenanceRecordRepository.countByVehicleVehicleIdAndStatus(vId, MaintenanceStatus.COMPLETED);
        long overdueMaint = maintenanceRecordRepository.countByVehicleVehicleIdAndStatus(vId, MaintenanceStatus.OVERDUE);

        Double completionRate = totalMaintenance > 0
                ? Math.round(((double) completedMaint / (double) totalMaintenance * 100.0) * 10.0) / 10.0
                : 100.0;

        return VehicleComparisonDTO.builder()
                .vehicleId(vId)
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .categoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : "Uncategorized")
                .fuelType(vehicle.getFuelType() != null ? vehicle.getFuelType() : "N/A")
                .currentOdometer(odometer)
                .serviceCost(serviceCost)
                .fuelCost(fuelCost)
                .maintenanceCost(maintenanceCost)
                .totalOwnershipCost(totalOwnershipCost)
                .avgMileageKmpl(avgMileage)
                .totalFuelLitres(totalFuelLitres)
                .runningCostPerKm(runningCostPerKm)
                .fuelCostPerKm(fuelCostPerKm)
                .totalServicesCount(totalServices)
                .totalFuelLogsCount(totalFuelLogs)
                .totalMaintenanceTasksCount(totalMaintenance)
                .maintenanceCompletedCount(completedMaint)
                .maintenanceOverdueCount(overdueMaint)
                .maintenanceCompletionRate(completionRate)
                .build();
    }

    private void assignPerformanceBadges(List<VehicleComparisonDTO> vehicles) {
        if (vehicles.isEmpty()) return;

        // 1. Most Fuel Efficient (highest non-null avgMileageKmpl > 0)
        VehicleComparisonDTO bestMileage = vehicles.stream()
                .filter(v -> v.getAvgMileageKmpl() != null && v.getAvgMileageKmpl() > 0)
                .max(Comparator.comparingDouble(VehicleComparisonDTO::getAvgMileageKmpl))
                .orElse(null);
        if (bestMileage != null) {
            bestMileage.setMostFuelEfficient(true);
        }

        // 2. Lowest Cost per KM (lowest runningCostPerKm > 0)
        VehicleComparisonDTO lowestCost = vehicles.stream()
                .filter(v -> v.getRunningCostPerKm() != null && v.getRunningCostPerKm().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(VehicleComparisonDTO::getRunningCostPerKm))
                .orElse(null);
        if (lowestCost != null) {
            lowestCost.setLowestCostPerKm(true);
        }

        // 3. Lowest Maintenance Cost
        VehicleComparisonDTO lowestMaint = vehicles.stream()
                .min(Comparator.comparing(VehicleComparisonDTO::getMaintenanceCost))
                .orElse(null);
        if (lowestMaint != null) {
            lowestMaint.setLowestMaintenanceCost(true);
        }

        // 4. Fleet Workhorse (highest current odometer > 0)
        VehicleComparisonDTO workhorse = vehicles.stream()
                .filter(v -> v.getCurrentOdometer() != null && v.getCurrentOdometer() > 0)
                .max(Comparator.comparingInt(VehicleComparisonDTO::getCurrentOdometer))
                .orElse(null);
        if (workhorse != null) {
            workhorse.setFleetWorkhorse(true);
        }
    }
}
