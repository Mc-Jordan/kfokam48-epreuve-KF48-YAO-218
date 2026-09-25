package cm.kfokam48.presences.tableau.api.dto;

import cm.kfokam48.presences.tableau.domaine.LigneTableau;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Élément de {@code GET /api/tableau} : les six champs du contrat.
 *
 * <p>La moyenne est arrondie au centième ici, et nulle part ailleurs. Le frontend
 * ne la recalcule ni ne l'arrondit : la contrainte {@code F3} interdit de dupliquer
 * une règle de gestion côté client, et un arrondi en est une.</p>
 */
public record LigneTableauReponse(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        BigDecimal moyenne,
        long relecturesEnAttente
) {
    public static LigneTableauReponse de(LigneTableau ligne) {
        return new LigneTableauReponse(
                ligne.etudiantId(),
                ligne.nom(),
                ligne.presences(),
                ligne.exercicesDeposes(),
                // RG24 — sans aucune note reçue, la moyenne est vide, pas zéro.
                ligne.moyenne() == null
                        ? null
                        : BigDecimal.valueOf(ligne.moyenne()).setScale(2, RoundingMode.HALF_UP),
                ligne.relecturesEnAttente());
    }
}
