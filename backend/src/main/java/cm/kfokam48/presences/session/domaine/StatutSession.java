package cm.kfokam48.presences.session.domaine;

/**
 * Les trois états d'une session (RG12), dans l'ordre et sans retour en arrière.
 *
 * <p>Le client emploie le mot « clôturer » pour deux actes de gestion distincts :
 * fermer les dépôts (Q12) et mettre fin aux corrections (Q10). Les deux ne pouvant
 * pas être simultanés, ils sont séparés en clôture et finalisation — voir la
 * section 7 du cahier des charges.</p>
 */
public enum StatutSession {

    /** Présences et dépôts acceptés, lien d'exercice remplaçable (RG10, RG11). */
    OUVERTE,

    /** Dépôts fermés, relecteurs attribués, relectures modifiables (RG13, RG20). */
    CLOTUREE,

    /** Relectures figées définitivement (RG21). */
    FINALISEE;

    public boolean estOuverte() {
        return this == OUVERTE;
    }
}
