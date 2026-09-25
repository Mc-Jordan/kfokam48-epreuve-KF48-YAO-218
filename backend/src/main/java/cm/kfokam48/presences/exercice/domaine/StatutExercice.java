package cm.kfokam48.presences.exercice.domaine;

/**
 * Le cycle de vie d'un exercice, tel que D4 le décrit.
 *
 * <p>Les quatre valeurs correspondent à la contrainte {@code ck_exercices_statut}
 * de la migration V1. Toute valeur ajoutée ici doit l'être aussi en base et dans
 * D4, faute de quoi {@code CoherenceD2MigrationTest} échoue — c'est son rôle.</p>
 */
public enum StatutExercice {

    /** Déposé, session encore ouverte, lien remplaçable (RG10). */
    DEPOSE,

    /** Un relecteur a été tiré au sort, il n'a pas encore rendu (RG23). */
    EN_ATTENTE_RELECTURE,

    /**
     * Une seule des deux relectures est rendue. La note s'affiche déjà, mais elle
     * est provisoire (RG24, RG26) — c'est exactement ce que le client a demandé.
     */
    RELU_PARTIEL,

    /** Les deux relectures sont rendues. La note est la moyenne, et elle est définitive. */
    RELU,

    /**
     * Aucun relecteur éligible n'existait à la clôture. État terminal (RG17) :
     * l'attribution n'ayant lieu qu'une fois, aucune présence nouvelle ne peut
     * plus le rattraper.
     */
    NON_ATTRIBUABLE
}
