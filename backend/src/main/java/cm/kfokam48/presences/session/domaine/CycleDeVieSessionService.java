package cm.kfokam48.presences.session.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.partage.erreur.TraducteurDeContraintes;
import cm.kfokam48.presences.presence.domaine.Presence;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
import cm.kfokam48.presences.relecture.domaine.AttributionDesRelectures;
import cm.kfokam48.presences.relecture.domaine.Relecture;
import cm.kfokam48.presences.relecture.domaine.RelectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Les deux actes du formateur qui font avancer une session.
 *
 * <p>Le client emploie le mot « clôturer » pour les deux, mais ils ne peuvent pas
 * être simultanés : la clôture crée les relectures, la finalisation les fige. Voir
 * la contradiction tranchée en section 7 du cahier des charges.</p>
 */
@Service
public class CycleDeVieSessionService {

    private final SessionRepository sessions;
    private final ExerciceRepository exercices;
    private final PresenceRepository presences;
    private final RelectureRepository relectures;
    private final AttributionDesRelectures attribution;
    private final TraducteurDeContraintes traducteur;
    private final Clock horloge;

    public CycleDeVieSessionService(SessionRepository sessions, ExerciceRepository exercices,
                                    PresenceRepository presences, RelectureRepository relectures,
                                    AttributionDesRelectures attribution,
                                    TraducteurDeContraintes traducteur, Clock horloge) {
        this.sessions = sessions;
        this.exercices = exercices;
        this.presences = presences;
        this.relectures = relectures;
        this.attribution = attribution;
        this.traducteur = traducteur;
        this.horloge = horloge;
    }

    /** Ce que la clôture a produit, tel que le contrat le renvoie. */
    public record ResultatCloture(Long sessionId, int exercicesAttribues, int exercicesNonAttribuables) {
    }

    /**
     * EF7 — clôture la session, ferme les dépôts et attribue les relecteurs.
     *
     * <p>Tout se joue dans une seule transaction : une clôture qui attribuerait la
     * moitié des exercices laisserait la séance dans un état qu'aucune règle ne
     * décrit, et que rien ne permettrait de rattraper — l'attribution n'ayant lieu
     * qu'une fois (RG17).</p>
     */
    @Transactional
    public ResultatCloture cloturer(Long sessionId) {
        Session session = sessions.findById(sessionId).orElseThrow(Erreurs::sessionInconnue);
        Instant maintenant = Instant.now(horloge);

        session.cloturer(maintenant);   // RG12, RG13 — refuse une seconde clôture

        List<Exercice> aAttribuer = exercices.findBySessionId(sessionId);
        List<Etudiant> presents = presences.findBySessionIdOrderByEnregistreeAtAsc(sessionId).stream()
                .map(Presence::getEtudiant)
                .toList();

        AttributionDesRelectures.Resultat resultat =
                attribution.attribuer(aAttribuer, presents, maintenant);

        // Deux clôtures concurrentes franchissent toutes deux le contrôle d'état :
        // c'est alors uq_relectures_exercice qui tranche, et son verdict doit se
        // lire SESSION_DEJA_CLOTUREE plutôt que comme une erreur interne.
        traducteur.enTraduisantLesConflits(() -> {
            for (Relecture relecture : resultat.attribuees()) {
                relectures.saveAndFlush(relecture);
                Exercice exercice = relecture.getExercice();
                exercice.enAttenteDeRelecture();
                exercices.save(exercice);
            }
            return null;
        });
        for (Exercice orphelin : resultat.nonAttribuables()) {
            orphelin.devenirNonAttribuable();   // RG17
            exercices.save(orphelin);
        }

        sessions.save(session);
        return new ResultatCloture(sessionId,
                resultat.attribuees().size(), resultat.nonAttribuables().size());
    }

    /** Ce que la finalisation a produit. */
    public record ResultatFinalisation(Long sessionId, int relecturesFigees) {
    }

    /**
     * EF10 — fige définitivement les relectures (RG21).
     *
     * <p>C'est ici que l'intention de Q15 s'applique : la note devient définitive,
     * mais à la finalisation et non à l'envoi. Une relecture jamais rendue n'est pas
     * figée — elle reste en attente, visible du formateur (RG23).</p>
     */
    @Transactional
    public ResultatFinalisation finaliser(Long sessionId) {
        Session session = sessions.findById(sessionId).orElseThrow(Erreurs::sessionInconnue);
        session.finaliser(Instant.now(horloge));   // RG12 — refuse si non clôturée ou déjà finalisée

        List<Relecture> aFiger = relectures.findBySessionId(sessionId);
        int figees = 0;
        for (Relecture relecture : aFiger) {
            var avant = relecture.getStatut();
            relecture.figer();
            if (relecture.getStatut() != avant) {
                relectures.save(relecture);
                figees++;
            }
        }

        sessions.save(session);
        return new ResultatFinalisation(sessionId, figees);
    }
}
