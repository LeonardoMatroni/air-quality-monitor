package br.com.escola.airquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirQualityMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirQualityMonitorApplication.class, args);
    }
}
