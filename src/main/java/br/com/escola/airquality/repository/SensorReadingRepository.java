package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.SensorReading;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    Optional<SensorReading> findTopBySensorIdOrderByRecordedAtDesc(Long sensorId);
    List<SensorReading> findBySensorIdAndRecordedAtBetweenOrderByRecordedAtDesc(Long sensorId, LocalDateTime start, LocalDateTime end);
    @Modifying
    @Query("DELETE FROM SensorReading r WHERE r.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
