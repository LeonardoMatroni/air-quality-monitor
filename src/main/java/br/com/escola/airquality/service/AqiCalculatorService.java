package br.com.escola.airquality.service;

import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Slf4j
@Service
public class AqiCalculatorService {

    public AqiResult calculate(BigDecimal co2Ppm, BigDecimal pm25Ugm3, BigDecimal pm10Ugm3, BigDecimal tvocMgm3, BigDecimal no2Ppb) {
        int maxAqi = 0;
        String worstPollutant = "none";
        if (co2Ppm != null) { int aqi = calculateCo2Aqi(co2Ppm.doubleValue()); if (aqi > maxAqi) { maxAqi = aqi; worstPollutant = "CO2"; } }
        if (pm25Ugm3 != null) { int aqi = calculatePm25Aqi(pm25Ugm3.doubleValue()); if (aqi > maxAqi) { maxAqi = aqi; worstPollutant = "PM2.5"; } }
        if (pm10Ugm3 != null) { int aqi = calculatePm10Aqi(pm10Ugm3.doubleValue()); if (aqi > maxAqi) { maxAqi = aqi; worstPollutant = "PM10"; } }
        if (tvocMgm3 != null) { int aqi = calculateTvocAqi(tvocMgm3.doubleValue()); if (aqi > maxAqi) { maxAqi = aqi; worstPollutant = "TVOC"; } }
        if (no2Ppb != null) { int aqi = calculateNo2Aqi(no2Ppb.doubleValue()); if (aqi > maxAqi) { maxAqi = aqi; worstPollutant = "NO2"; } }
        AqiCategory category = categorize(maxAqi);
        return new AqiResult(maxAqi, category, worstPollutant, describeCategory(category));
    }

    int calculateCo2Aqi(double co2) {
        return linearInterpolate(co2, new double[]{400,800,1000,1500,2000,5000,10000}, new int[]{0,50,100,150,200,300,500});
    }
    int calculatePm25Aqi(double pm25) {
        return linearInterpolate(pm25, new double[]{0.0,12.0,35.4,55.4,150.4,250.4,500.4}, new int[]{0,50,100,150,200,300,500});
    }
    int calculatePm10Aqi(double pm10) {
        return linearInterpolate(pm10, new double[]{0,54,154,254,354,424,604}, new int[]{0,50,100,150,200,300,500});
    }
    int calculateTvocAqi(double tvoc) {
        return linearInterpolate(tvoc, new double[]{0.0,0.3,1.0,3.0,10.0,25.0,50.0}, new int[]{0,50,100,150,200,300,500});
    }
    int calculateNo2Aqi(double no2) {
        return linearInterpolate(no2, new double[]{0,53,100,360,649,1249,2049}, new int[]{0,50,100,150,200,300,500});
    }

    int linearInterpolate(double concentration, double[] concBp, int[] aqiBp) {
        if (concentration <= concBp[0]) return aqiBp[0];
        if (concentration >= concBp[concBp.length-1]) return aqiBp[aqiBp.length-1];
        for (int i = 0; i < concBp.length-1; i++) {
            if (concentration >= concBp[i] && concentration < concBp[i+1]) {
                double ratio = (concentration - concBp[i]) / (concBp[i+1] - concBp[i]);
                return (int) Math.round(aqiBp[i] + ratio * (aqiBp[i+1] - aqiBp[i]));
            }
        }
        return aqiBp[aqiBp.length-1];
    }

    AqiCategory categorize(int aqi) {
        if (aqi <= 50) return AqiCategory.GOOD;
        if (aqi <= 100) return AqiCategory.MODERATE;
        if (aqi <= 150) return AqiCategory.UNHEALTHY_SENSITIVE;
        if (aqi <= 200) return AqiCategory.UNHEALTHY;
        if (aqi <= 300) return AqiCategory.VERY_UNHEALTHY;
        return AqiCategory.HAZARDOUS;
    }

    String describeCategory(AqiCategory category) {
        return switch (category) {
            case GOOD -> "Qualidade do ar satisfatória.";
            case MODERATE -> "Qualidade do ar aceitável.";
            case UNHEALTHY_SENSITIVE -> "Grupos sensíveis podem ter problemas.";
            case UNHEALTHY -> "Toda a população pode sentir efeitos.";
            case VERY_UNHEALTHY -> "Alerta de saúde. Evacuar se possível.";
            case HAZARDOUS -> "EMERGÊNCIA. Evacuar imediatamente.";
        };
    }

    public record AqiResult(int aqiValue, AqiCategory category, String worstPollutant, String description) {
        public boolean isActionRequired() {
            return category == AqiCategory.UNHEALTHY_SENSITIVE || category == AqiCategory.UNHEALTHY
                    || category == AqiCategory.VERY_UNHEALTHY || category == AqiCategory.HAZARDOUS;
        }
        public boolean isEmergency() {
            return category == AqiCategory.HAZARDOUS || category == AqiCategory.VERY_UNHEALTHY;
        }
    }
}
