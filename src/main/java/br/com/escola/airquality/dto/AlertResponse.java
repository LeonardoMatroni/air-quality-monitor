package br.com.escola.airquality.dto;

import br.com.escola.airquality.domain.Alert.AlertSeverity;
import br.com.escola.airquality.domain.Alert.AlertType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertResponse(
    Long id,
    @JsonProperty("sensor_id") Long sensorId,
    @JsonProperty("serial_number") String serialNumber,
    @JsonProperty("room_name") String roomName,
    @JsonProperty("school_name") String schoolName,
    @JsonProperty("alert_type") AlertType alertType,
    AlertSeverity severity,
    String pollutant,
    @JsonProperty("measured_value") BigDecimal measuredValue,
    @JsonProperty("threshold_value") BigDecimal thresholdValue,
    String message,
    boolean acknowledged,
    boolean resolved,
    @JsonProperty("created_at") LocalDateTime createdAt
) {}
