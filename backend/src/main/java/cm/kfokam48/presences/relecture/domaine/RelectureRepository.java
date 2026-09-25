package cm.kfokam48.presences.relecture.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    List<Relecture> findBySessionId(Long sessionId);

    List<Relecture> findByRelecteurIdOrderByAttribueeAtDesc(Long relecteurId);

    Optional<Relecture> findByExerciceId(Long exerciceId);
}
