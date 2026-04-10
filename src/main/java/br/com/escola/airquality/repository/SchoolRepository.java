package br.com.escola.airquality.repository;

import br.com.escola.airquality.domain.School;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findByActiveTrue();
}
