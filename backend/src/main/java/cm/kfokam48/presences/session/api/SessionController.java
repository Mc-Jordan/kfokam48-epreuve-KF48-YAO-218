package cm.kfokam48.presences.session.api;

import cm.kfokam48.presences.session.api.dto.OuvertureSessionRequete;
import cm.kfokam48.presences.session.api.dto.SessionOuverteReponse;
import cm.kfokam48.presences.session.api.dto.SessionResumeReponse;
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

    public SessionController(SessionService service) {
        this.service = service;
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
}
