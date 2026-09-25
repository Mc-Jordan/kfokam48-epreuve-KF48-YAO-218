package cm.kfokam48.presences.presence.domaine;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TentativePresenceRepository extends JpaRepository<TentativePresence, Long> {

    /**
     * Les dernières tentatives d'un étudiant, de la plus récente à la plus ancienne.
     *
     * <p>On en lit cinq et pas davantage : RG6 ne s'intéresse qu'aux cinq derniers
     * essais. Relire tout l'historique à chaque saisie serait inutile et coûteux.</p>
     */
    List<TentativePresence> findByEtudiantIdOrderByTenteeAtDesc(Long etudiantId, Limit limite);
}
