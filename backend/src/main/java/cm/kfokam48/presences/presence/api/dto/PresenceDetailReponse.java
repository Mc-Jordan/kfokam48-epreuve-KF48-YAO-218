package cm.kfokam48.presences.presence.api.dto;

import cm.kfokam48.presences.presence.domaine.Presence;
import cm.kfokam48.presences.presence.domaine.SourcePresence;

import java.time.Instant;

/**
 * Élément de {@code GET /api/sessions/{id}/presences}.
 *
 * <p>Q16 demande la présence « à chaque session », alors que {@code GET /api/tableau}
 * impose un simple compteur. Le contrat imposé n'étant pas modifiable, le détail
 * passe par cette opération — c'est aussi le seul endroit où la source
 * {@code FORMATEUR} de Q14 devient visible.</p>
 */
public record PresenceDetailReponse(Long etudiantId, String nom, SourcePresence source, Instant enregistreeAt) {

    public static PresenceDetailReponse de(Presence presence) {
        return new PresenceDetailReponse(
                presence.getEtudiant().getId(),
                presence.getEtudiant().getNom(),
                presence.getSource(),
                presence.getEnregistreeAt());
    }
}
