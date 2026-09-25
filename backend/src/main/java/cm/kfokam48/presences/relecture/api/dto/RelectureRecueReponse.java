package cm.kfokam48.presences.relecture.api.dto;

import cm.kfokam48.presences.exercice.domaine.StatutExercice;
import cm.kfokam48.presences.relecture.domaine.RelectureService;

/**
 * Réponse de {@code GET /api/exercices/{id}/relecture}, destinée à l'auteur.
 *
 * <p><strong>Aucun champ n'identifie le relecteur</strong>, ni directement ni
 * indirectement — pas même la date d'attribution, qui rapprochée de l'ordre des
 * présences pourrait le trahir. RG22 est ainsi tenue par la forme de la réponse,
 * et non par une règle applicative qu'on peut oublier.</p>
 */
public record RelectureRecueReponse(
        Long exerciceId,
        StatutExercice statut,
        Short note,
        String commentaire
) {
    public static RelectureRecueReponse de(RelectureService.ResultatConsultation resultat) {
        var relecture = resultat.relecture();
        return new RelectureRecueReponse(
                resultat.exercice().getId(),
                resultat.exercice().getStatut(),
                relecture == null ? null : relecture.getNote(),
                relecture == null ? null : relecture.getCommentaire());
    }
}
