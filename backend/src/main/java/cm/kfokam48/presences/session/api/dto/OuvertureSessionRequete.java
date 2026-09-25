package cm.kfokam48.presences.session.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/sessions}, tel que le contrat l'impose :
 * {@code titre} et {@code promotionId}, tous deux obligatoires.
 *
 * <p>Un manquement produit un {@code 400 CHAMP_MANQUANT} (B4, RG25).</p>
 */
public record OuvertureSessionRequete(

        @NotBlank(message = "le titre est obligatoire")
        @Size(max = 200, message = "le titre ne peut pas dépasser 200 caractères")
        String titre,

        @NotNull(message = "la promotion est obligatoire")
        Long promotionId
) {
}
