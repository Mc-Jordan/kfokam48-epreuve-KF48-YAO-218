package cm.kfokam48.presences.relecture.api.dto;

import cm.kfokam48.presences.relecture.domaine.Relecture;
import cm.kfokam48.presences.relecture.domaine.StatutRelecture;

/**
 * Élément de {@code GET /api/relectures?relecteurId=}.
 *
 * <p>Destiné au relecteur : il a besoin du lien à ouvrir et du titre de la séance
 * pour se repérer. Le nom de l'auteur n'y figure pas — la relecture par les pairs
 * gagne à ne pas savoir qui l'on corrige.</p>
 */
public record RelectureAssigneeReponse(
        Long relectureId,
        Long exerciceId,
        String sessionTitre,
        String lien,
        StatutRelecture statut,
        Short note,
        String commentaire
) {
    public static RelectureAssigneeReponse de(Relecture relecture) {
        return new RelectureAssigneeReponse(
                relecture.getId(),
                relecture.getExercice().getId(),
                relecture.getSession().getTitre(),
                relecture.getExercice().getLien(),
                relecture.getStatut(),
                relecture.getNote(),
                relecture.getCommentaire());
    }
}
