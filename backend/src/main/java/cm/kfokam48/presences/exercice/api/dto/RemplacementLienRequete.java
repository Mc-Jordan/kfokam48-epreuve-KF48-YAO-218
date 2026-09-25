package cm.kfokam48.presences.exercice.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de {@code PUT /api/exercices/{id}}.
 *
 * <p>Comme au dépôt, la forme du lien n'est pas contrôlée par annotation : un lien
 * mal formé doit répondre {@code 400 LIEN_INVALIDE} (RG9), pas
 * {@code CHAMP_MANQUANT}.</p>
 */
public record RemplacementLienRequete(

        @NotBlank(message = "le lien est obligatoire")
        String lien
) {
}
