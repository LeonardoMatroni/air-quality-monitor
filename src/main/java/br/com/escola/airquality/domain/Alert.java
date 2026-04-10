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
    private String pollutant;
    @Column(name = "measured_value", precision = 10, scale = 4)
    private BigDecimal measuredValue;
    @Column(name = "threshold_value", precision = 10, scale = 4)
    private BigDecimal thresholdValue;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Builder.Default private boolean acknowledged = false;
    @Column(name = "acknowledged_by") private String acknowledgedBy;
    @Column(name = "acknowledged_at") private LocalDateTime acknowledgedAt;
    @Builder.Default private boolean resolved = false;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Builder.Default private boolean notified = false;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
    public enum AlertType { THRESHOLD_EXCEEDED, SENSOR_OFFLINE, SENSOR_FAULT, RAPID_CHANGE, SUSTAINED_POOR_QUALITY }
    public enum AlertSeverity { INFO, WARNING, CRITICAL, EMERGENCY }
}
