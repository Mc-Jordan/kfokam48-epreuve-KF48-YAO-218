package cm.kfokam48.presences.presence.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences}, tel que le contrat l'impose.
 *
 * <p>La longueur du code n'est pas contrainte ici : un code de mauvaise longueur
 * doit répondre {@code 400 CODE_INCONNU}, ce que dit RG2, et non
 * {@code CHAMP_MANQUANT}. La validation de forme s'arrête donc à la présence.</p>
 */
public record MarquagePresenceRequete(

        @NotBlank(message = "le code de présence est obligatoire")
        String code,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId
) {
}
