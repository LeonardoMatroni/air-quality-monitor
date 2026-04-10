// ============================================================
// repository/SensorReadingRepository.java
// ============================================================
package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.SensorReading;
import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Optional<SensorReading> findTopBySensorIdOrderByRecordedAtDesc(Long sensorId);

    List<SensorReading> findBySensorIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long sensorId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT r FROM SensorReading r
        JOIN r.sensor s
        JOIN s.room rm
        WHERE rm.school.id = :schoolId
          AND r.recordedAt >= :since
        ORDER BY r.recordedAt DESC
        """)
    List<SensorReading> findBySchoolIdSince(@Param("schoolId") Long schoolId,
                                            @Param("since") LocalDateTime since);

    @Query("""
        SELECT r FROM SensorReading r
        WHERE r.sensor.id = :sensorId
          AND r.aqiCategory IN :categories
          AND r.recordedAt >= :since
        ORDER BY r.recordedAt DESC
        """)
    List<SensorReading> findPoorQualityReadings(@Param("sensorId") Long sensorId,
                                                @Param("categories") List<AqiCategory> categories,
                                                @Param("since") LocalDateTime since);

    @Query(value = """
        SELECT
            DATE_TRUNC('hour', recorded_at) AS hour,
            AVG(co2_ppm)       AS avg_co2,
            AVG(pm25_ugm3)     AS avg_pm25,
            AVG(pm10_ugm3)     AS avg_pm10,
            AVG(temperature_c) AS avg_temp,
            AVG(humidity_pct)  AS avg_humidity,
            MAX(aqi_value)     AS max_aqi,
            COUNT(*)           AS reading_count
        FROM sensor_readings
        WHERE sensor_id = :sensorId
          AND recorded_at BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('hour', recorded_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> findHourlyAverages(@Param("sensorId") Long sensorId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM SensorReading r WHERE r.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}


// ============================================================
// repository/SensorRepository.java
// ============================================================
package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.Sensor;
import br.com.escola.airquality.domain.Sensor.SensorStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findBySerialNumber(String serialNumber);

    List<Sensor> findByRoomIdAndStatus(Long roomId, SensorStatus status);

    List<Sensor> findByStatus(SensorStatus status);

    @Query("""
        SELECT s FROM Sensor s
        JOIN s.room r
        WHERE r.school.id = :schoolId
          AND s.status = 'ACTIVE'
        ORDER BY r.name, s.serialNumber
        """)
    List<Sensor> findActiveBySchoolId(@Param("schoolId") Long schoolId);

    @Query("""
        SELECT s FROM Sensor s
        WHERE s.status = 'ACTIVE'
          AND (s.lastSeenAt IS NULL OR s.lastSeenAt < :threshold)
        """)
    List<Sensor> findPotentiallyOffline(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE Sensor s SET s.lastSeenAt = :ts WHERE s.id = :id")
    void updateLastSeenAt(@Param("id") Long id, @Param("ts") LocalDateTime ts);

    @Modifying
    @Query("UPDATE Sensor s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") SensorStatus status);
}


// ============================================================
// repository/AlertRepository.java
// ============================================================
package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.Alert;
import br.com.escola.airquality.domain.Alert.AlertSeverity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findBySensorIdAndResolvedFalseOrderByCreatedAtDesc(Long sensorId);

    @Query("""
        SELECT a FROM Alert a
        JOIN a.sensor s
        JOIN s.room r
        WHERE r.school.id = :schoolId
          AND a.resolved = FALSE
        ORDER BY a.severity DESC, a.createdAt DESC
        """)
    List<Alert> findUnresolvedBySchoolId(@Param("schoolId") Long schoolId);

    @Query("""
        SELECT a FROM Alert a
        WHERE a.notified = FALSE
          AND a.resolved = FALSE
        ORDER BY a.severity DESC, a.createdAt ASC
        """)
    List<Alert> findPendingNotifications();

    @Query("""
        SELECT COUNT(a) FROM Alert a
        JOIN a.sensor s
        JOIN s.room r
        WHERE r.school.id = :schoolId
          AND a.severity = :severity
          AND a.resolved = FALSE
        """)
    long countUnresolvedBySeverity(@Param("schoolId") Long schoolId,
                                   @Param("severity") AlertSeverity severity);

    boolean existsBySensorIdAndAlertTypeAndResolvedFalse(Long sensorId,
            Alert.AlertType alertType);
}


// ============================================================
// repository/SchoolRepository.java
// ============================================================
package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.School;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByActiveTrue();

    Optional<School> findByCnpj(String cnpj);
}


// ============================================================
// dto/SensorReadingRequest.java
// ============================================================
package br.com.escola.airquality.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload enviado pelo sensor (ou gateway IoT) ao registrar uma leitura.
 */
public record SensorReadingRequest(

    @NotBlank(message = "Número de série do sensor é obrigatório")
    @JsonProperty("serial_number")
    String serialNumber,

    @DecimalMin(value = "0.0", message = "CO₂ não pode ser negativo")
    @DecimalMax(value = "50000.0", message = "CO₂ fora do intervalo plausível")
    @JsonProperty("co2_ppm")
    BigDecimal co2Ppm,

    @DecimalMin(value = "0.0")
    @JsonProperty("pm25_ugm3")
    BigDecimal pm25Ugm3,

    @DecimalMin(value = "0.0")
    @JsonProperty("pm10_ugm3")
    BigDecimal pm10Ugm3,

    @DecimalMin(value = "0.0")
    @JsonProperty("tvoc_mgm3")
    BigDecimal tvocMgm3,

    @DecimalMin(value = "-40.0")
    @DecimalMax(value = "85.0")
    @JsonProperty("temperature_c")
    BigDecimal temperatureC,

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @JsonProperty("humidity_pct")
    BigDecimal humidityPct,

    @DecimalMin(value = "0.0")
    @JsonProperty("no2_ppb")
    BigDecimal no2Ppb,

    /** Timestamp da leitura no sensor; usa NOW() se omitido */
    @JsonProperty("recorded_at")
    LocalDateTime recordedAt
) {}


// ============================================================
// dto/SensorReadingResponse.java
// ============================================================
package br.com.escola.airquality.dto;

import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorReadingResponse(
    Long id,

    @JsonProperty("sensor_id")
    Long sensorId,

    @JsonProperty("serial_number")
    String serialNumber,

    @JsonProperty("room_name")
    String roomName,

    @JsonProperty("school_name")
    String schoolName,

    @JsonProperty("co2_ppm")
    BigDecimal co2Ppm,

    @JsonProperty("pm25_ugm3")
    BigDecimal pm25Ugm3,

    @JsonProperty("pm10_ugm3")
    BigDecimal pm10Ugm3,

    @JsonProperty("tvoc_mgm3")
    BigDecimal tvocMgm3,

    @JsonProperty("temperature_c")
    BigDecimal temperatureC,

    @JsonProperty("humidity_pct")
    BigDecimal humidityPct,

    @JsonProperty("no2_ppb")
    BigDecimal no2Ppb,

    @JsonProperty("aqi_value")
    Integer aqiValue,

    @JsonProperty("aqi_category")
    AqiCategory aqiCategory,

    @JsonProperty("aqi_label")
    String aqiLabel,

    @JsonProperty("recorded_at")
    LocalDateTime recordedAt
) {}


// ============================================================
// dto/AlertResponse.java
// ============================================================
package br.com.escola.airquality.dto;

import br.com.escola.airquality.domain.Alert.AlertSeverity;
import br.com.escola.airquality.domain.Alert.AlertType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertResponse(
    Long id,

    @JsonProperty("sensor_id")
    Long sensorId,

    @JsonProperty("serial_number")
    String serialNumber,

    @JsonProperty("room_name")
    String roomName,

    @JsonProperty("school_name")
    String schoolName,

    @JsonProperty("alert_type")
    AlertType alertType,

    AlertSeverity severity,
    String pollutant,

    @JsonProperty("measured_value")
    BigDecimal measuredValue,

    @JsonProperty("threshold_value")
    BigDecimal thresholdValue,

    String message,
    boolean acknowledged,
    boolean resolved,

    @JsonProperty("created_at")
    LocalDateTime createdAt
) {}


// ============================================================
// dto/SchoolSummaryResponse.java
// ============================================================
package br.com.escola.airquality.dto;

import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SchoolSummaryResponse(
    Long id,
    String name,
    String city,
    String state,

    @JsonProperty("total_rooms")
    int totalRooms,

    @JsonProperty("active_sensors")
    int activeSensors,

    @JsonProperty("offline_sensors")
    int offlineSensors,

    @JsonProperty("current_aqi_category")
    AqiCategory currentAqiCategory,

    @JsonProperty("worst_room")
    String worstRoom,

    @JsonProperty("open_alerts")
    int openAlerts,

    @JsonProperty("critical_alerts")
    int criticalAlerts,

    @JsonProperty("room_summaries")
    List<RoomSummary> roomSummaries
) {
    public record RoomSummary(
        Long id,
        String name,

        @JsonProperty("aqi_value")
        Integer aqiValue,

        @JsonProperty("aqi_category")
        AqiCategory aqiCategory,

        @JsonProperty("co2_ppm")
        Double co2Ppm,

        @JsonProperty("temperature_c")
        Double temperatureC
    ) {}
}
