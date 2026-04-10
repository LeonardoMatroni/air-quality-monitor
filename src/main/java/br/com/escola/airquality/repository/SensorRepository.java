package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.Sensor;
import br.com.escola.airquality.domain.Sensor.SensorStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findBySerialNumber(String serialNumber);
    List<Sensor> findByStatus(SensorStatus status);
    @Query("SELECT s FROM Sensor s JOIN s.room r WHERE r.school.id = :schoolId AND s.status = 'ACTIVE'")
    List<Sensor> findActiveBySchoolId(@Param("schoolId") Long schoolId);
    @Query("SELECT s FROM Sensor s WHERE s.status = 'ACTIVE' AND (s.lastSeenAt IS NULL OR s.lastSeenAt < :threshold)")
    List<Sensor> findPotentiallyOffline(@Param("threshold") LocalDateTime threshold);
    @Modifying
    @Query("UPDATE Sensor s SET s.lastSeenAt = :ts WHERE s.id = :id")
    void updateLastSeenAt(@Param("id") Long id, @Param("ts") LocalDateTime ts);
    @Modifying
    @Query("UPDATE Sensor s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") SensorStatus status);
}
