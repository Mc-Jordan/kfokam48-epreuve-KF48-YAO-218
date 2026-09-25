package cm.kfokam48.presences.tableau.domaine;

/**
 * Une ligne du tableau récapitulatif, telle que la base la calcule.
 *
 * <p>Projection d'une requête unique : la calculer en Java à partir des entités
 * exigerait une lecture par étudiant, ce que {@code ENF2} interdit pour une
 * promotion de soixante étudiants.</p>
 *
 * <p>{@code moyenne} est nullable : un étudiant sans aucune note reçue n'a pas
 * une moyenne de zéro, il n'en a pas (RG24).</p>
 */
public record LigneTableau(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        long relecturesEnAttente,
        /**
         * Vrai dès qu'au moins un exercice de l'étudiant n'a pas reçu toutes ses
         * relectures : la moyenne agrège alors une note qui peut encore changer
         * (RG26). Le formateur doit pouvoir le distinguer d'une moyenne définitive.
         */
        boolean moyenneProvisoire
) {
}
