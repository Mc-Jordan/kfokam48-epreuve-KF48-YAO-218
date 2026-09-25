package cm.kfokam48.presences.session.api.dto;

import cm.kfokam48.presences.session.domaine.CycleDeVieSessionService;
import cm.kfokam48.presences.session.domaine.StatutSession;

/** Réponse de {@code POST /api/sessions/{id}/finalisation}. */
public record ResultatFinalisationReponse(Long id, StatutSession statut, int relecturesFigees) {

    public static ResultatFinalisationReponse de(CycleDeVieSessionService.ResultatFinalisation resultat) {
        return new ResultatFinalisationReponse(
                resultat.sessionId(), StatutSession.FINALISEE, resultat.relecturesFigees());
    }
}
