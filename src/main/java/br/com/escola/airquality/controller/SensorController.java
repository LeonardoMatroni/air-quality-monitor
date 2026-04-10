package br.com.escola.airquality.controller;

import br.com.escola.airquality.dto.*;
import br.com.escola.airquality.service.SensorService;
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
public class SensorController {
    private final SensorService sensorService;

    @PostMapping("/readings")
    public ResponseEntity<SensorReadingResponse> recordReading(@Valid @RequestBody SensorReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.processReading(request));
    }

    @GetMapping("/{sensorId}/readings/latest")
    public ResponseEntity<SensorReadingResponse> getLatestReading(@PathVariable Long sensorId) {
        return ResponseEntity.ok(sensorService.getLatestReading(sensorId));
    }

    @GetMapping("/{sensorId}/readings/history")
    public ResponseEntity<List<SensorReadingResponse>> getHistory(
            @PathVariable Long sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(sensorService.getReadingHistory(sensorId, from, to));
    }
}
