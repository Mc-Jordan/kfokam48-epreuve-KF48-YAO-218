package cm.kfokam48.presences.relecture.api.dto;

import cm.kfokam48.presences.exercice.domaine.StatutExercice;
import cm.kfokam48.presences.relecture.domaine.NoteDUnExercice;
import cm.kfokam48.presences.relecture.domaine.RelectureService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Réponse de {@code GET /api/exercices/{id}/relecture}, destinée à l'auteur.
 *
 * <p><strong>Aucun champ n'identifie un relecteur</strong>, ni directement ni
 * indirectement — pas même la date d'attribution, qui rapprochée de l'ordre des
 * présences pourrait le trahir, ni l'ordre des commentaires, qui dirait qui a rendu
 * en premier. RG22 est tenue par la forme de la réponse, et non par une règle
 * applicative qu'on peut oublier.</p>
 *
 * <p>Depuis l'étape 3, la note est la <strong>moyenne</strong> des relectures rendues
 * (RG24), et elle est <strong>provisoire</strong> tant que les deux relecteurs n'ont
 * pas rendu (RG26).</p>
 */
public record RelectureRecueReponse(
        Long exerciceId,
        StatutExercice statut,
        BigDecimal note,
        boolean provisoire,
        int relecturesAttendues,
        int relecturesRendues,
        List<String> commentaires
) {
    public static RelectureRecueReponse de(RelectureService.ResultatConsultation resultat) {
        NoteDUnExercice note = resultat.note();
        return new RelectureRecueReponse(
                resultat.exercice().getId(),
                resultat.exercice().getStatut(),
                note.moyenne(),
                note.provisoire(),
                note.relecturesAttendues(),
                note.relecturesRendues(),
                note.commentaires());
    }
}
