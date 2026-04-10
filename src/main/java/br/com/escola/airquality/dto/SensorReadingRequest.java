package br.com.escola.airquality.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorReadingRequest(
    @NotBlank(message = "Número de série é obrigatório")
    @JsonProperty("serial_number")
    String serialNumber,
    @JsonProperty("co2_ppm") BigDecimal co2Ppm,
    @JsonProperty("pm25_ugm3") BigDecimal pm25Ugm3,
    @JsonProperty("pm10_ugm3") BigDecimal pm10Ugm3,
    @JsonProperty("tvoc_mgm3") BigDecimal tvocMgm3,
    @JsonProperty("temperature_c") BigDecimal temperatureC,
    @JsonProperty("humidity_pct") BigDecimal humidityPct,
    @JsonProperty("no2_ppb") BigDecimal no2Ppb,
    @JsonProperty("recorded_at") LocalDateTime recordedAt
) {}
