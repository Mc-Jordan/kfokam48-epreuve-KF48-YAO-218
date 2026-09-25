package cm.kfokam48.presences.presence.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    /** RG3 — une présence par étudiant et par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Presence> findBySessionIdOrderByEnregistreeAtAsc(Long sessionId);
}
