package br.com.escola.airquality.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "air-quality")
public class AirQualityProperties {
    private Aqi aqi = new Aqi();

    @Data
    public static class Aqi {
        private long checkIntervalMs = 60000;
        private String reportCron = "0 0 7 * * MON-FRI";
        private int dataRetentionDays = 365;
    }
}
