package cm.kfokam48.presences.relecture.domaine;

/**
 * Le cycle de vie d'une relecture.
 *
 * <p>Les trois valeurs correspondent à la contrainte {@code ck_relectures_statut}
 * de la migration V1.</p>
 */
public enum StatutRelecture {

    /**
     * Le relecteur a été tiré au sort, il n'a pas encore rendu. C'est cet état qui
     * matérialise l'attente que Q11 exige de rendre visible au formateur (RG23).
     */
    ATTRIBUEE,

    /** Note et commentaire rendus, encore modifiables tant que la session vit (RG20). */
    RENDUE,

    /** Figée par la finalisation. Plus aucune modification (RG21). */
    FIGEE
}
