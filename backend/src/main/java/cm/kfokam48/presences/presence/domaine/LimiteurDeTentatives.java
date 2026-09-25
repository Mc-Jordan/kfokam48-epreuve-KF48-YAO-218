package cm.kfokam48.presences.presence.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * RG6 — après cinq codes erronés consécutifs, l'étudiant attend deux minutes.
 *
 * <p>Q4 en donne la raison : « sinon ils vont deviner les codes entre eux ». La
 * mesure décourage, elle n'empêche pas — le dispositif n'ayant aucune
 * authentification (Q1), c'est tout ce qu'elle peut faire, et le cahier des charges
 * l'assume.</p>
 *
 * <p>Le blocage porte sur <strong>l'étudiant</strong>, non sur le code ni sur
 * l'adresse réseau : bloquer par code serait inopérant puisque c'est justement ce
 * que l'étudiant cherche, et bloquer par adresse punirait toute une salle partageant
 * la même connexion.</p>
 */
@Component
public class LimiteurDeTentatives {

    /** Q4 — « au bout de cinq erreurs ». */
    public static final int ECHECS_TOLERES = 5;

    /** Q4 — « bloquez-le deux minutes ». */
    public static final Duration DUREE_DU_BLOCAGE = Duration.ofMinutes(2);

    private final TentativePresenceRepository tentatives;

    public LimiteurDeTentatives(TentativePresenceRepository tentatives) {
        this.tentatives = tentatives;
    }

    /**
     * Lève {@code 429 TROP_DE_TENTATIVES} si l'étudiant est bloqué.
     *
     * <p>Le compteur repart de zéro sur une réussite : il suffit qu'une tentative
     * réussie figure parmi les cinq dernières pour que le blocage ne s'applique pas.
     * Il repart aussi de lui-même à l'échéance des deux minutes.</p>
     */
    @Transactional(readOnly = true)
    public void verifier(Long etudiantId, Instant maintenant) {
        List<TentativePresence> dernieres =
                tentatives.findByEtudiantIdOrderByTenteeAtDesc(etudiantId, Limit.of(ECHECS_TOLERES));

        if (dernieres.size() < ECHECS_TOLERES) {
            return;
        }
        if (!dernieres.stream().allMatch(TentativePresence::estUnEchec)) {
            return;
        }

        // La plus récente des cinq date le début du blocage.
        Instant finDuBlocage = dernieres.getFirst().getTenteeAt().plus(DUREE_DU_BLOCAGE);
        if (maintenant.isBefore(finDuBlocage)) {
            throw Erreurs.tropDeTentatives();
        }
    }

    /**
     * Trace la tentative.
     *
     * <p>{@code REQUIRES_NEW} : un échec fait remonter une exception qui annulera la
     * transaction appelante. Sans transaction séparée, la trace disparaîtrait avec
     * elle — et le compteur de RG6 ne monterait jamais.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tracer(Etudiant etudiant, String code, boolean reussie, Instant maintenant) {
        tentatives.save(TentativePresence.tracer(etudiant, code, reussie, maintenant));
    }
}
