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
    @Column(name = "co2_ppm", precision = 7, scale = 2)
    private BigDecimal co2Ppm;
    @Column(name = "pm25_ugm3", precision = 7, scale = 3)
    private BigDecimal pm25Ugm3;
    @Column(name = "pm10_ugm3", precision = 7, scale = 3)
    private BigDecimal pm10Ugm3;
    @Column(name = "tvoc_mgm3", precision = 7, scale = 4)
    private BigDecimal tvocMgm3;
    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;
    @Column(name = "humidity_pct", precision = 5, scale = 2)
    private BigDecimal humidityPct;
    @Column(name = "no2_ppb", precision = 7, scale = 3)
    private BigDecimal no2Ppb;
    @Column(name = "aqi_value")
    private Integer aqiValue;
    @Enumerated(EnumType.STRING)
    @Column(name = "aqi_category", length = 30)
    private AqiCategory aqiCategory;
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    @PrePersist
    void prePersist() { if (this.recordedAt == null) this.recordedAt = LocalDateTime.now(); }
    public enum AqiCategory { GOOD, MODERATE, UNHEALTHY_SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }
}
