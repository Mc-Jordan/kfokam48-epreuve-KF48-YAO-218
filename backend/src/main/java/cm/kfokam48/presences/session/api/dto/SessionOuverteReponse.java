package cm.kfokam48.presences.session.api.dto;

import cm.kfokam48.presences.session.domaine.Session;

import java.time.Instant;

/**
 * Réponse {@code 201} de {@code POST /api/sessions}.
 *
 * <p>Les quatre champs du contrat, ni plus ni moins. Le formateur reçoit le code ;
 * l'entité JPA, elle, ne franchit jamais cette frontière (B3).</p>
 */
public record SessionOuverteReponse(
        Long id,
        String code,
        Instant ouvertureAt,
        Instant expirationAt
) {
    public static SessionOuverteReponse de(Session session) {
        return new SessionOuverteReponse(
                session.getId(), session.getCode(),
                session.getOuvertureAt(), session.getExpirationAt());
    }
}
