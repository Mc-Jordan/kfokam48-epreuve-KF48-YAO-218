package cm.kfokam48.presences.presence.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/presences/manuelles}.
 *
 * <p>Pas de code : c'est tout l'objet de l'opération. Q14 décrit l'étudiant dont
 * le téléphone n'a pas fonctionné — exiger le code serait exiger ce qui a échoué.</p>
 */
public record PresenceManuelleRequete(

        @NotNull(message = "la session est obligatoire")
        Long sessionId,

        @NotNull(message = "l'étudiant est obligatoire")
        Long etudiantId
) {
}
