package cm.kfokam48.presences.session.api.dto;

import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.StatutSession;

import java.time.Instant;

/**
 * Élément de {@code GET /api/sessions}.
 *
 * <p>Le code de présence n'y figure pas : cette liste alimente les trois écrans,
 * y compris celui de l'étudiant, à qui le code doit être dicté et non affiché.</p>
 */
public record SessionResumeReponse(
        Long id,
        String titre,
        StatutSession statut,
        Instant ouvertureAt,
        Instant expirationAt,
        Instant clotureAt,
        Instant finalisationAt
) {
    public static SessionResumeReponse de(Session session) {
        return new SessionResumeReponse(
                session.getId(), session.getTitre(), session.getStatut(),
                session.getOuvertureAt(), session.getExpirationAt(),
                session.getClotureAt(), session.getFinalisationAt());
    }
}
