package cm.kfokam48.presences.session.api.dto;

import cm.kfokam48.presences.session.domaine.CycleDeVieSessionService;
import cm.kfokam48.presences.session.domaine.StatutSession;

/**
 * Réponse de {@code POST /api/sessions/{id}/cloture}.
 *
 * <p>Le compte des exercices non attribuables est renvoyé explicitement : le
 * formateur doit savoir, au moment où il clôture, que certains exercices ne seront
 * jamais relus (RG17). Le découvrir plus tard au tableau serait trop tard.</p>
 */
public record ResultatClotureReponse(
        Long id,
        StatutSession statut,
        int exercicesAttribues,
        int exercicesNonAttribuables
) {
    public static ResultatClotureReponse de(CycleDeVieSessionService.ResultatCloture resultat) {
        return new ResultatClotureReponse(resultat.sessionId(), StatutSession.CLOTUREE,
                resultat.exercicesAttribues(), resultat.exercicesNonAttribuables());
    }
}
