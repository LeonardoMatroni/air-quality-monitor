// ============================================================
// service/SensorService.java
// ============================================================
package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.*;
import br.com.escola.airquality.domain.Sensor.SensorStatus;
import br.com.escola.airquality.dto.*;
import br.com.escola.airquality.repository.*;
import br.com.escola.airquality.service.AqiCalculatorService.AqiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final AqiCalculatorService aqiCalculator;
    private final AlertService alertService;

    /**
     * Processa uma leitura enviada pelo sensor:
     * 1. Localiza o sensor pelo número de série
     * 2. Calcula o AQI
     * 3. Persiste a leitura
     * 4. Atualiza o timestamp de última atividade
     * 5. Dispara verificação de alertas
     */
    @Transactional
    public SensorReadingResponse processReading(SensorReadingRequest request) {
        Sensor sensor = sensorRepository.findBySerialNumber(request.serialNumber())
                .orElseThrow(() -> new NoSuchElementException(
                        "Sensor não encontrado: " + request.serialNumber()));

        // Reativar sensor que estava marcado como offline
        if (sensor.getStatus() == SensorStatus.OFFLINE) {
            sensor.setStatus(SensorStatus.ACTIVE);
            sensorRepository.save(sensor);
            log.info("Sensor {} voltou a operar", sensor.getSerialNumber());
        }

        // Calcular AQI
        AqiResult aqiResult = aqiCalculator.calculate(
                request.co2Ppm(), request.pm25Ugm3(), request.pm10Ugm3(),
                request.tvocMgm3(), request.no2Ppb());

        // Persistir leitura
        SensorReading reading = SensorReading.builder()
                .sensor(sensor)
                .co2Ppm(request.co2Ppm())
                .pm25Ugm3(request.pm25Ugm3())
                .pm10Ugm3(request.pm10Ugm3())
                .tvocMgm3(request.tvocMgm3())
                .temperatureC(request.temperatureC())
                .humidityPct(request.humidityPct())
                .no2Ppb(request.no2Ppb())
                .aqiValue(aqiResult.aqiValue())
                .aqiCategory(aqiResult.category())
                .recordedAt(request.recordedAt() != null ? request.recordedAt() : LocalDateTime.now())
                .build();

        reading = readingRepository.save(reading);

        // Atualizar last_seen_at do sensor
        sensorRepository.updateLastSeenAt(sensor.getId(), LocalDateTime.now());

        // Disparar análise assíncrona de alertas
        alertService.analyzeReading(sensor, reading, aqiResult);

        log.debug("Leitura {} processada — AQI: {} ({})",
                sensor.getSerialNumber(), aqiResult.aqiValue(), aqiResult.category());

        return toResponse(reading, sensor, aqiResult);
    }

    @Transactional(readOnly = true)
    public SensorReadingResponse getLatestReading(Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoSuchElementException("Sensor não encontrado: " + sensorId));

        SensorReading reading = readingRepository
                .findTopBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElseThrow(() -> new NoSuchElementException("Nenhuma leitura encontrada para o sensor " + sensorId));

        AqiResult aqiResult = aqiCalculator.calculate(
                reading.getCo2Ppm(), reading.getPm25Ugm3(), reading.getPm10Ugm3(),
                reading.getTvocMgm3(), reading.getNo2Ppb());

        return toResponse(reading, sensor, aqiResult);
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> getReadingHistory(Long sensorId,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoSuchElementException("Sensor não encontrado: " + sensorId));

        return readingRepository
                .findBySensorIdAndRecordedAtBetweenOrderByRecordedAtDesc(sensorId, from, to)
                .stream()
                .map(r -> {
                    AqiResult aqi = aqiCalculator.calculate(
                            r.getCo2Ppm(), r.getPm25Ugm3(), r.getPm10Ugm3(),
                            r.getTvocMgm3(), r.getNo2Ppb());
                    return toResponse(r, r.getSensor(), aqi);
                })
                .toList();
    }

    // ----------------------------------------------------------
    // Mapeamento para DTO de resposta
    // ----------------------------------------------------------

    private SensorReadingResponse toResponse(SensorReading reading, Sensor sensor, AqiResult aqiResult) {
        Room room = sensor.getRoom();
        School school = room.getSchool();
        return new SensorReadingResponse(
                reading.getId(),
                sensor.getId(),
                sensor.getSerialNumber(),
                room.getName(),
                school.getName(),
                reading.getCo2Ppm(),
                reading.getPm25Ugm3(),
                reading.getPm10Ugm3(),
                reading.getTvocMgm3(),
                reading.getTemperatureC(),
                reading.getHumidityPct(),
                reading.getNo2Ppb(),
                aqiResult.aqiValue(),
                aqiResult.category(),
                aqiResult.description(),
                reading.getRecordedAt()
        );
    }
}


// ============================================================
// service/AlertService.java
// ============================================================
package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.*;
import br.com.escola.airquality.domain.Alert.*;
import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import br.com.escola.airquality.dto.AlertResponse;
import br.com.escola.airquality.repository.AlertRepository;
import br.com.escola.airquality.service.AqiCalculatorService.AqiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    /**
     * Analisa a leitura recém-persistida e cria alertas conforme necessário.
     * Executado de forma assíncrona para não bloquear a resposta ao sensor.
     */
    @Async
    @Transactional
    public void analyzeReading(Sensor sensor, SensorReading reading, AqiResult aqiResult) {
        List<Alert> alerts = new ArrayList<>();

        // Alerta por AQI geral
        if (aqiResult.isActionRequired()) {
            AlertSeverity severity = determineSeverity(aqiResult.category());
            boolean alreadyOpen = alertRepository.existsBySensorIdAndAlertTypeAndResolvedFalse(
                    sensor.getId(), AlertType.THRESHOLD_EXCEEDED);

            if (!alreadyOpen) {
                Alert alert = Alert.builder()
                        .sensor(sensor)
                        .readingId(reading.getId())
                        .alertType(AlertType.THRESHOLD_EXCEEDED)
                        .severity(severity)
                        .pollutant(aqiResult.worstPollutant())
                        .aqiValue(reading.getAqiValue())
                        .message(buildAlertMessage(sensor, aqiResult))
                        .build();

                alerts.add(alert);
                log.warn("ALERTA [{}/{}] Sensor {}: AQI={} — {}",
                        severity, aqiResult.category(),
                        sensor.getSerialNumber(),
                        aqiResult.aqiValue(), aqiResult.description());
            }
        }

        // Alerta específico: CO₂ elevado independente do AQI geral
        if (reading.getCo2Ppm() != null && reading.getCo2Ppm().doubleValue() > 1500) {
            boolean alreadyOpen = alertRepository.existsBySensorIdAndAlertTypeAndResolvedFalse(
                    sensor.getId(), AlertType.SUSTAINED_POOR_QUALITY);
            if (!alreadyOpen) {
                alerts.add(buildCo2Alert(sensor, reading));
            }
        }

        if (!alerts.isEmpty()) {
            List<Alert> saved = alertRepository.saveAll(alerts);
            notificationService.sendAlertNotifications(saved);
        }
    }

    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado: " + alertId));

        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alerta {} reconhecido por {}", alertId, acknowledgedBy);
        return toResponse(alert);
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado: " + alertId));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alerta {} marcado como resolvido", alertId);
        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnresolvedAlerts(Long schoolId) {
        return alertRepository.findUnresolvedBySchoolId(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private AlertSeverity determineSeverity(AqiCategory category) {
        return switch (category) {
            case UNHEALTHY_SENSITIVE -> AlertSeverity.WARNING;
            case UNHEALTHY           -> AlertSeverity.CRITICAL;
            case VERY_UNHEALTHY,
                 HAZARDOUS           -> AlertSeverity.EMERGENCY;
            default                  -> AlertSeverity.INFO;
        };
    }

    private String buildAlertMessage(Sensor sensor, AqiResult aqiResult) {
        return String.format(
                "Qualidade do ar %s na sala '%s' (escola: %s). AQI=%d — Poluente crítico: %s. %s",
                aqiResult.category(),
                sensor.getRoom().getName(),
                sensor.getRoom().getSchool().getName(),
                aqiResult.aqiValue(),
                aqiResult.worstPollutant(),
                aqiResult.description());
    }

    private Alert buildCo2Alert(Sensor sensor, SensorReading reading) {
        return Alert.builder()
                .sensor(sensor)
                .readingId(reading.getId())
                .alertType(AlertType.SUSTAINED_POOR_QUALITY)
                .severity(AlertSeverity.WARNING)
                .pollutant("CO2")
                .measuredValue(reading.getCo2Ppm())
                .thresholdValue(BigDecimal.valueOf(1500))
                .message(String.format(
                        "CO₂ elevado (%.0f ppm) na sala '%s'. Recomendar ventilação imediata.",
                        reading.getCo2Ppm().doubleValue(),
                        sensor.getRoom().getName()))
                .build();
    }

    private AlertResponse toResponse(Alert alert) {
        Sensor sensor = alert.getSensor();
        Room room = sensor.getRoom();
        School school = room.getSchool();
        return new AlertResponse(
                alert.getId(),
                sensor.getId(),
                sensor.getSerialNumber(),
                room.getName(),
                school.getName(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getPollutant(),
                alert.getMeasuredValue(),
                alert.getThresholdValue(),
                alert.getMessage(),
                alert.isAcknowledged(),
                alert.isResolved(),
                alert.getCreatedAt()
        );
    }

    // Método auxiliar utilizado apenas internamente pelo Alert builder
    // para incluir aqiValue (campo não na entidade; apenas para mensagem)
    @lombok.Builder(builderMethodName = "_ignored")
    static Alert buildWithAqiValue(Sensor sensor, Long readingId,
                                    AlertType alertType, AlertSeverity severity,
                                    String pollutant, Integer aqiValue, String message) {
        Alert a = new Alert();
        a.setSensor(sensor);
        a.setReadingId(readingId);
        a.setAlertType(alertType);
        a.setSeverity(severity);
        a.setPollutant(pollutant);
        a.setMessage(message);
        return a;
    }
}
