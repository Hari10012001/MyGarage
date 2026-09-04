package com.mygarage.service;

import com.mygarage.dto.response.*;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleReliabilityServiceImpl implements VehicleReliabilityService {

    public static final String RELIABILITY_DISCLAIMER =
            "Reliability classification, subsystem identification, chronic-defect detection, and VRI " +
            "are deterministic rule-based analytical models derived from existing service and maintenance records, " +
            "not OEM diagnostic or sensor-confirmed failure data.";

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    @Override
    public VehicleReliabilityReportDTO getVehicleReliability(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        List<ServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateAsc(vehicleId);
        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        return buildReliabilityReport(vehicle, serviceRecords, maintenanceRecords);
    }

    @Override
    public GarageReliabilityMatrixDTO getGarageReliabilityMatrix(Long userId) {
        List<Vehicle> userVehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        if (userVehicles.isEmpty()) {
            return GarageReliabilityMatrixDTO.builder()
                    .averageGarageVri(100)
                    .fleetReliabilityGrade("EXCELLENT")
                    .fleetMdbfKm(null)
                    .fleetTotalBreakdownSpend(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .fleetTotalServiceVisits(0)
                    .mostReliableVehicle(null)
                    .highestRiskVehicle(null)
                    .vehicleSummaries(Collections.emptyList())
                    .subsystemSpendDistribution(Collections.emptyList())
                    .recommendations(List.of("No vehicles in garage. Add a vehicle to begin reliability tracking."))
                    .disclaimer(RELIABILITY_DISCLAIMER)
                    .build();
        }

        List<VehicleReliabilitySummaryItemDTO> summaries = new ArrayList<>();
        Map<String, BigDecimal> fleetSubsystemSpend = new LinkedHashMap<>();
        Map<String, Integer> fleetSubsystemCounts = new LinkedHashMap<>();
        Map<String, String> fleetSubsystemDisplay = new LinkedHashMap<>();

        // Initialize all 6 standard subsystems + general
        for (Subsystem sub : Subsystem.values()) {
            fleetSubsystemSpend.put(sub.name(), BigDecimal.ZERO);
            fleetSubsystemCounts.put(sub.name(), 0);
            fleetSubsystemDisplay.put(sub.name(), sub.getDisplayName());
        }

        BigDecimal fleetTotalBreakdownSpend = BigDecimal.ZERO;
        int fleetTotalVisits = 0;
        int totalVri = 0;
        double sumMdbf = 0.0;
        int mdbfCount = 0;

        for (Vehicle v : userVehicles) {
            List<ServiceRecord> sRecords = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateAsc(v.getVehicleId());
            List<MaintenanceRecord> mRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(v.getVehicleId());
            VehicleReliabilityReportDTO report = buildReliabilityReport(v, sRecords, mRecords);

            summaries.add(VehicleReliabilitySummaryItemDTO.builder()
                    .vehicleId(v.getVehicleId())
                    .plateNumber(v.getPlateNumber())
                    .make(v.getMake())
                    .model(v.getModel())
                    .year(v.getYear())
                    .vriScore(report.getVriScore())
                    .reliabilityGrade(report.getReliabilityGrade())
                    .totalVisits(report.getTotalServiceVisits())
                    .mdbfKm(report.getMdbfKm())
                    .breakdownRisk(deriveBreakdownRisk(report.getVriScore()))
                    .build());

            fleetTotalBreakdownSpend = fleetTotalBreakdownSpend.add(report.getUnscheduledBreakdownSpend());
            fleetTotalVisits += report.getTotalServiceVisits();
            totalVri += report.getVriScore();

            if (report.getMdbfKm() != null) {
                sumMdbf += report.getMdbfKm();
                mdbfCount++;
            }

            for (SubsystemFailureBreakdownDTO subDto : report.getSubsystemBreakdowns()) {
                String k = subDto.getSubsystem();
                if (fleetSubsystemSpend.containsKey(k)) {
                    fleetSubsystemSpend.put(k, fleetSubsystemSpend.get(k).add(subDto.getTotalCost()));
                    fleetSubsystemCounts.put(k, fleetSubsystemCounts.get(k) + subDto.getRecordCount());
                }
            }
        }

        // Sort summaries by VRI descending
        summaries.sort(Comparator.comparingInt(VehicleReliabilitySummaryItemDTO::getVriScore).reversed());

        int avgVri = userVehicles.isEmpty() ? 100 : Math.round((float) totalVri / userVehicles.size());
        Double fleetAvgMdbf = mdbfCount > 0 ? Math.round((sumMdbf / mdbfCount) * 100.0) / 100.0 : null;

        VehicleReliabilitySummaryItemDTO mostReliable = summaries.get(0);
        VehicleReliabilitySummaryItemDTO highestRisk = summaries.get(summaries.size() - 1);

        // Build fleet subsystem spend distribution
        BigDecimal totalSpendAll = fleetSubsystemSpend.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SubsystemFailureBreakdownDTO> fleetSubsystemList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : fleetSubsystemSpend.entrySet()) {
            double pct = 0.0;
            if (totalSpendAll.compareTo(BigDecimal.ZERO) > 0) {
                pct = entry.getValue().divide(totalSpendAll, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                pct = Math.round(pct * 10.0) / 10.0;
            }
            fleetSubsystemList.add(SubsystemFailureBreakdownDTO.builder()
                    .subsystem(entry.getKey())
                    .displayName(fleetSubsystemDisplay.get(entry.getKey()))
                    .recordCount(fleetSubsystemCounts.get(entry.getKey()))
                    .totalCost(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                    .spendPercentage(pct)
                    .build());
        }

        // Recommendations
        List<String> recs = new ArrayList<>();
        if (highestRisk.getVriScore() < 60) {
            recs.add("Vehicle " + highestRisk.getMake() + " " + highestRisk.getModel() + " (" + highestRisk.getPlateNumber() +
                    ") has an elevated breakdown risk (VRI " + highestRisk.getVriScore() + "). Conduct a comprehensive pre-emptive inspection.");
        }
        // Check top spend subsystem
        Optional<SubsystemFailureBreakdownDTO> topSub = fleetSubsystemList.stream()
                .filter(s -> s.getSpendPercentage() != null && s.getSpendPercentage() > 25.0)
                .max(Comparator.comparing(SubsystemFailureBreakdownDTO::getTotalCost));
        topSub.ifPresent(s -> recs.add(s.getDisplayName() + " accounts for " + s.getSpendPercentage() +
                "% of fleet repair expenditures. Standardize preventive maintenance in this area."));

        if (recs.isEmpty()) {
            recs.add("Overall fleet reliability is in a healthy operating window. Continue scheduled preventive servicing.");
        }

        return GarageReliabilityMatrixDTO.builder()
                .averageGarageVri(avgVri)
                .fleetReliabilityGrade(deriveGrade(avgVri))
                .fleetMdbfKm(fleetAvgMdbf)
                .fleetTotalBreakdownSpend(fleetTotalBreakdownSpend.setScale(2, RoundingMode.HALF_UP))
                .fleetTotalServiceVisits(fleetTotalVisits)
                .mostReliableVehicle(mostReliable)
                .highestRiskVehicle(highestRisk)
                .vehicleSummaries(summaries)
                .subsystemSpendDistribution(fleetSubsystemList)
                .recommendations(recs)
                .disclaimer(RELIABILITY_DISCLAIMER)
                .build();
    }

    private VehicleReliabilityReportDTO buildReliabilityReport(Vehicle vehicle,
                                                               List<ServiceRecord> serviceRecords,
                                                               List<MaintenanceRecord> maintenanceRecords) {
        int totalVisits = serviceRecords.size();
        BigDecimal totalSpend = BigDecimal.ZERO;
        for (ServiceRecord sr : serviceRecords) {
            if (sr.getCost() != null) {
                totalSpend = totalSpend.add(sr.getCost());
            }
        }

        if (totalVisits == 0) {
            return VehicleReliabilityReportDTO.builder()
                    .vehicleId(vehicle.getVehicleId())
                    .plateNumber(vehicle.getPlateNumber())
                    .make(vehicle.getMake())
                    .model(vehicle.getModel())
                    .year(vehicle.getYear())
                    .currentOdometer(vehicle.getCurrentOdometer())
                    .vriScore(100)
                    .reliabilityGrade("EXCELLENT")
                    .totalServiceVisits(0)
                    .totalServiceSpend(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .mdbfKm(null)
                    .mtbsDays(null)
                    .unscheduledBreakdownCount(0)
                    .unscheduledBreakdownSpend(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .correctiveServiceRatio(0.0)
                    .routineMaintenanceCount(0)
                    .routineMaintenanceSpend(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .subsystemBreakdowns(createEmptySubsystems())
                    .chronicDefectAlerts(Collections.emptyList())
                    .workshops(Collections.emptyList())
                    .serviceAccelerationStatus("INSUFFICIENT_DATA")
                    .disclaimer(RELIABILITY_DISCLAIMER)
                    .build();
        }

        // 1. MDBF and MTBS calculations (sequential intervals)
        List<Long> dayIntervals = new ArrayList<>();
        List<Integer> kmIntervals = new ArrayList<>();

        for (int i = 1; i < serviceRecords.size(); i++) {
            ServiceRecord prev = serviceRecords.get(i - 1);
            ServiceRecord curr = serviceRecords.get(i);

            if (curr.getServiceDate() != null && prev.getServiceDate() != null) {
                long days = ChronoUnit.DAYS.between(prev.getServiceDate(), curr.getServiceDate());
                if (days >= 0) {
                    dayIntervals.add(days);
                }
            }

            if (curr.getOdometerAtService() != null && prev.getOdometerAtService() != null) {
                int deltaKm = curr.getOdometerAtService() - prev.getOdometerAtService();
                // Defensive guard: only strictly positive or monotonic intervals
                if (deltaKm > 0) {
                    kmIntervals.add(deltaKm);
                }
            }
        }

        Double mtbsDays = dayIntervals.isEmpty() ? null :
                Math.round(dayIntervals.stream().mapToLong(Long::longValue).average().orElse(0.0) * 10.0) / 10.0;
        Double mdbfKm = kmIntervals.isEmpty() ? null :
                Math.round(kmIntervals.stream().mapToInt(Integer::intValue).average().orElse(0.0) * 10.0) / 10.0;

        // 2. Service Acceleration Velocity
        String accelerationStatus = "INSUFFICIENT_DATA";
        if (dayIntervals.size() >= 3) {
            int mid = dayIntervals.size() / 2;
            double earlyAvg = dayIntervals.subList(0, mid).stream().mapToLong(Long::longValue).average().orElse(0.0);
            double recentAvg = dayIntervals.subList(mid, dayIntervals.size()).stream().mapToLong(Long::longValue).average().orElse(0.0);

            if (earlyAvg > 0) {
                double ratio = recentAvg / earlyAvg;
                if (ratio < 0.70) {
                    accelerationStatus = "ACCELERATING"; // Visiting workshop noticeably more frequently
                } else if (ratio > 1.30) {
                    accelerationStatus = "IMPROVING";
                } else {
                    accelerationStatus = "STABLE";
                }
            }
        }

        // 3. Subsystem Breakdown & Routine vs Corrective Classification
        Map<Subsystem, List<ServiceRecord>> recordsBySubsystem = new EnumMap<>(Subsystem.class);
        for (Subsystem s : Subsystem.values()) {
            recordsBySubsystem.put(s, new ArrayList<>());
        }

        int unscheduledCount = 0;
        BigDecimal unscheduledSpend = BigDecimal.ZERO;
        int routineCount = 0;
        BigDecimal routineSpend = BigDecimal.ZERO;

        for (ServiceRecord sr : serviceRecords) {
            Subsystem sub = classifySubsystem(sr.getServiceType(), sr.getDescription());
            recordsBySubsystem.get(sub).add(sr);

            boolean isCorrective = isCorrectiveService(sr.getServiceType(), sr.getDescription());
            BigDecimal cost = sr.getCost() != null ? sr.getCost() : BigDecimal.ZERO;
            if (isCorrective) {
                unscheduledCount++;
                unscheduledSpend = unscheduledSpend.add(cost);
            } else {
                routineCount++;
                routineSpend = routineSpend.add(cost);
            }
        }

        // Build subsystem breakdown DTOs
        List<SubsystemFailureBreakdownDTO> subsystemBreakdowns = new ArrayList<>();
        for (Subsystem s : Subsystem.values()) {
            List<ServiceRecord> list = recordsBySubsystem.get(s);
            BigDecimal subCost = BigDecimal.ZERO;
            LocalDate latestDate = null;

            for (ServiceRecord sr : list) {
                if (sr.getCost() != null) {
                    subCost = subCost.add(sr.getCost());
                }
                if (sr.getServiceDate() != null) {
                    if (latestDate == null || sr.getServiceDate().isAfter(latestDate)) {
                        latestDate = sr.getServiceDate();
                    }
                }
            }

            double pct = 0.0;
            if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                pct = subCost.divide(totalSpend, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                pct = Math.round(pct * 10.0) / 10.0;
            }

            subsystemBreakdowns.add(SubsystemFailureBreakdownDTO.builder()
                    .subsystem(s.name())
                    .displayName(s.getDisplayName())
                    .recordCount(list.size())
                    .totalCost(subCost.setScale(2, RoundingMode.HALF_UP))
                    .spendPercentage(pct)
                    .lastRepairDate(latestDate)
                    .build());
        }

        double csr = 0.0;
        if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
            csr = unscheduledSpend.divide(totalSpend, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        } else if (totalVisits > 0) {
            csr = ((double) unscheduledCount / totalVisits) * 100.0;
        }
        csr = Math.round(csr * 10.0) / 10.0;

        // 4. Chronic Defect Clustering
        List<ChronicDefectAlertDTO> chronicAlerts = new ArrayList<>();
        for (Map.Entry<Subsystem, List<ServiceRecord>> entry : recordsBySubsystem.entrySet()) {
            Subsystem sub = entry.getKey();
            if (sub == Subsystem.GENERAL_ROUTINE) continue;

            List<ServiceRecord> subList = entry.getValue();
            if (subList.size() >= 2) {
                // Check pairwise gaps between consecutive subsystem incidents
                int clusterCount = 1;
                long minDayGap = Long.MAX_VALUE;
                Integer minKmGap = null;

                for (int i = 1; i < subList.size(); i++) {
                    ServiceRecord p = subList.get(i - 1);
                    ServiceRecord c = subList.get(i);

                    long dayGap = Long.MAX_VALUE;
                    if (c.getServiceDate() != null && p.getServiceDate() != null) {
                        dayGap = Math.abs(ChronoUnit.DAYS.between(p.getServiceDate(), c.getServiceDate()));
                    }

                    Integer kmGap = null;
                    if (c.getOdometerAtService() != null && p.getOdometerAtService() != null) {
                        int diff = Math.abs(c.getOdometerAtService() - p.getOdometerAtService());
                        if (diff > 0) {
                            kmGap = diff;
                        }
                    }

                    if (dayGap <= 180 || (kmGap != null && kmGap <= 5000)) {
                        clusterCount++;
                        if (dayGap < minDayGap) minDayGap = dayGap;
                        if (kmGap != null && (minKmGap == null || kmGap < minKmGap)) minKmGap = kmGap;
                    }
                }

                if (clusterCount >= 2) {
                    String severity;
                    if (clusterCount == 2) severity = "MODERATE";
                    else if (clusterCount == 3) severity = "HIGH";
                    else severity = "CRITICAL";

                    chronicAlerts.add(ChronicDefectAlertDTO.builder()
                            .subsystem(sub.name())
                            .subsystemDisplayName(sub.getDisplayName())
                            .recurringCount(clusterCount)
                            .dayGap(minDayGap != Long.MAX_VALUE ? minDayGap : null)
                            .odometerGap(minKmGap)
                            .severity(severity)
                            .diagnosticNote("Detected " + clusterCount + " recurring service visits in " +
                                    sub.getDisplayName() + " within close operational windows (<180 days or <5,000 km).")
                            .build());
                }
            }
        }

        // 5. Workshop Analytics & Mean Return Interval (MRI)
        Map<String, List<Integer>> workshopIndices = new LinkedHashMap<>();
        Map<String, BigDecimal> workshopSpend = new LinkedHashMap<>();

        for (int i = 0; i < serviceRecords.size(); i++) {
            ServiceRecord sr = serviceRecords.get(i);
            String gName = (sr.getGarageName() != null && !sr.getGarageName().trim().isEmpty())
                    ? sr.getGarageName().trim() : "Independent / Unspecified Workshop";

            workshopIndices.computeIfAbsent(gName, k -> new ArrayList<>()).add(i);
            BigDecimal cost = sr.getCost() != null ? sr.getCost() : BigDecimal.ZERO;
            workshopSpend.put(gName, workshopSpend.getOrDefault(gName, BigDecimal.ZERO).add(cost));
        }

        List<WorkshopReliabilityDTO> workshopDtos = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : workshopIndices.entrySet()) {
            String gName = entry.getKey();
            List<Integer> idxs = entry.getValue();
            int count = idxs.size();
            BigDecimal totalGSpend = workshopSpend.get(gName);
            BigDecimal avgCost = count > 0 ? totalGSpend.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            List<Long> returnDays = new ArrayList<>();
            for (int idx : idxs) {
                if (idx < serviceRecords.size() - 1) {
                    ServiceRecord next = serviceRecords.get(idx + 1);
                    ServiceRecord current = serviceRecords.get(idx);
                    if (next.getServiceDate() != null && current.getServiceDate() != null) {
                        long d = ChronoUnit.DAYS.between(current.getServiceDate(), next.getServiceDate());
                        if (d >= 0) {
                            returnDays.add(d);
                        }
                    }
                }
            }

            Double mri = returnDays.isEmpty() ? null :
                    Math.round(returnDays.stream().mapToLong(Long::longValue).average().orElse(0.0) * 10.0) / 10.0;

            workshopDtos.add(WorkshopReliabilityDTO.builder()
                    .workshopName(gName)
                    .visitCount(count)
                    .totalSpend(totalGSpend.setScale(2, RoundingMode.HALF_UP))
                    .averageCostPerVisit(avgCost)
                    .meanReturnIntervalDays(mri)
                    .build());
        }

        // Sort workshops by visit count descending
        workshopDtos.sort(Comparator.comparingInt(WorkshopReliabilityDTO::getVisitCount).reversed());

        // 6. VRI Composite Scoring
        int vriScore = computeVri(vehicle, totalVisits, mdbfKm, csr, chronicAlerts);
        String grade = deriveGrade(vriScore);

        return VehicleReliabilityReportDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .currentOdometer(vehicle.getCurrentOdometer())
                .vriScore(vriScore)
                .reliabilityGrade(grade)
                .totalServiceVisits(totalVisits)
                .totalServiceSpend(totalSpend.setScale(2, RoundingMode.HALF_UP))
                .mdbfKm(mdbfKm)
                .mtbsDays(mtbsDays)
                .unscheduledBreakdownCount(unscheduledCount)
                .unscheduledBreakdownSpend(unscheduledSpend.setScale(2, RoundingMode.HALF_UP))
                .correctiveServiceRatio(csr)
                .routineMaintenanceCount(routineCount)
                .routineMaintenanceSpend(routineSpend.setScale(2, RoundingMode.HALF_UP))
                .subsystemBreakdowns(subsystemBreakdowns)
                .chronicDefectAlerts(chronicAlerts)
                .workshops(workshopDtos)
                .serviceAccelerationStatus(accelerationStatus)
                .disclaimer(RELIABILITY_DISCLAIMER)
                .build();
    }

    private int computeVri(Vehicle vehicle, int totalVisits, Double mdbfKm, double csr, List<ChronicDefectAlertDTO> chronicAlerts) {
        if (totalVisits <= 1 && chronicAlerts.isEmpty()) {
            return 100;
        }

        double score = 100.0;

        // Penalty 1: Frequency / MDBF penalty
        if (mdbfKm != null) {
            if (mdbfKm < 2500.0) {
                score -= 30.0;
            } else if (mdbfKm < 5000.0) {
                score -= 20.0;
            } else if (mdbfKm < 8000.0) {
                score -= 10.0;
            }
        }

        // Penalty 2: Corrective Service Ratio penalty
        if (csr > 75.0) {
            score -= 25.0;
        } else if (csr > 50.0) {
            score -= 15.0;
        } else if (csr > 25.0) {
            score -= 8.0;
        }

        // Penalty 3: Chronic Recurring Defects penalty
        int chronicPenalty = Math.min(30, chronicAlerts.size() * 10);
        score -= chronicPenalty;

        // Bonus: Aging/Mileage resilience bonus
        int currentYear = LocalDate.now().getYear();
        int age = (vehicle.getYear() != null && vehicle.getYear() > 1900) ? Math.max(0, currentYear - vehicle.getYear()) : 0;
        int odo = vehicle.getCurrentOdometer() != null ? vehicle.getCurrentOdometer() : 0;

        boolean hasCriticalChronic = chronicAlerts.stream().anyMatch(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity()));
        if ((age >= 5 || odo >= 75000) && csr <= 40.0 && !hasCriticalChronic) {
            score += 10.0;
        }

        int clamped = (int) Math.round(score);
        return Math.max(0, Math.min(100, clamped));
    }

    private String deriveGrade(int vri) {
        if (vri >= 90) return "EXCELLENT";
        if (vri >= 75) return "GOOD";
        if (vri >= 60) return "MODERATE";
        if (vri >= 40) return "POOR";
        return "CRITICAL_RISK";
    }

    private String deriveBreakdownRisk(int vri) {
        if (vri >= 85) return "LOW";
        if (vri >= 70) return "MODERATE";
        if (vri >= 50) return "ELEVATED";
        return "HIGH";
    }

    private boolean isCorrectiveService(String serviceType, String description) {
        String text = ((serviceType != null ? serviceType : "") + " " + (description != null ? description : "")).toLowerCase();

        // Corrective breakdown keywords
        String[] correctiveKeywords = {
                "repair", "broken", "replace", "failure", "fault", "leak", "noise",
                "vibration", "overhaul", "damaged", "defect", "engine light", "overheating",
                "breakdown", "emergency", "stuck", "crack", "dead battery", "puncture"
        };

        // Routine keywords
        String[] routineKeywords = {
                "routine", "periodic", "scheduled", "general service", "regular service",
                "annual service", "minor service", "major service", "oil change", "inspection",
                "pms", "tune up", "tune-up", "fluid check", "wheel alignment"
        };

        boolean matchesCorrective = Arrays.stream(correctiveKeywords).anyMatch(text::contains);
        boolean matchesRoutine = Arrays.stream(routineKeywords).anyMatch(text::contains);

        if (matchesCorrective && !matchesRoutine) return true;
        if (matchesRoutine && !matchesCorrective) return false;
        if (matchesCorrective && matchesRoutine) {
            // If it has "replace brake pad" as part of routine, or "leak repair"
            return text.contains("leak") || text.contains("broken") || text.contains("failure") || text.contains("emergency");
        }

        // Fallback default: standard regular service is routine, everything else is corrective
        return !text.contains("service") && !text.contains("oil");
    }

    private Subsystem classifySubsystem(String serviceType, String description) {
        String text = ((serviceType != null ? serviceType : "") + " " + (description != null ? description : "")).toLowerCase();

        // Specific multi-word check first (like "cabin filter")
        if (text.contains("cabin filter") || text.contains("air condition") || text.contains("compressor")) {
            return Subsystem.HVAC_BODY_AUXILIARY;
        }

        for (Subsystem sub : Subsystem.values()) {
            if (sub == Subsystem.GENERAL_ROUTINE) continue;
            for (String kw : sub.getKeywords()) {
                String regex = "\\b" + java.util.regex.Pattern.quote(kw) + "\\b";
                if (java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                    return sub;
                }
            }
        }
        return Subsystem.GENERAL_ROUTINE;
    }

    private List<SubsystemFailureBreakdownDTO> createEmptySubsystems() {
        List<SubsystemFailureBreakdownDTO> list = new ArrayList<>();
        for (Subsystem s : Subsystem.values()) {
            list.add(SubsystemFailureBreakdownDTO.builder()
                    .subsystem(s.name())
                    .displayName(s.getDisplayName())
                    .recordCount(0)
                    .totalCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .spendPercentage(0.0)
                    .lastRepairDate(null)
                    .build());
        }
        return list;
    }

    public enum Subsystem {
        HVAC_BODY_AUXILIARY("HVAC & Body", new String[]{
                "ac", "a/c", "air condition", "air conditioning", "heater", "compressor", "blower", "wiper", "body", "paint", "door", "glass", "window", "mirror", "cabin", "dent", "cabin filter"
        }),
        SUSPENSION_STEERING("Suspension & Steering", new String[]{
                "suspension", "shock", "strut", "spring", "steering", "rack", "tie rod", "ball joint", "bushing", "stabilizer", "sway bar"
        }),
        TRANSMISSION_DRIVETRAIN("Transmission & Drivetrain", new String[]{
                "transmission", "gearbox", "clutch", "drive shaft", "driveshaft", "differential", "axle", "gear", "cv joint"
        }),
        BRAKING_TIRES("Braking & Tires", new String[]{
                "brake", "pad", "disc", "rotor", "caliper", "abs", "tire", "tyre", "wheel", "alignment", "balancing"
        }),
        ELECTRICAL_BATTERY("Electrical & Battery", new String[]{
                "battery", "alternator", "starter", "fuse", "wiring", "light", "bulb", "sensor", "ecu", "horn", "electrical", "ignition"
        }),
        POWERTRAIN_ENGINE("Powertrain & Engine", new String[]{
                "engine", "oil", "oil filter", "fuel filter", "timing", "piston", "spark plug", "sparkplug",
                "cylinder", "injector", "turbo", "coolant", "radiator", "exhaust", "catalytic", "fuel pump"
        }),
        GENERAL_ROUTINE("General Maintenance", new String[]{
                "service", "inspection", "wash", "polish", "checkup", "maintenance"
        });

        private final String displayName;
        private final String[] keywords;

        Subsystem(String displayName, String[] keywords) {
            this.displayName = displayName;
            this.keywords = keywords;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String[] getKeywords() {
            return keywords;
        }
    }
}
