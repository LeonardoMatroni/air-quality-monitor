package br.com.escola.airquality.controller;

import br.com.escola.airquality.dto.AlertResponse;
import br.com.escola.airquality.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping("/schools/{schoolId}")
    public ResponseEntity<List<AlertResponse>> getUnresolvedAlerts(@PathVariable Long schoolId) {
        return ResponseEntity.ok(alertService.getUnresolvedAlerts(schoolId));
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledgeAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId, "admin"));
    }

    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.resolveAlert(alertId));
    }
}
