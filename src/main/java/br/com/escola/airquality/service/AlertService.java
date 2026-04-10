package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.*;
import br.com.escola.airquality.domain.Alert.*;
import br.com.escola.airquality.dto.AlertResponse;
import br.com.escola.airquality.repository.AlertRepository;
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
public class AlertService {
    private final AlertRepository alertRepository;

    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public AlertResponse resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado: " + alertId));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnresolvedAlerts(Long schoolId) {
        return alertRepository.findUnresolvedBySchoolId(schoolId).stream().map(this::toResponse).toList();
    }

    private AlertResponse toResponse(Alert alert) {
        Sensor sensor = alert.getSensor();
        Room room = sensor.getRoom();
        School school = room.getSchool();
        return new AlertResponse(alert.getId(), sensor.getId(), sensor.getSerialNumber(),
                room.getName(), school.getName(), alert.getAlertType(), alert.getSeverity(),
                alert.getPollutant(), alert.getMeasuredValue(), alert.getThresholdValue(),
                alert.getMessage(), alert.isAcknowledged(), alert.isResolved(), alert.getCreatedAt());
    }
}
