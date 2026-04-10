package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.*;
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

    @Transactional
    public SensorReadingResponse processReading(SensorReadingRequest request) {
        Sensor sensor = sensorRepository.findBySerialNumber(request.serialNumber())
                .orElseThrow(() -> new NoSuchElementException("Sensor não encontrado: " + request.serialNumber()));
        AqiResult aqiResult = aqiCalculator.calculate(request.co2Ppm(), request.pm25Ugm3(), request.pm10Ugm3(), request.tvocMgm3(), request.no2Ppb());
        SensorReading reading = SensorReading.builder()
                .sensor(sensor).co2Ppm(request.co2Ppm()).pm25Ugm3(request.pm25Ugm3())
                .pm10Ugm3(request.pm10Ugm3()).tvocMgm3(request.tvocMgm3())
                .temperatureC(request.temperatureC()).humidityPct(request.humidityPct())
                .no2Ppb(request.no2Ppb()).aqiValue(aqiResult.aqiValue()).aqiCategory(aqiResult.category())
                .recordedAt(request.recordedAt() != null ? request.recordedAt() : LocalDateTime.now())
                .build();
        reading = readingRepository.save(reading);
        sensorRepository.updateLastSeenAt(sensor.getId(), LocalDateTime.now());
        return toResponse(reading, sensor, aqiResult);
    }

    @Transactional(readOnly = true)
    public SensorReadingResponse getLatestReading(Long sensorId) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new NoSuchElementException("Sensor não encontrado: " + sensorId));
        SensorReading reading = readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElseThrow(() -> new NoSuchElementException("Nenhuma leitura encontrada"));
        AqiResult aqiResult = aqiCalculator.calculate(reading.getCo2Ppm(), reading.getPm25Ugm3(), reading.getPm10Ugm3(), reading.getTvocMgm3(), reading.getNo2Ppb());
        return toResponse(reading, sensor, aqiResult);
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> getReadingHistory(Long sensorId, LocalDateTime from, LocalDateTime to) {
        return readingRepository.findBySensorIdAndRecordedAtBetweenOrderByRecordedAtDesc(sensorId, from, to)
                .stream().map(r -> {
                    AqiResult aqi = aqiCalculator.calculate(r.getCo2Ppm(), r.getPm25Ugm3(), r.getPm10Ugm3(), r.getTvocMgm3(), r.getNo2Ppb());
                    return toResponse(r, r.getSensor(), aqi);
                }).toList();
    }

    private SensorReadingResponse toResponse(SensorReading reading, Sensor sensor, AqiResult aqiResult) {
        Room room = sensor.getRoom();
        School school = room.getSchool();
        return new SensorReadingResponse(reading.getId(), sensor.getId(), sensor.getSerialNumber(),
                room.getName(), school.getName(), reading.getCo2Ppm(), reading.getPm25Ugm3(),
                reading.getPm10Ugm3(), reading.getTvocMgm3(), reading.getTemperatureC(),
                reading.getHumidityPct(), reading.getNo2Ppb(), aqiResult.aqiValue(),
                aqiResult.category(), aqiResult.description(), reading.getRecordedAt());
    }
}
