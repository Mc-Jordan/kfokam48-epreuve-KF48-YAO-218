package cm.kfokam48.presences.exercice.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.partage.erreur.TraducteurDeContraintes;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Le dépôt d'exercice.
 *
 * <p>Deux règles issues d'arbitrages du cahier des charges se croisent ici :</p>
 * <ul>
 *   <li><strong>RG8</strong> — le dépôt exige une présence enregistrée, quelle qu'en
 *       soit la source. C'est l'arbitrage du trou n°3 : Q12 autorise le dépôt tardif,
 *       Q7 réserve la relecture aux présents, et rien ne traitait le croisement.
 *       Exiger la présence garantit qu'il y a toujours au moins autant de présents
 *       que d'exercices, donc qu'une répartition sans doublon existe (RG16).</li>
 *   <li><strong>RG11</strong> — le dépôt reste possible après l'expiration du code,
 *       jusqu'à la clôture. L'expiration ferme le marquage de présence, pas le dépôt.</li>
 * </ul>
 */
@Service
public class ExerciceService {

    private final ExerciceRepository exercices;
    private final SessionRepository sessions;
    private final PresenceRepository presences;
    private final EtudiantService etudiants;
    private final ValidateurDeLien validateur;
    private final TraducteurDeContraintes traducteur;
    private final Clock horloge;

    public ExerciceService(ExerciceRepository exercices, SessionRepository sessions,
                           PresenceRepository presences, EtudiantService etudiants,
                           ValidateurDeLien validateur, TraducteurDeContraintes traducteur,
                           Clock horloge) {
        this.exercices = exercices;
        this.sessions = sessions;
        this.presences = presences;
        this.etudiants = etudiants;
        this.validateur = validateur;
        this.traducteur = traducteur;
        this.horloge = horloge;
    }

    /** EF5 — l'étudiant dépose le lien de son exercice. */
    @Transactional
    public Exercice deposer(Long sessionId, Long etudiantId, String lien) {
        String lienValide = validateur.valider(lien);

        Session session = sessions.findById(sessionId).orElseThrow(Erreurs::sessionInconnue);
        Etudiant etudiant = etudiants.parIdentifiant(etudiantId);

        // RG11 — l'expiration du code ne ferme pas le dépôt ; la clôture, si.
        if (!session.getStatut().estOuverte()) {
            throw Erreurs.sessionFermee();
        }

        // RG8 — arbitrage du trou n°3 : pas de dépôt sans présence.
        if (!presences.existsBySessionIdAndEtudiantId(sessionId, etudiant.getId())) {
            throw Erreurs.nonPresent();
        }

        // RG7 — un exercice par étudiant et par session.
        if (exercices.existsBySessionIdAndEtudiantId(sessionId, etudiant.getId())) {
            throw Erreurs.exerciceDejaDepose();
        }

        // Même motif qu'au marquage de présence : le contrôle sert le cas courant,
        // la contrainte tranche le cas concurrent, et son verdict devient RG7.
        return traducteur.enTraduisantLesConflits(() -> exercices.saveAndFlush(
                Exercice.deposer(session, etudiant, lienValide, Instant.now(horloge))));
    }

    /**
     * EF6 — l'étudiant remplace le lien de son exercice.
     *
     * <p>RG10 : le remplacement est libre tant que la séance est ouverte, et cesse
     * à la clôture. Q13 le formule autrement — « tant que personne n'a commencé à
     * relire » — mais les deux coïncident ici : le tirage n'ayant lieu qu'à la
     * clôture (arbitrage du trou n°2), personne ne peut avoir commencé avant.
     * C'est ce qui rend Q13 applicable sans avoir à définir « commencer ».</p>
     */
    @Transactional
    public Exercice remplacerLien(Long exerciceId, String lien) {
        String lienValide = validateur.valider(lien);

        Exercice exercice = exercices.findById(exerciceId).orElseThrow(Erreurs::exerciceInconnu);

        if (!exercice.getSession().getStatut().estOuverte()) {
            throw Erreurs.remplacementImpossible();
        }

        exercice.remplacerLien(lienValide, Instant.now(horloge));
        return exercices.save(exercice);
    }
}
