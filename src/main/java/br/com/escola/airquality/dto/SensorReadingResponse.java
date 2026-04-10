package br.com.escola.airquality.dto;

import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorReadingResponse(
    Long id,
    @JsonProperty("sensor_id") Long sensorId,
    @JsonProperty("serial_number") String serialNumber,
    @JsonProperty("room_name") String roomName,
    @JsonProperty("school_name") String schoolName,
    @JsonProperty("co2_ppm") BigDecimal co2Ppm,
    @JsonProperty("pm25_ugm3") BigDecimal pm25Ugm3,
    @JsonProperty("pm10_ugm3") BigDecimal pm10Ugm3,
    @JsonProperty("tvoc_mgm3") BigDecimal tvocMgm3,
    @JsonProperty("temperature_c") BigDecimal temperatureC,
    @JsonProperty("humidity_pct") BigDecimal humidityPct,
    @JsonProperty("no2_ppb") BigDecimal no2Ppb,
    @JsonProperty("aqi_value") Integer aqiValue,
    @JsonProperty("aqi_category") AqiCategory aqiCategory,
    @JsonProperty("aqi_label") String aqiLabel,
    @JsonProperty("recorded_at") LocalDateTime recordedAt
) {}
