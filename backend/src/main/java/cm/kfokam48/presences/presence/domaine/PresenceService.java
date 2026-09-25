package cm.kfokam48.presences.presence.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
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
 * <p>Le blocage après cinq échecs (RG6) se placera <strong>avant</strong> la recherche
 * de session, comme D3 l'indique : sans quoi l'écart entre {@code 400} et {@code 410}
 * révélerait à un étudiant bloqué qu'un code existe, ce que Q4 cherche à empêcher.
 * Il arrive avec le ticket #14.</p>
 */
@Service
public class PresenceService {

    private final PresenceRepository presences;
    private final SessionRepository sessions;
    private final EtudiantService etudiants;
    private final Clock horloge;

    public PresenceService(PresenceRepository presences, SessionRepository sessions,
                           EtudiantService etudiants, Clock horloge) {
        this.presences = presences;
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.horloge = horloge;
    }

    /** EF2 — l'étudiant marque sa présence avec le code dicté par le formateur. */
    @Transactional
    public Presence marquer(String code, Long etudiantId) {
        Etudiant etudiant = etudiants.parIdentifiant(etudiantId);
        Instant maintenant = Instant.now(horloge);

        // 1. RG2 — le code désigne-t-il une session ?
        Session session = sessions.findByCode(code.trim().toUpperCase())
                .orElseThrow(Erreurs::codeInconnu);

        // 2. RG1 — avant l'unicité : un code périmé ne vaut plus rien, quoi qu'il arrive.
        if (session.codeExpireA(maintenant)) {
            throw Erreurs.codeExpire();
        }

        // 3. RG3 — une seule présence par étudiant et par session.
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw Erreurs.dejaPresent();
        }

        return presences.save(
                Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
    }

    @Transactional(readOnly = true)
    public List<Presence> listerParSession(Long sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw Erreurs.sessionInconnue();
        }
        return presences.findAvecEtudiantParSession(sessionId);
    }
}
