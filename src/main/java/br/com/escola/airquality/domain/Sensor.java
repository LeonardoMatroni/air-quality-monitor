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
    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;
    private String model;
    private String manufacturer;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    @Builder.Default @Column(name = "measures_co2") private boolean measuresCo2 = true;
    @Builder.Default @Column(name = "measures_pm25") private boolean measuresPm25 = true;
    @Builder.Default @Column(name = "measures_pm10") private boolean measuresPm10 = true;
    @Builder.Default @Column(name = "measures_tvoc") private boolean measuresTvoc = false;
    @Builder.Default @Column(name = "measures_temp") private boolean measuresTemp = true;
    @Builder.Default @Column(name = "measures_humidity") private boolean measuresHumidity = true;
    @Builder.Default @Column(name = "measures_no2") private boolean measuresNo2 = false;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SensorStatus status = SensorStatus.ACTIVE;
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
    @Column(name = "installed_at")
    private LocalDateTime installedAt;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
    public enum SensorStatus { ACTIVE, INACTIVE, MAINTENANCE, OFFLINE, FAULT }
}
