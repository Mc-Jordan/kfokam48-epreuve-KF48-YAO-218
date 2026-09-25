package cm.kfokam48.presences.presence.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import cm.kfokam48.presences.session.domaine.StatutSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Le marquage de présence.
 *
 * <p><strong>L'ordre des contrôles suit le diagramme D3 et n'est pas négociable.</strong>
 * Il porte une garantie de sécurité, pas une préférence de style :</p>
 * <ol>
 *   <li>la session est cherchée par son code — inconnue, c'est {@code 400} (RG2) ;</li>
 *   <li>l'expiration est vérifiée avant l'unicité : un code périmé renvoie {@code 410}
 *       même si l'étudiant était déjà présent, l'information utile étant que le code
 *       ne vaut plus rien (RG1) ;</li>
 *   <li>l'unicité vient en dernier — {@code 409} (RG3).</li>
 * </ol>
 *
 * <p>Le blocage après cinq échecs (RG6) intervient <strong>avant</strong> la recherche
 * de session, comme D3 l'impose : sans quoi l'écart entre {@code 400} et {@code 410}
 * révélerait à un étudiant bloqué qu'un code existe, ce que Q4 cherche à empêcher.</p>
 */
@Service
public class PresenceService {

    private final PresenceRepository presences;
    private final SessionRepository sessions;
    private final EtudiantService etudiants;
    private final LimiteurDeTentatives limiteur;
    private final Clock horloge;

    public PresenceService(PresenceRepository presences, SessionRepository sessions,
                           EtudiantService etudiants, LimiteurDeTentatives limiteur, Clock horloge) {
        this.presences = presences;
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.limiteur = limiteur;
        this.horloge = horloge;
    }

    /** EF2 — l'étudiant marque sa présence avec le code dicté par le formateur. */
    @Transactional
    public Presence marquer(String code, Long etudiantId) {
        Etudiant etudiant = etudiants.parIdentifiant(etudiantId);
        Instant maintenant = Instant.now(horloge);
        String codeNormalise = code.trim().toUpperCase();

        // 1. RG6 — avant toute autre chose. Un étudiant bloqué ne doit rien pouvoir
        //    apprendre de la réponse, pas même qu'un code existe.
        limiteur.verifier(etudiant.getId(), maintenant);

        // 2. RG2 — le code désigne-t-il une session ?
        Session session = sessions.findByCode(codeNormalise).orElseGet(() -> {
            limiteur.tracer(etudiant, codeNormalise, false, maintenant);
            throw Erreurs.codeInconnu();
        });

        // 3. RG1 — avant l'unicité : un code périmé ne vaut plus rien, quoi qu'il arrive.
        if (session.codeExpireA(maintenant)) {
            throw Erreurs.codeExpire();
        }

        // 4. RG3 — une seule présence par étudiant et par session.
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw Erreurs.dejaPresent();
        }

        // RG6 — une réussite remet le compteur à zéro.
        limiteur.tracer(etudiant, codeNormalise, true, maintenant);

        return presences.save(
                Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
    }

    /**
     * EF4 — le formateur enregistre une présence à la main.
     *
     * <p>Ni code, ni contrôle d'expiration : c'est exactement la situation que Q14
     * décrit — « il arrive qu'un étudiant ait un souci de téléphone ». Exiger le
     * code ici serait absurde, puisque c'est souvent lui qui a échoué.</p>
     *
     * <p>La présence porte {@code source = FORMATEUR} (RG4) : l'ajout doit se voir,
     * le client l'a demandé explicitement. La seule limite est la clôture — après
     * elle, la composition de la séance est figée et sert au tirage (RG13).</p>
     */
    @Transactional
    public Presence enregistrerManuellement(Long sessionId, Long etudiantId) {
        Session session = sessions.findById(sessionId).orElseThrow(Erreurs::sessionInconnue);
        Etudiant etudiant = etudiants.parIdentifiant(etudiantId);

        // RG5 — possible même après l'expiration du code, mais pas après la clôture.
        if (session.getStatut() != StatutSession.OUVERTE) {
            throw Erreurs.sessionFermee();
        }

        // RG3 — l'unicité vaut quelle que soit la source.
        if (presences.existsBySessionIdAndEtudiantId(sessionId, etudiant.getId())) {
            throw Erreurs.dejaPresent();
        }

        return presences.save(Presence.enregistrer(
                session, etudiant, SourcePresence.FORMATEUR, Instant.now(horloge)));
    }

    @Transactional(readOnly = true)
    public List<Presence> listerParSession(Long sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw Erreurs.sessionInconnue();
        }
        return presences.findAvecEtudiantParSession(sessionId);
    }
}
