package com.mygarage.service;

import com.mygarage.dto.request.FuelRecordRequest;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.repository.FuelRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FuelRecordService {

    private final FuelRecordRepository fuelRecordRepository;
    private final VehicleService vehicleService;

    public FuelRecord addFuelRecord(Long vehicleId, Long userId, FuelRecordRequest request) {
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);

        FuelRecord record = new FuelRecord();
        record.setVehicle(vehicle);
        record.setFuelDate(request.getFuelDate());
        record.setFuelType(request.getFuelType());
        record.setQuantityLitres(request.getQuantityLitres());
        record.setCostPerLitre(request.getCostPerLitre());
        record.setOdometerAtFill(request.getOdometerAtFill());
        record.setNotes(request.getNotes());

        // Calculate estimated mileage from previous record
        if (request.getOdometerAtFill() != null) {
            calculateEstimatedMileage(record, vehicleId, request.getFuelDate(), request.getOdometerAtFill());
            // Update vehicle odometer
            if (request.getOdometerAtFill() > (vehicle.getCurrentOdometer() == null ? 0 : vehicle.getCurrentOdometer())) {
                vehicle.setCurrentOdometer(request.getOdometerAtFill());
            }
        }
        return fuelRecordRepository.save(record);
    }

    /**
     * Calculates estimated mileage (km/L) based on odometer difference
     * from the previous fuel record. Labelled as ESTIMATED - not guaranteed actual.
     */
    private void calculateEstimatedMileage(FuelRecord record, Long vehicleId,
                                           LocalDate date, int currentOdometer) {
        List<FuelRecord> previous = fuelRecordRepository.findPreviousRecord(
                vehicleId, date, PageRequest.of(0, 1));
        if (!previous.isEmpty() && previous.get(0).getOdometerAtFill() != null) {
            int prevOdometer = previous.get(0).getOdometerAtFill();
            int distanceTravelled = currentOdometer - prevOdometer;
            if (distanceTravelled > 0 && record.getQuantityLitres().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal mileage = BigDecimal.valueOf(distanceTravelled)
                        .divide(record.getQuantityLitres(), 2, RoundingMode.HALF_UP);
                record.setEstimatedMileageKmpl(mileage);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FuelRecord> getFuelRecordsForVehicle(Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        return fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public List<FuelRecord> getFuelRecordsFiltered(Long vehicleId, Long userId,
                                                   LocalDate from, LocalDate to) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        if (from != null && to != null) {
            return fuelRecordRepository.findByVehicleAndDateRange(vehicleId, from, to);
        }
        return fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public FuelRecord getFuelRecord(Long fuelId, Long userId) {
        return fuelRecordRepository.findByFuelIdAndVehicleUserUserId(fuelId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Fuel record not found or access denied."));
    }

    @Transactional(readOnly = true)
    public FuelRecord getFuelRecordForVehicle(Long fuelId, Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        FuelRecord record = getFuelRecord(fuelId, userId);
        if (!record.getVehicle().getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("Fuel record does not belong to the specified vehicle.");
        }
        return record;
    }

    public FuelRecord updateFuelRecord(Long fuelId, Long userId, FuelRecordRequest request) {
        FuelRecord record = getFuelRecord(fuelId, userId);
        record.setFuelDate(request.getFuelDate());
        record.setFuelType(request.getFuelType());
        record.setQuantityLitres(request.getQuantityLitres());
        record.setCostPerLitre(request.getCostPerLitre());
        record.setOdometerAtFill(request.getOdometerAtFill());
        record.setNotes(request.getNotes());
        // Recalculate estimated mileage
        if (request.getOdometerAtFill() != null) {
            calculateEstimatedMileage(record, record.getVehicle().getVehicleId(),
                    request.getFuelDate(), request.getOdometerAtFill());
            if (request.getOdometerAtFill() > (record.getVehicle().getCurrentOdometer() == null ? 0 : record.getVehicle().getCurrentOdometer())) {
                record.getVehicle().setCurrentOdometer(request.getOdometerAtFill());
            }
        }
        return fuelRecordRepository.save(record);
    }

    public FuelRecord updateFuelRecordForVehicle(Long fuelId, Long vehicleId, Long userId, FuelRecordRequest request) {
        FuelRecord record = getFuelRecordForVehicle(fuelId, vehicleId, userId);
        record.setFuelDate(request.getFuelDate());
        record.setFuelType(request.getFuelType());
        record.setQuantityLitres(request.getQuantityLitres());
        record.setCostPerLitre(request.getCostPerLitre());
        record.setOdometerAtFill(request.getOdometerAtFill());
        record.setNotes(request.getNotes());
        if (request.getOdometerAtFill() != null) {
            calculateEstimatedMileage(record, vehicleId, request.getFuelDate(), request.getOdometerAtFill());
            if (request.getOdometerAtFill() > (record.getVehicle().getCurrentOdometer() == null ? 0 : record.getVehicle().getCurrentOdometer())) {
                record.getVehicle().setCurrentOdometer(request.getOdometerAtFill());
            }
        }
        return fuelRecordRepository.save(record);
    }

    public void deleteFuelRecord(Long fuelId, Long userId) {
        FuelRecord record = getFuelRecord(fuelId, userId);
        fuelRecordRepository.delete(record);
    }

    public void deleteFuelRecordForVehicle(Long fuelId, Long vehicleId, Long userId) {
        FuelRecord record = getFuelRecordForVehicle(fuelId, vehicleId, userId);
        fuelRecordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<FuelRecord> getAllFuelForUser(Long userId) {
        return fuelRecordRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalFuelQuantityForUser(Long userId) {
        return fuelRecordRepository.sumQuantityLitresByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Double averageEstimatedMileageForUser(Long userId) {
        return fuelRecordRepository.avgEstimatedMileageByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countForUser(Long userId) {
        return fuelRecordRepository.countByVehicleUserUserId(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalFuelCostForUser(Long userId) {
        return fuelRecordRepository.sumTotalCostByUserId(userId);
    }
}
