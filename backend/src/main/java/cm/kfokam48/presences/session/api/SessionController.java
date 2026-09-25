package cm.kfokam48.presences.session.api;

import cm.kfokam48.presences.session.api.dto.OuvertureSessionRequete;
import cm.kfokam48.presences.session.api.dto.SessionOuverteReponse;
import cm.kfokam48.presences.session.api.dto.ResultatClotureReponse;
import cm.kfokam48.presences.session.api.dto.ResultatFinalisationReponse;
import cm.kfokam48.presences.session.api.dto.SessionResumeReponse;
import cm.kfokam48.presences.session.domaine.CycleDeVieSessionService;
import cm.kfokam48.presences.presence.api.dto.PresenceDetailReponse;
import cm.kfokam48.presences.presence.domaine.PresenceService;
import cm.kfokam48.presences.session.domaine.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Les opérations de session du contrat.
 *
 * <p>Le contrôleur ne fait que trois choses : valider l'entrée, appeler le service,
 * traduire le résultat en DTO. Aucune requête en base, aucune règle de gestion,
 * aucune entité JPA sérialisée (B3).</p>
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;
    private final CycleDeVieSessionService cycleDeVie;
    private final PresenceService presences;

    public SessionController(SessionService service, CycleDeVieSessionService cycleDeVie,
                             PresenceService presences) {
        this.service = service;
        this.cycleDeVie = cycleDeVie;
        this.presences = presences;
    }

    /** EF1 — {@code POST /api/sessions} : 201, ou 400 si un champ manque. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionOuverteReponse ouvrir(@Valid @RequestBody OuvertureSessionRequete requete) {
        return SessionOuverteReponse.de(service.ouvrir(requete.titre(), requete.promotionId()));
    }

    /** EF1, EF5, EF12 — {@code GET /api/sessions} : 200, ou 404 si la promotion est inconnue. */
    @GetMapping
    public List<SessionResumeReponse> lister(@RequestParam Long promotionId) {
        return service.listerParPromotion(promotionId).stream()
                .map(SessionResumeReponse::de)
                .toList();
    }

    /**
     * EF4, EF12 — {@code GET /api/sessions/{id}/presences} : le détail des présences,
     * source comprise. 200, ou 404 SESSION_INCONNUE.
     */
    @GetMapping("/{sessionId}/presences")
    public List<PresenceDetailReponse> listerPresences(@PathVariable Long sessionId) {
        return presences.listerParSession(sessionId).stream()
                .map(PresenceDetailReponse::de)
                .toList();
    }

    /**
     * EF7 — {@code POST /api/sessions/{id}/cloture} : ferme les dépôts et attribue
     * les relecteurs. 200, 404 SESSION_INCONNUE, 409 SESSION_DEJA_CLOTUREE.
     */
    @PostMapping("/{sessionId}/cloture")
    public ResultatClotureReponse cloturer(@PathVariable Long sessionId) {
        return ResultatClotureReponse.de(cycleDeVie.cloturer(sessionId));
    }

    /**
     * EF10 — {@code POST /api/sessions/{id}/finalisation} : fige les relectures.
     * 200, 404 SESSION_INCONNUE, 409 SESSION_NON_CLOTUREE ou SESSION_DEJA_FINALISEE.
     */
    @PostMapping("/{sessionId}/finalisation")
    public ResultatFinalisationReponse finaliser(@PathVariable Long sessionId) {
        return ResultatFinalisationReponse.de(cycleDeVie.finaliser(sessionId));
    }
}
