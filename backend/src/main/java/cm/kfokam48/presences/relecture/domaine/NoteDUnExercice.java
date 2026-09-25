package cm.kfokam48.presences.relecture.domaine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * La note d'un exercice, calculée à partir de ses relectures (RG24).
 *
 * <p>Depuis l'étape 3, un exercice reçoit deux relectures et sa note est leur
 * moyenne. Tant qu'une seule est rendue, cette note s'applique mais reste
 * <strong>provisoire</strong> (RG26) — c'est exactement ce que le client a demandé :
 * « on affiche sa note en attendant, mais marquée comme provisoire ».</p>
 *
 * <p>Le cas d'un seul relecteur attendu n'est pas une anomalie : une séance à deux
 * présents ne permet pas d'en désigner deux (RG17). La note y est alors provisoire
 * à titre définitif, et le formateur doit pouvoir le distinguer d'une relecture
 * simplement en retard.</p>
 */
public record NoteDUnExercice(
        BigDecimal moyenne,
        boolean provisoire,
        int relecturesAttendues,
        int relecturesRendues,
        List<String> commentaires
) {

    /** Aucun relecteur n'a pu être désigné : il n'y a rien à attendre (RG17). */
    public static NoteDUnExercice aucuneRelectureAttendue() {
        return new NoteDUnExercice(null, false, 0, 0, List.of());
    }

    public static NoteDUnExercice de(List<Relecture> relecturesDeLExercice) {
        if (relecturesDeLExercice.isEmpty()) {
            return aucuneRelectureAttendue();
        }

        List<Relecture> rendues = relecturesDeLExercice.stream()
                .filter(relecture -> relecture.getNote() != null)
                .toList();

        return new NoteDUnExercice(
                moyenneDe(rendues),
                // RG26 — provisoire tant que tous les relecteurs attendus n'ont pas
                // rendu. Zéro rendue est aussi « provisoire » : il n'y a pas de note.
                rendues.size() < relecturesDeLExercice.size(),
                relecturesDeLExercice.size(),
                rendues.size(),
                commentairesDe(rendues));
    }

    /** RG24 — moyenne des seules relectures rendues, arrondie au centième. */
    private static BigDecimal moyenneDe(List<Relecture> rendues) {
        return rendues.isEmpty() ? null : BigDecimal.valueOf(
                        rendues.stream().mapToInt(Relecture::getNote).average().orElseThrow())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Les commentaires, dans un ordre stable mais <strong>sans rapport</strong> avec
     * l'ordre de rendu.
     *
     * <p>RG22 interdit d'identifier le relecteur. Conserver l'ordre d'arrivée
     * laisserait deviner qui a rendu en premier, ce qui suffit souvent à le
     * reconnaître dans une promotion que l'on côtoie. On trie donc sur le texte :
     * l'ordre ne dit plus rien de personne, et reste le même d'un appel à l'autre.</p>
     */
    private static List<String> commentairesDe(List<Relecture> rendues) {
        return rendues.stream()
                .map(Relecture::getCommentaire)
                .filter(commentaire -> commentaire != null && !commentaire.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
