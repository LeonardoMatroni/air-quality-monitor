// ============================================================
// AirQualityMonitorApplication.java
// ============================================================
package br.com.escola.airquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirQualityMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirQualityMonitorApplication.class, args);
    }
}


// ============================================================
// domain/School.java
// ============================================================
package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "schools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 18)
    private String cnpj;

    private String address;
    private String city;

    @Column(length = 2)
    private String state;

    @Column(name = "zip_code", length = 9)
    private String zipCode;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Room> rooms;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


// ============================================================
// domain/Room.java
// ============================================================
package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private School school;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String floor;

    private Integer capacity;

    @Column(name = "area_m2", precision = 6, scale = 2)
    private BigDecimal areaM2;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 50)
    @Builder.Default
    private RoomType roomType = RoomType.CLASSROOM;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Sensor> sensors;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum RoomType {
        CLASSROOM, LABORATORY, LIBRARY, CAFETERIA, GYM, OFFICE, CORRIDOR, BATHROOM
    }
}


// ============================================================
// domain/Sensor.java
// ============================================================
package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Room room;

    @Column(name = "serial_number", unique = true, nullable = false, length = 100)
    private String serialNumber;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String manufacturer;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Builder.Default
    @Column(name = "measures_co2")
    private boolean measuresCo2 = true;

    @Builder.Default
    @Column(name = "measures_pm25")
    private boolean measuresPm25 = true;

    @Builder.Default
    @Column(name = "measures_pm10")
    private boolean measuresPm10 = true;

    @Builder.Default
    @Column(name = "measures_tvoc")
    private boolean measuresTvoc = false;

    @Builder.Default
    @Column(name = "measures_temp")
    private boolean measuresTemp = true;

    @Builder.Default
    @Column(name = "measures_humidity")
    private boolean measuresHumidity = true;

    @Builder.Default
    @Column(name = "measures_no2")
    private boolean measuresNo2 = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SensorStatus status = SensorStatus.ACTIVE;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "installed_at")
    private LocalDateTime installedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum SensorStatus {
        ACTIVE, INACTIVE, MAINTENANCE, OFFLINE, FAULT
    }
}


// ============================================================
// domain/SensorReading.java
// ============================================================
package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sensor sensor;

    /** CO₂ em partes por milhão */
    @Column(name = "co2_ppm", precision = 7, scale = 2)
    private BigDecimal co2Ppm;

    /** Material particulado fino (≤2,5 µm) em µg/m³ */
    @Column(name = "pm25_ugm3", precision = 7, scale = 3)
    private BigDecimal pm25Ugm3;

    /** Material particulado (≤10 µm) em µg/m³ */
    @Column(name = "pm10_ugm3", precision = 7, scale = 3)
    private BigDecimal pm10Ugm3;

    /** Compostos orgânicos voláteis totais em mg/m³ */
    @Column(name = "tvoc_mgm3", precision = 7, scale = 4)
    private BigDecimal tvocMgm3;

    /** Temperatura em graus Celsius */
    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    /** Umidade relativa em % */
    @Column(name = "humidity_pct", precision = 5, scale = 2)
    private BigDecimal humidityPct;

    /** Dióxido de nitrogênio em ppb */
    @Column(name = "no2_ppb", precision = 7, scale = 3)
    private BigDecimal no2Ppb;

    /** Índice de qualidade do ar calculado (0–500+) */
    @Column(name = "aqi_value")
    private Integer aqiValue;

    /** Categoria textual do AQI */
    @Enumerated(EnumType.STRING)
    @Column(name = "aqi_category", length = 30)
    private AqiCategory aqiCategory;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void prePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    public enum AqiCategory {
        GOOD,               // 0-50
        MODERATE,           // 51-100
        UNHEALTHY_SENSITIVE, // 101-150 — grupos sensíveis
        UNHEALTHY,          // 151-200
        VERY_UNHEALTHY,     // 201-300
        HAZARDOUS           // 301+
    }
}


// ============================================================
// domain/Alert.java
// ============================================================
package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sensor sensor;

    @Column(name = "reading_id")
    private Long readingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 50, nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AlertSeverity severity;

    @Column(length = 20)
    private String pollutant;

    @Column(name = "measured_value", precision = 10, scale = 4)
    private BigDecimal measuredValue;

    @Column(name = "threshold_value", precision = 10, scale = 4)
    private BigDecimal thresholdValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Builder.Default
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder.Default
    private boolean notified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AlertType {
        THRESHOLD_EXCEEDED,
        SENSOR_OFFLINE,
        SENSOR_FAULT,
        RAPID_CHANGE,
        SUSTAINED_POOR_QUALITY
    }

    public enum AlertSeverity {
        INFO, WARNING, CRITICAL, EMERGENCY
    }
}
