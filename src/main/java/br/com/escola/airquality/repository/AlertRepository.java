package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.Alert;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    @Query("SELECT a FROM Alert a JOIN a.sensor s JOIN s.room r WHERE r.school.id = :schoolId AND a.resolved = FALSE ORDER BY a.severity DESC, a.createdAt DESC")
    List<Alert> findUnresolvedBySchoolId(@Param("schoolId") Long schoolId);
    @Query("SELECT a FROM Alert a WHERE a.notified = FALSE AND a.resolved = FALSE ORDER BY a.createdAt ASC")
    List<Alert> findPendingNotifications();
    boolean existsBySensorIdAndAlertTypeAndResolvedFalse(Long sensorId, Alert.AlertType alertType);
}
