package cm.kfokam48.presences.presence.domaine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /** RG3 — une présence par étudiant et par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Presence> findBySessionIdOrderByEnregistreeAtAsc(Long sessionId);

    /**
     * Les présences d'une séance, <strong>avec</strong> leur étudiant.
     *
     * <p>Le DTO expose le nom, chargé paresseusement, et la conversion a lieu dans
     * le contrôleur — hors transaction. Sans cette jointure, la réponse est un 500.</p>
     */
    @Query("""
            SELECT p FROM Presence p
                JOIN FETCH p.etudiant
            WHERE p.session.id = :sessionId
            ORDER BY p.enregistreeAt ASC
            """)
    List<Presence> findAvecEtudiantParSession(@Param("sessionId") Long sessionId);
}
