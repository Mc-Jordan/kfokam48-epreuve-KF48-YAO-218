package cm.kfokam48.presences.partage.erreur;

import static cm.kfokam48.presences.partage.erreur.CodeErreur.*;

/**
 * Fabriques d'exceptions métier, nommées d'après la règle de gestion qu'elles
 * font respecter. Un service lève {@code Erreurs.codeExpire()} plutôt que de
 * construire une exception à la main : le code d'erreur reste ainsi attaché à
 * un seul endroit, et la règle se lit dans l'appel.
 */
public final class Erreurs {

    private Erreurs() {
    }

    /** RG1 — le code expire quinze minutes après l'ouverture. */
    public static ExceptionMetier codeExpire() {
        return new ExceptionMetier(CODE_EXPIRE);
    }

    /** RG2 — aucune session ne porte ce code. */
    public static ExceptionMetier codeInconnu() {
        return new ExceptionMetier(CODE_INCONNU);
    }

    /** RG3 — une présence par étudiant et par session. */
    public static ExceptionMetier dejaPresent() {
        return new ExceptionMetier(DEJA_PRESENT);
    }

    /** RG6 — cinq échecs consécutifs bloquent deux minutes. */
    public static ExceptionMetier tropDeTentatives() {
        return new ExceptionMetier(TROP_DE_TENTATIVES);
    }

    /** RG7 — un exercice par étudiant et par session. */
    public static ExceptionMetier exerciceDejaDepose() {
        return new ExceptionMetier(EXERCICE_DEJA_DEPOSE);
    }

    /** RG8 — le dépôt exige une présence enregistrée. */
    public static ExceptionMetier nonPresent() {
        return new ExceptionMetier(NON_PRESENT);
    }

    /** RG9 — le lien est une adresse http ou https absolue. */
    public static ExceptionMetier lienInvalide() {
        return new ExceptionMetier(LIEN_INVALIDE);
    }

    /** RG10 — le lien n'est plus remplaçable après la clôture. */
    public static ExceptionMetier remplacementImpossible() {
        return new ExceptionMetier(REMPLACEMENT_IMPOSSIBLE);
    }

    /** RG11 — plus aucun dépôt après la clôture. */
    public static ExceptionMetier sessionFermee() {
        return new ExceptionMetier(SESSION_FERMEE);
    }

    /** RG12 — la clôture n'a lieu qu'une fois. */
    public static ExceptionMetier sessionDejaCloturee() {
        return new ExceptionMetier(SESSION_DEJA_CLOTUREE);
    }

    /** RG12 — on ne finalise que ce qui est clôturé. */
    public static ExceptionMetier sessionNonCloturee() {
        return new ExceptionMetier(SESSION_NON_CLOTUREE);
    }

    /** RG12 — la finalisation n'a lieu qu'une fois. */
    public static ExceptionMetier sessionDejaFinalisee() {
        return new ExceptionMetier(SESSION_DEJA_FINALISEE);
    }

    /** RG18 — la note est un entier de 0 à 20. */
    public static ExceptionMetier noteInvalide() {
        return new ExceptionMetier(NOTE_INVALIDE);
    }

    /** RG19 — jamais relire son propre exercice. */
    public static ExceptionMetier autoRelecture() {
        return new ExceptionMetier(AUTO_RELECTURE);
    }

    /** RG21 — une relecture figée ne bouge plus. */
    public static ExceptionMetier relectureDejaRendue() {
        return new ExceptionMetier(RELECTURE_DEJA_RENDUE);
    }

    public static ExceptionMetier promotionInconnue() {
        return new ExceptionMetier(PROMOTION_INCONNUE);
    }

    public static ExceptionMetier sessionInconnue() {
        return new ExceptionMetier(SESSION_INCONNUE);
    }

    public static ExceptionMetier etudiantInconnu() {
        return new ExceptionMetier(ETUDIANT_INCONNU);
    }

    public static ExceptionMetier exerciceInconnu() {
        return new ExceptionMetier(EXERCICE_INCONNU);
    }

    public static ExceptionMetier relectureInconnue() {
        return new ExceptionMetier(RELECTURE_INCONNUE);
    }
}
