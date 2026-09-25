package cm.kfokam48.presences.presence.api.dto;

import cm.kfokam48.presences.presence.domaine.Presence;
import cm.kfokam48.presences.presence.domaine.SourcePresence;

/** Réponse {@code 201} de {@code POST /api/presences} : les quatre champs du contrat. */
public record PresenceReponse(Long id, Long sessionId, Long etudiantId, SourcePresence source) {

    public static PresenceReponse de(Presence presence) {
        return new PresenceReponse(
                presence.getId(),
                presence.getSession().getId(),
                presence.getEtudiant().getId(),
                presence.getSource());
    }
}
