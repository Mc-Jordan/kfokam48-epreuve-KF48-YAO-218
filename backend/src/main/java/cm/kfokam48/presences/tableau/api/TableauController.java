package cm.kfokam48.presences.tableau.api;

import cm.kfokam48.presences.tableau.api.dto.LigneTableauReponse;
import cm.kfokam48.presences.tableau.domaine.TableauService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService service;

    public TableauController(TableauService service) {
        this.service = service;
    }

    /** EF12 — {@code GET /api/tableau?promotionId=} : 200, ou 404 PROMOTION_INCONNUE. */
    @GetMapping
    public List<LigneTableauReponse> recapitulatif(@RequestParam Long promotionId) {
        return service.recapitulatif(promotionId).stream()
                .map(LigneTableauReponse::de)
                .toList();
    }
}
