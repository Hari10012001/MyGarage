package com.mygarage.service;

import com.mygarage.dto.response.GarageReportSummaryDTO;
import com.mygarage.dto.response.VehicleDossierDTO;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.repository.FuelRecordRepository;
import com.mygarage.repository.MaintenanceRecordRepository;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    private Vehicle getVehicleAndValidateOwnership(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + vehicleId));
        if (!vehicle.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied: Vehicle does not belong to the current user");
        }
        return vehicle;
    }

    @Override
    public VehicleDossierDTO getVehicleDossier(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleAndValidateOwnership(vehicleId, userId);

        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<FuelRecord> fuelLogs = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        // Dynamically compute statuses for maintenance
        maintenanceRecords.forEach(m -> m.setStatus(MaintenanceService.computeStatus(m.getScheduledDate(), m.getCompletedDate())));

        BigDecimal totalServiceCost = serviceRecordRepository.sumCostByVehicleId(vehicleId);
        BigDecimal totalFuelCost = fuelRecordRepository.sumTotalCostByVehicleId(vehicleId);
        BigDecimal totalMaintenanceCost = maintenanceRecordRepository.sumCostByVehicleId(vehicleId);
        BigDecimal totalOwnershipCost = totalServiceCost.add(totalFuelCost).add(totalMaintenanceCost);

        BigDecimal totalFuelLitres = fuelRecordRepository.sumQuantityLitresByVehicleId(vehicleId);
        Double avgMileage = fuelRecordRepository.avgEstimatedMileageByVehicleId(vehicleId);

        return VehicleDossierDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .currentOdometer(vehicle.getCurrentOdometer())
                .fuelType(vehicle.getFuelType())
                .categoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : "Uncategorized")
                .notes(vehicle.getNotes())
                .ownerName(vehicle.getUser().getFullName())
                .ownerEmail(vehicle.getUser().getEmail())
                .ownerPhone(vehicle.getUser().getPhone())
                .totalServiceCost(totalServiceCost)
                .totalFuelCost(totalFuelCost)
                .totalMaintenanceCost(totalMaintenanceCost)
                .totalOwnershipCost(totalOwnershipCost)
                .totalFuelLitres(totalFuelLitres)
                .averageMileageKmpl(avgMileage != null ? BigDecimal.valueOf(avgMileage).setScale(2, RoundingMode.HALF_UP).doubleValue() : null)
                .totalServicesCount(services.size())
                .totalFuelLogsCount(fuelLogs.size())
                .totalMaintenanceTasksCount(maintenanceRecords.size())
                .services(services)
                .fuelLogs(fuelLogs)
                .maintenanceRecords(maintenanceRecords)
                .generatedAt(LocalDateTime.now())
                .verificationStatus("OFFICIALLY VERIFIED MYGARAGE DIGITAL RECORD")
                .build();
    }

    @Override
    public GarageReportSummaryDTO getGarageReportSummary(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        BigDecimal totalServiceCost = serviceRecordRepository.sumCostByUserId(userId);
        BigDecimal totalFuelCost = fuelRecordRepository.sumTotalCostByUserId(userId);
        BigDecimal totalMaintenanceCost = maintenanceRecordRepository.sumCostByUserId(userId);
        BigDecimal totalGarageCost = totalServiceCost.add(totalFuelCost).add(totalMaintenanceCost);

        Double avgMileage = fuelRecordRepository.avgEstimatedMileageByUserId(userId);
        long totalServices = serviceRecordRepository.countByVehicleUserUserId(userId);
        long totalFuelLogs = fuelRecordRepository.countByVehicleUserUserId(userId);
        long totalMaintenanceTasks = maintenanceRecordRepository.countByVehicleUserUserId(userId);

        List<GarageReportSummaryDTO.VehicleReportItemDTO> breakdown = new ArrayList<>();
        for (Vehicle v : vehicles) {
            BigDecimal vSvc = serviceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            BigDecimal vFuel = fuelRecordRepository.sumTotalCostByVehicleId(v.getVehicleId());
            BigDecimal vMaint = maintenanceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            BigDecimal vTotal = vSvc.add(vFuel).add(vMaint);
            Double vMileage = fuelRecordRepository.avgEstimatedMileageByVehicleId(v.getVehicleId());
            int recCount = (int) (serviceRecordRepository.countByVehicleVehicleId(v.getVehicleId())
                    + fuelRecordRepository.countByVehicleVehicleId(v.getVehicleId())
                    + maintenanceRecordRepository.countByVehicleVehicleId(v.getVehicleId()));

            breakdown.add(GarageReportSummaryDTO.VehicleReportItemDTO.builder()
                    .vehicleId(v.getVehicleId())
                    .plateNumber(v.getPlateNumber())
                    .make(v.getMake())
                    .model(v.getModel())
                    .year(v.getYear())
                    .category(v.getCategory() != null ? v.getCategory().getName() : "Uncategorized")
                    .currentOdometer(v.getCurrentOdometer())
                    .serviceCost(vSvc)
                    .fuelCost(vFuel)
                    .maintenanceCost(vMaint)
                    .totalCost(vTotal)
                    .avgMileage(vMileage != null ? BigDecimal.valueOf(vMileage).setScale(2, RoundingMode.HALF_UP).doubleValue() : null)
                    .totalRecords(recCount)
                    .build());
        }

        return GarageReportSummaryDTO.builder()
                .totalVehicles(vehicles.size())
                .totalServices(totalServices)
                .totalFuelLogs(totalFuelLogs)
                .totalMaintenanceTasks(totalMaintenanceTasks)
                .totalServiceCost(totalServiceCost)
                .totalFuelCost(totalFuelCost)
                .totalMaintenanceCost(totalMaintenanceCost)
                .totalGarageCost(totalGarageCost)
                .averageMileage(avgMileage != null ? BigDecimal.valueOf(avgMileage).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0)
                .vehicleBreakdown(breakdown)
                .build();
    }

    @Override
    public String exportVehicleServicesCsv(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleAndValidateOwnership(vehicleId, userId);
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);

        StringBuilder sb = new StringBuilder();
        sb.append("Service ID,Vehicle Plate,Service Date,Service Type,Garage Name,Cost (INR),Odometer,Next Service Due,Description,Notes\n");
        for (ServiceRecord s : services) {
            sb.append(escapeCsv(s.getServiceId())).append(",")
              .append(escapeCsv(vehicle.getPlateNumber())).append(",")
              .append(escapeCsv(s.getServiceDate())).append(",")
              .append(escapeCsv(s.getServiceType())).append(",")
              .append(escapeCsv(s.getGarageName())).append(",")
              .append(escapeCsv(s.getCost())).append(",")
              .append(escapeCsv(s.getOdometerAtService())).append(",")
              .append(escapeCsv(s.getNextServiceDueDate())).append(",")
              .append(escapeCsv(s.getDescription())).append(",")
              .append(escapeCsv(s.getNotes())).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportVehicleFuelCsv(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleAndValidateOwnership(vehicleId, userId);
        List<FuelRecord> fuelLogs = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);

        StringBuilder sb = new StringBuilder();
        sb.append("Fuel ID,Vehicle Plate,Fuel Date,Fuel Type,Quantity (Litres),Cost Per Litre (INR),Total Cost (INR),Odometer,Estimated Mileage (km/L),Notes\n");
        for (FuelRecord f : fuelLogs) {
            sb.append(escapeCsv(f.getFuelId())).append(",")
              .append(escapeCsv(vehicle.getPlateNumber())).append(",")
              .append(escapeCsv(f.getFuelDate())).append(",")
              .append(escapeCsv(f.getFuelType())).append(",")
              .append(escapeCsv(f.getQuantityLitres())).append(",")
              .append(escapeCsv(f.getCostPerLitre())).append(",")
              .append(escapeCsv(f.getTotalCost())).append(",")
              .append(escapeCsv(f.getOdometerAtFill())).append(",")
              .append(escapeCsv(f.getEstimatedMileageKmpl())).append(",")
              .append(escapeCsv(f.getNotes())).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportVehicleMaintenanceCsv(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleAndValidateOwnership(vehicleId, userId);
        List<MaintenanceRecord> tasks = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        StringBuilder sb = new StringBuilder();
        sb.append("Maintenance ID,Vehicle Plate,Title,Status,Scheduled Date,Completed Date,Cost (INR),Description,Notes\n");
        for (MaintenanceRecord m : tasks) {
            sb.append(escapeCsv(m.getMaintenanceId())).append(",")
              .append(escapeCsv(vehicle.getPlateNumber())).append(",")
              .append(escapeCsv(m.getTitle())).append(",")
              .append(escapeCsv(MaintenanceService.computeStatus(m.getScheduledDate(), m.getCompletedDate()))).append(",")
              .append(escapeCsv(m.getScheduledDate())).append(",")
              .append(escapeCsv(m.getCompletedDate())).append(",")
              .append(escapeCsv(m.getCost())).append(",")
              .append(escapeCsv(m.getDescription())).append(",")
              .append(escapeCsv(m.getNotes())).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportVehicleAllCsv(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleAndValidateOwnership(vehicleId, userId);
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<FuelRecord> fuelLogs = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
        List<MaintenanceRecord> tasks = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        List<UnifiedHistoryRow> rows = new ArrayList<>();
        for (ServiceRecord s : services) {
            rows.add(new UnifiedHistoryRow("SERVICE", s.getServiceId(), s.getServiceDate(),
                    s.getServiceType(), s.getGarageName(), s.getOdometerAtService(), "COMPLETED", s.getCost(), s.getDescription()));
        }
        for (FuelRecord f : fuelLogs) {
            rows.add(new UnifiedHistoryRow("FUEL", f.getFuelId(), f.getFuelDate(),
                    f.getFuelType() + " Fill", f.getQuantityLitres() + " L", f.getOdometerAtFill(), "LOGGED", f.getTotalCost(), f.getNotes()));
        }
        for (MaintenanceRecord m : tasks) {
            rows.add(new UnifiedHistoryRow("MAINTENANCE", m.getMaintenanceId(), m.getScheduledDate(),
                    m.getTitle(), m.getDescription(), null, MaintenanceService.computeStatus(m.getScheduledDate(), m.getCompletedDate()).name(), m.getCost(), m.getNotes()));
        }

        // Sort rows by date descending
        rows.sort(Comparator.comparing(UnifiedHistoryRow::date, Comparator.nullsLast(Comparator.reverseOrder())));

        StringBuilder sb = new StringBuilder();
        sb.append("Record Type,Record ID,Vehicle Plate,Date,Title / Type,Provider / Details,Odometer,Status,Cost (INR),Notes\n");
        for (UnifiedHistoryRow r : rows) {
            sb.append(escapeCsv(r.type)).append(",")
              .append(escapeCsv(r.id)).append(",")
              .append(escapeCsv(vehicle.getPlateNumber())).append(",")
              .append(escapeCsv(r.date)).append(",")
              .append(escapeCsv(r.title)).append(",")
              .append(escapeCsv(r.details)).append(",")
              .append(escapeCsv(r.odometer)).append(",")
              .append(escapeCsv(r.status)).append(",")
              .append(escapeCsv(r.cost)).append(",")
              .append(escapeCsv(r.notes)).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportUserGarageCsv(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("Vehicle ID,Plate Number,Make,Model,Year,Category,Color,Current Odometer,Fuel Type,Service Cost (INR),Fuel Cost (INR),Maintenance Cost (INR),Total Cost (INR),Avg Mileage (km/L),Total Records\n");
        for (Vehicle v : vehicles) {
            BigDecimal vSvc = serviceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            BigDecimal vFuel = fuelRecordRepository.sumTotalCostByVehicleId(v.getVehicleId());
            BigDecimal vMaint = maintenanceRecordRepository.sumCostByVehicleId(v.getVehicleId());
            BigDecimal vTotal = vSvc.add(vFuel).add(vMaint);
            Double vMileage = fuelRecordRepository.avgEstimatedMileageByVehicleId(v.getVehicleId());
            int recCount = (int) (serviceRecordRepository.countByVehicleVehicleId(v.getVehicleId())
                    + fuelRecordRepository.countByVehicleVehicleId(v.getVehicleId())
                    + maintenanceRecordRepository.countByVehicleVehicleId(v.getVehicleId()));

            sb.append(escapeCsv(v.getVehicleId())).append(",")
              .append(escapeCsv(v.getPlateNumber())).append(",")
              .append(escapeCsv(v.getMake())).append(",")
              .append(escapeCsv(v.getModel())).append(",")
              .append(escapeCsv(v.getYear())).append(",")
              .append(escapeCsv(v.getCategory() != null ? v.getCategory().getName() : "Uncategorized")).append(",")
              .append(escapeCsv(v.getColor())).append(",")
              .append(escapeCsv(v.getCurrentOdometer())).append(",")
              .append(escapeCsv(v.getFuelType())).append(",")
              .append(escapeCsv(vSvc)).append(",")
              .append(escapeCsv(vFuel)).append(",")
              .append(escapeCsv(vMaint)).append(",")
              .append(escapeCsv(vTotal)).append(",")
              .append(escapeCsv(vMileage != null ? BigDecimal.valueOf(vMileage).setScale(2, RoundingMode.HALF_UP) : "")).append(",")
              .append(escapeCsv(recCount)).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String str = String.valueOf(val);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private record UnifiedHistoryRow(String type, Long id, LocalDate date, String title,
                                     String details, Integer odometer, String status,
                                     BigDecimal cost, String notes) {}
}
