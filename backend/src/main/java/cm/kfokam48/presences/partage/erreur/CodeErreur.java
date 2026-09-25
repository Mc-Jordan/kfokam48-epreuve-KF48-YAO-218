package cm.kfokam48.presences.partage.erreur;

import org.springframework.http.HttpStatus;

/**
 * Les codes d'erreur stables du projet, tels que recensés en section 8 du cahier
 * des charges et en fin de {@code api/contrat.yaml}.
 *
 * <p>Cette énumération est la source unique : tout code renvoyé par l'API vient
 * d'ici, et un code présent au contrat mais absent d'ici est un défaut. Le statut
 * HTTP est porté par le code lui-même, pour qu'un contrôleur ne puisse pas en
 * choisir un autre.</p>
 */
public enum CodeErreur {

    // --- 400 ----------------------------------------------------------------
    CHAMP_MANQUANT(HttpStatus.BAD_REQUEST, "Une donnée obligatoire est absente ou invalide."),
    CODE_INCONNU(HttpStatus.BAD_REQUEST, "Ce code de présence ne correspond à aucune session."),
    LIEN_INVALIDE(HttpStatus.BAD_REQUEST, "Le lien doit être une adresse http ou https absolue."),
    NOTE_INVALIDE(HttpStatus.BAD_REQUEST, "La note doit être un nombre entier compris entre 0 et 20."),

    // --- 403 ----------------------------------------------------------------
    NON_PRESENT(HttpStatus.FORBIDDEN, "Vous devez être présent à la session pour y déposer un exercice."),
    AUTO_RELECTURE(HttpStatus.FORBIDDEN, "Un étudiant ne peut pas relire son propre exercice."),

    // --- 404 ----------------------------------------------------------------
    PROMOTION_INCONNUE(HttpStatus.NOT_FOUND, "Cette promotion n'existe pas."),
    SESSION_INCONNUE(HttpStatus.NOT_FOUND, "Cette session n'existe pas."),
    ETUDIANT_INCONNU(HttpStatus.NOT_FOUND, "Cet étudiant n'existe pas."),
    EXERCICE_INCONNU(HttpStatus.NOT_FOUND, "Cet exercice n'existe pas."),
    RELECTURE_INCONNUE(HttpStatus.NOT_FOUND, "Cette relecture n'existe pas."),
    RESSOURCE_INCONNUE(HttpStatus.NOT_FOUND, "Cette ressource n'existe pas."),

    // --- 405 ----------------------------------------------------------------
    METHODE_NON_AUTORISEE(HttpStatus.METHOD_NOT_ALLOWED, "Cette opération n'accepte pas ce verbe HTTP."),

    // --- 409 ----------------------------------------------------------------
    DEJA_PRESENT(HttpStatus.CONFLICT, "Votre présence est déjà enregistrée pour cette session."),
    EXERCICE_DEJA_DEPOSE(HttpStatus.CONFLICT, "Vous avez déjà déposé un exercice pour cette session."),
    RELECTURE_DEJA_RENDUE(HttpStatus.CONFLICT, "La session est finalisée, cette relecture ne peut plus être modifiée."),
    SESSION_FERMEE(HttpStatus.CONFLICT, "Cette session est clôturée."),
    SESSION_DEJA_CLOTUREE(HttpStatus.CONFLICT, "Cette session est déjà clôturée."),
    SESSION_NON_CLOTUREE(HttpStatus.CONFLICT, "Cette session doit être clôturée avant d'être finalisée."),
    SESSION_DEJA_FINALISEE(HttpStatus.CONFLICT, "Cette session est déjà finalisée."),
    REMPLACEMENT_IMPOSSIBLE(HttpStatus.CONFLICT, "Le lien ne peut plus être remplacé, la session est clôturée."),

    // --- 410 ----------------------------------------------------------------
    CODE_EXPIRE(HttpStatus.GONE, "Le code de présence a expiré."),

    // --- 429 ----------------------------------------------------------------
    TROP_DE_TENTATIVES(HttpStatus.TOO_MANY_REQUESTS, "Trop de codes erronés. Réessayez dans deux minutes."),

    // --- 500 ----------------------------------------------------------------
    ERREUR_INTERNE(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");

    private final HttpStatus statut;
    private final String messageParDefaut;

    CodeErreur(HttpStatus statut, String messageParDefaut) {
        this.statut = statut;
        this.messageParDefaut = messageParDefaut;
    }

    public HttpStatus statut() {
        return statut;
    }

    public String messageParDefaut() {
        return messageParDefaut;
    }
}
