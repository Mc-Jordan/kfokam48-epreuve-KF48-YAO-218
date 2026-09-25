package cm.kfokam48.presences.partage.erreur;

/**
 * Le corps d'erreur imposé par le contrat, pour toutes les erreurs sans exception :
 * {@code { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }}.
 *
 * <p>Applique RG25. Aucun champ supplémentaire : ni trace d'exécution, ni horodatage,
 * ni chemin. Le contrat en impose deux, il y en a deux.</p>
 */
public record ReponseErreur(String code, String message) {

    public static ReponseErreur de(CodeErreur code) {
        return new ReponseErreur(code.name(), code.messageParDefaut());
    }

    public static ReponseErreur de(CodeErreur code, String message) {
        return new ReponseErreur(code.name(), message);
    }
}
