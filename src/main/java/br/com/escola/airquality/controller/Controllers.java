// ============================================================
// controller/SensorController.java
// ============================================================
package br.com.escola.airquality.controller;

import br.com.escola.airquality.dto.*;
import br.com.escola.airquality.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensores", description = "Recebimento e consulta de leituras dos sensores")
public class SensorController {

    private final SensorService sensorService;

    @PostMapping("/readings")
    @Operation(summary = "Registrar leitura de sensor",
               description = "Endpoint chamado pelo sensor (ou gateway IoT) para enviar dados coletados")
    public ResponseEntity<SensorReadingResponse> recordReading(
            @Valid @RequestBody SensorReadingRequest request) {

        SensorReadingResponse response = sensorService.processReading(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sensorId}/readings/latest")
    @Operation(summary = "Última leitura do sensor")
    public ResponseEntity<SensorReadingResponse> getLatestReading(@PathVariable Long sensorId) {
        return ResponseEntity.ok(sensorService.getLatestReading(sensorId));
    }

    @GetMapping("/{sensorId}/readings/history")
    @Operation(summary = "Histórico de leituras com filtro por período")
    public ResponseEntity<List<SensorReadingResponse>> getHistory(
            @PathVariable Long sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(sensorService.getReadingHistory(sensorId, from, to));
    }
}


// ============================================================
// controller/AlertController.java
// ============================================================
package br.com.escola.airquality.controller;

import br.com.escola.airquality.dto.AlertResponse;
import br.com.escola.airquality.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Gestão de alertas de qualidade do ar")
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/schools/{schoolId}")
    @Operation(summary = "Listar alertas não resolvidos de uma escola")
    public ResponseEntity<List<AlertResponse>> getUnresolvedAlerts(
            @PathVariable Long schoolId) {
        return ResponseEntity.ok(alertService.getUnresolvedAlerts(schoolId));
    }

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(summary = "Reconhecer um alerta")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId, user.getUsername()));
    }

    @PatchMapping("/{alertId}/resolve")
    @Operation(summary = "Marcar alerta como resolvido")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.resolveAlert(alertId));
    }
}


// ============================================================
// controller/SchoolController.java
// ============================================================
package br.com.escola.airquality.controller;

import br.com.escola.airquality.dto.SchoolSummaryResponse;
import br.com.escola.airquality.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
@Tag(name = "Escolas", description = "Dashboard e sumário por escola")
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping
    @Operation(summary = "Listar todas as escolas ativas")
    public ResponseEntity<List<SchoolSummaryResponse>> listSchools() {
        return ResponseEntity.ok(schoolService.listAllActive());
    }

    @GetMapping("/{schoolId}/dashboard")
    @Operation(summary = "Dashboard completo da escola com leituras atuais por sala")
    public ResponseEntity<SchoolSummaryResponse> getDashboard(@PathVariable Long schoolId) {
        return ResponseEntity.ok(schoolService.getDashboard(schoolId));
    }
}


// ============================================================
// controller/ReportController.java
// ============================================================
package br.com.escola.airquality.controller;

import br.com.escola.airquality.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Geração de relatórios de qualidade do ar")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/schools/{schoolId}/daily")
    @Operation(summary = "Gerar relatório diário de qualidade do ar")
    public ResponseEntity<byte[]> generateDailyReport(
            @PathVariable Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        byte[] report = reportService.generateDailyReport(schoolId, date);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"relatorio-ar-" + date + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    @GetMapping("/schools/{schoolId}/weekly")
    @Operation(summary = "Gerar relatório semanal resumido")
    public ResponseEntity<byte[]> generateWeeklyReport(
            @PathVariable Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        byte[] report = reportService.generateWeeklyReport(schoolId, weekStart);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"relatorio-semanal-" + weekStart + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }
}


// ============================================================
// service/NotificationService.java
// ============================================================
package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.Alert;
import br.com.escola.airquality.domain.Alert.AlertSeverity;
import br.com.escola.airquality.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertRepository alertRepository;

    /**
     * Envia notificações por todos os canais configurados.
     * Usa REQUIRES_NEW para garantir que falhas na notificação
     * não revertam a transação da leitura do sensor.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAlertNotifications(List<Alert> alerts) {
        alerts.forEach(alert -> {
            try {
                sendWebSocketAlert(alert);
                if (alert.getSeverity() == AlertSeverity.CRITICAL ||
                    alert.getSeverity() == AlertSeverity.EMERGENCY) {
                    sendEmailAlert(alert);
                }
                // Marcar como notificado
                alert.setNotified(true);
                alertRepository.save(alert);
            } catch (Exception e) {
                log.error("Falha ao enviar notificação para alerta {}: {}", alert.getId(), e.getMessage());
            }
        });
    }

    /** Push via WebSocket para o dashboard em tempo real. */
    private void sendWebSocketAlert(Alert alert) {
        String destination = "/topic/alerts/" + alert.getSensor().getRoom().getSchool().getId();
        messagingTemplate.convertAndSend(destination, buildWebSocketPayload(alert));
        log.debug("WebSocket enviado para {} — alerta {}", destination, alert.getId());
    }

    /** E-mail para diretoria/responsáveis em alertas críticos. */
    private void sendEmailAlert(Alert alert) {
        String schoolEmail = alert.getSensor().getRoom().getSchool().getEmail();
        if (schoolEmail == null || schoolEmail.isBlank()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(schoolEmail);
        message.setSubject("[" + alert.getSeverity() + "] Alerta Qualidade do Ar — " +
                alert.getSensor().getRoom().getSchool().getName());
        message.setText(buildEmailBody(alert));

        mailSender.send(message);
        log.info("E-mail de alerta enviado para {}", schoolEmail);
    }

    private String buildWebSocketPayload(Alert alert) {
        return String.format(
                """
                {"alertId":%d,"severity":"%s","room":"%s","message":"%s","timestamp":"%s"}""",
                alert.getId(),
                alert.getSeverity(),
                alert.getSensor().getRoom().getName(),
                alert.getMessage().replace("\"", "'"),
                alert.getCreatedAt()
        );
    }

    private String buildEmailBody(Alert alert) {
        return String.format("""
                ALERTA DE QUALIDADE DO AR
                ─────────────────────────────────────────
                Escola:      %s
                Sala:        %s
                Sensor:      %s
                Severidade:  %s
                Poluente:    %s
                ─────────────────────────────────────────
                %s
                ─────────────────────────────────────────
                Acesse o painel para mais detalhes e ações recomendadas.
                """,
                alert.getSensor().getRoom().getSchool().getName(),
                alert.getSensor().getRoom().getName(),
                alert.getSensor().getSerialNumber(),
                alert.getSeverity(),
                alert.getPollutant() != null ? alert.getPollutant() : "—",
                alert.getMessage()
        );
    }
}


// ============================================================
// scheduler/AirQualityScheduler.java
// ============================================================
package br.com.escola.airquality.scheduler;

import br.com.escola.airquality.domain.Alert;
import br.com.escola.airquality.domain.Sensor.SensorStatus;
import br.com.escola.airquality.repository.*;
import br.com.escola.airquality.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirQualityScheduler {

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    @Value("${air-quality.aqi.data-retention-days:365}")
    private int dataRetentionDays;

    /**
     * A cada 5 minutos: verificar sensores que pararam de enviar dados.
     * Sensor sem leituras por mais de 15 minutos → OFFLINE.
     */
    @Scheduled(fixedRateString = "${air-quality.aqi.check-interval-ms:300000}")
    @Transactional
    public void checkOfflineSensors() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<br.com.escola.airquality.domain.Sensor> potentiallyOffline =
                sensorRepository.findPotentiallyOffline(threshold);

        for (var sensor : potentiallyOffline) {
            sensorRepository.updateStatus(sensor.getId(), SensorStatus.OFFLINE);

            // Verificar se já existe alerta de offline aberto
            boolean alreadyAlerted = alertRepository
                    .existsBySensorIdAndAlertTypeAndResolvedFalse(sensor.getId(),
                            Alert.AlertType.SENSOR_OFFLINE);

            if (!alreadyAlerted) {
                Alert alert = Alert.builder()
                        .sensor(sensor)
                        .alertType(Alert.AlertType.SENSOR_OFFLINE)
                        .severity(Alert.AlertSeverity.WARNING)
                        .message("Sensor " + sensor.getSerialNumber() +
                                " na sala '" + sensor.getRoom().getName() +
                                "' está offline (sem dados por > 15 minutos).")
                        .build();

                Alert saved = alertRepository.save(alert);
                notificationService.sendAlertNotifications(List.of(saved));
                log.warn("Sensor {} marcado como OFFLINE", sensor.getSerialNumber());
            }
        }

        if (!potentiallyOffline.isEmpty()) {
            log.info("Checagem offline: {} sensor(es) marcado(s) como OFFLINE", potentiallyOffline.size());
        }
    }

    /**
     * Todo dia à 1h da manhã: limpar leituras mais antigas que o período de retenção.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanOldReadings() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(dataRetentionDays);
        int deleted = readingRepository.deleteOlderThan(cutoff);
        log.info("Limpeza de dados: {} leitura(s) anteriores a {} removida(s)", deleted, cutoff.toLocalDate());
    }

    /**
     * Reenviar notificações pendentes que possam ter falhado.
     * Executa a cada 10 minutos.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void retryPendingNotifications() {
        List<Alert> pending = alertRepository.findPendingNotifications();
        if (!pending.isEmpty()) {
            log.info("Reenviando {} notificação(ões) pendente(s)", pending.size());
            notificationService.sendAlertNotifications(pending);
        }
    }
}


// ============================================================
// config/WebSocketConfig.java
// ============================================================
package br.com.escola.airquality.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}


// ============================================================
// config/AirQualityProperties.java
// ============================================================
package br.com.escola.airquality.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "air-quality")
public class AirQualityProperties {

    private Aqi aqi = new Aqi();
    private Thresholds thresholds = new Thresholds();

    @Data
    public static class Aqi {
        private long checkIntervalMs = 60_000;
        private String reportCron = "0 0 7 * * MON-FRI";
        private int dataRetentionDays = 365;
    }

    @Data
    public static class Thresholds {
        private Co2Thresholds co2 = new Co2Thresholds();
        private PmThresholds pm25 = new PmThresholds();
        private PmThresholds pm10 = new PmThresholds();
        private TvocThresholds tvoc = new TvocThresholds();

        @Data
        public static class Co2Thresholds {
            private double good = 800;
            private double moderate = 1000;
            private double unhealthySensitive = 1500;
            private double unhealthy = 2000;
            private double hazardous = 5000;
        }

        @Data
        public static class PmThresholds {
            private double good;
            private double moderate;
            private double unhealthySensitive;
            private double unhealthy;
            private double hazardous;
        }

        @Data
        public static class TvocThresholds {
            private double good = 0.3;
            private double moderate = 1.0;
            private double unhealthySensitive = 3.0;
            private double unhealthy = 10.0;
            private double hazardous = 25.0;
        }
    }
}
