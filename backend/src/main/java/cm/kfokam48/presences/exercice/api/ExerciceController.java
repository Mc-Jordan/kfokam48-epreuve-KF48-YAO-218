package cm.kfokam48.presences.exercice.api;

import cm.kfokam48.presences.exercice.api.dto.DepotExerciceRequete;
import cm.kfokam48.presences.exercice.api.dto.ExerciceReponse;
import cm.kfokam48.presences.exercice.domaine.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService service;

    public ExerciceController(ExerciceService service) {
        this.service = service;
    }

    /**
     * EF5 — {@code POST /api/exercices}.
     *
     * <p>Codes du contrat : {@code 201}, {@code 400 LIEN_INVALIDE},
     * {@code 403 NON_PRESENT}, {@code 409 EXERCICE_DEJA_DEPOSE}.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciceReponse deposer(@Valid @RequestBody DepotExerciceRequete requete) {
        return ExerciceReponse.de(
                service.deposer(requete.sessionId(), requete.etudiantId(), requete.lien()));
    }
}
