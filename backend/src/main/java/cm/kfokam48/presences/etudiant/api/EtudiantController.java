package cm.kfokam48.presences.etudiant.api;

import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.etudiant.api.dto.EtudiantReponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class EtudiantController {

    private final EtudiantService service;

    public EtudiantController(EtudiantService service) {
        this.service = service;
    }

    /** EF2 — {@code GET /api/promotions/{id}/etudiants} : 200, ou 404 si la promotion est inconnue. */
    @GetMapping("/{promotionId}/etudiants")
    public List<EtudiantReponse> lister(@PathVariable Long promotionId) {
        return service.listerParPromotion(promotionId).stream()
                .map(EtudiantReponse::de)
                .toList();
    }
}
