package cm.kfokam48.presences.relecture.api;

import cm.kfokam48.presences.relecture.api.dto.RelectureAssigneeReponse;
import cm.kfokam48.presences.relecture.api.dto.RelectureRecueReponse;
import cm.kfokam48.presences.relecture.api.dto.RenduRelectureRequete;
import cm.kfokam48.presences.relecture.domaine.RelectureService;
import cm.kfokam48.presences.relecture.domaine.StatutRelecture;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RelectureController {

    private final RelectureService service;

    public RelectureController(RelectureService service) {
        this.service = service;
    }

    /** EF8 — {@code GET /api/relectures?relecteurId=} : 200, ou 404 ETUDIANT_INCONNU. */
    @GetMapping("/api/relectures")
    public List<RelectureAssigneeReponse> listerPourRelecteur(
            @RequestParam Long relecteurId,
            @RequestParam(required = false) StatutRelecture statut) {
        return service.listerPourRelecteur(relecteurId, statut).stream()
                .map(RelectureAssigneeReponse::de)
                .toList();
    }

    /**
     * EF9 — {@code POST /api/relectures/{id}} : rend ou corrige une relecture.
     *
     * <p>Codes du contrat : {@code 200}, {@code 400 NOTE_INVALIDE},
     * {@code 403 AUTO_RELECTURE}, {@code 409 RELECTURE_DEJA_RENDUE}. Ce dernier
     * ne survient qu'après la finalisation : avant, un second envoi vaut
     * correction et renvoie {@code 200} (RG20, arbitrage Q10 contre Q15).</p>
     */
    @PostMapping("/api/relectures/{relectureId}")
    public void rendre(@PathVariable Long relectureId,
                       @Valid @RequestBody RenduRelectureRequete requete) {
        service.rendre(relectureId, requete.noteEntiere(), requete.commentaire());
    }

    /** EF11 — {@code GET /api/exercices/{id}/relecture} : 200, ou 404 EXERCICE_INCONNU. */
    @GetMapping("/api/exercices/{exerciceId}/relecture")
    public RelectureRecueReponse consulterPourAuteur(@PathVariable Long exerciceId) {
        return RelectureRecueReponse.de(service.consulterPourAuteur(exerciceId));
    }
}
