package cm.kfokam48.presences.exercice.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    /** RG7 — un exercice par étudiant et par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findBySessionId(Long sessionId);
}
