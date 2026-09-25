package cm.kfokam48.presences.exercice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/exercices}.
 *
 * <p>La forme du lien n'est pas contrôlée ici : un lien mal formé doit répondre
 * {@code 400 LIEN_INVALIDE} (RG9), pas {@code CHAMP_MANQUANT}. La validation
 * d'annotation s'arrête donc à la présence, et {@code ValidateurDeLien} fait le reste.</p>
 */
public record DepotExerciceRequete(

        @NotNull(message = "la session est obligatoire")
        Long sessionId,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId,

        @NotBlank(message = "le lien est obligatoire")
        String lien
) {
}
