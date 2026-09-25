package cm.kfokam48.presences.presence.api;

import cm.kfokam48.presences.presence.api.dto.MarquagePresenceRequete;
import cm.kfokam48.presences.presence.api.dto.PresenceReponse;
import cm.kfokam48.presences.presence.domaine.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    /**
     * EF2 — {@code POST /api/presences}.
     *
     * <p>Codes du contrat : {@code 201}, {@code 400 CODE_INCONNU},
     * {@code 409 DEJA_PRESENT}, {@code 410 CODE_EXPIRE}. Le
     * {@code 429 TROP_DE_TENTATIVES} viendra avec le ticket #14.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceReponse marquer(@Valid @RequestBody MarquagePresenceRequete requete) {
        return PresenceReponse.de(service.marquer(requete.code(), requete.etudiantId()));
    }
}
