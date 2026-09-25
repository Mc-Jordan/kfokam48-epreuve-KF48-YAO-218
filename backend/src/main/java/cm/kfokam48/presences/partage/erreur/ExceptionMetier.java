package cm.kfokam48.presences.partage.erreur;

/**
 * Racine des exceptions métier. Chacune porte son {@link CodeErreur}, qui porte
 * lui-même son statut HTTP : un service décide de l'erreur, jamais du transport.
 */
public class ExceptionMetier extends RuntimeException {

    private final transient CodeErreur code;

    public ExceptionMetier(CodeErreur code) {
        super(code.messageParDefaut());
        this.code = code;
    }

    public ExceptionMetier(CodeErreur code, String message) {
        super(message);
        this.code = code;
    }

    public CodeErreur code() {
        return code;
    }
}
