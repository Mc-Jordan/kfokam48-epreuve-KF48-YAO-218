package cm.kfokam48.presences.presence.api;

import cm.kfokam48.presences.presence.api.dto.MarquagePresenceRequete;
import cm.kfokam48.presences.presence.api.dto.PresenceManuelleRequete;
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

    /**
     * EF4 — {@code POST /api/presences/manuelles} : le formateur enregistre une
     * présence sans code.
     *
     * <p>Chemin distinct de {@code POST /api/presences}, qui reste inchangé.
     * Codes : {@code 201}, {@code 400 CHAMP_MANQUANT},
     * {@code 404 SESSION_INCONNUE} ou {@code ETUDIANT_INCONNU},
     * {@code 409 DEJA_PRESENT} ou {@code SESSION_FERMEE}.</p>
     */
    @PostMapping("/manuelles")
    @ResponseStatus(HttpStatus.CREATED)
    public PresenceReponse enregistrerManuellement(@Valid @RequestBody PresenceManuelleRequete requete) {
        return PresenceReponse.de(
                service.enregistrerManuellement(requete.sessionId(), requete.etudiantId()));
    }
}
