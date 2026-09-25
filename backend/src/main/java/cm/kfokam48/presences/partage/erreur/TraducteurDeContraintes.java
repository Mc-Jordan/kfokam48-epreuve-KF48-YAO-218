package cm.kfokam48.presences.partage.erreur;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Traduit une violation de contrainte de base en code d'erreur du contrat.
 *
 * <h2>Pourquoi ce n'est pas une rustine</h2>
 *
 * <p>Vérifier l'unicité avant d'écrire ne peut pas suffire : entre la lecture et
 * l'écriture, une autre requête peut insérer la même ligne. Le contrôle applicatif
 * répond correctement dans le cas courant, la contrainte de base tranche le cas
 * concurrent — mais il fallait encore traduire son verdict dans le langage du
 * contrat, faute de quoi le client recevait {@code 500 ERREUR_INTERNE} là où la
 * règle dit {@code 409}.</p>
 *
 * <p>La correspondance est établie sur le <strong>nom</strong> de la contrainte, pas
 * sur le texte du message : les noms sont fixés par nos migrations et figurent dans
 * le diagramme {@code D2}, tandis que le message dépend de la version et de la
 * langue du serveur PostgreSQL.</p>
 */
@Component
public class TraducteurDeContraintes {

    /** Chaque contrainte d'unicité du schéma, et la règle qu'elle fait respecter. */
    private static final Map<String, CodeErreur> CODES_PAR_CONTRAINTE = Map.of(
            "uq_presences_session_etudiant", CodeErreur.DEJA_PRESENT,           // RG3
            "uq_exercices_session_etudiant", CodeErreur.EXERCICE_DEJA_DEPOSE,   // RG7
            "uq_relectures_exercice", CodeErreur.SESSION_DEJA_CLOTUREE,         // RG15, clôture concurrente
            "uq_relectures_session_relecteur", CodeErreur.SESSION_DEJA_CLOTUREE // RG16, idem
    );

    /**
     * Rend le code du contrat correspondant à la contrainte violée, s'il en existe un.
     *
     * <p>Une contrainte inconnue rend {@link Optional#empty()} : mieux vaut une erreur
     * interne franche qu'un code d'erreur inventé, qui mentirait sur la cause.</p>
     */
    public Optional<CodeErreur> traduire(DataIntegrityViolationException exception) {
        String trace = trace(exception).toLowerCase(Locale.ROOT);
        return CODES_PAR_CONTRAINTE.entrySet().stream()
                .filter(entree -> trace.contains(entree.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Exécute une écriture et convertit une violation de contrainte connue en
     * exception métier.
     *
     * <p>La traduction a lieu ici, au service, et non dans le seul
     * {@code @RestControllerAdvice} : tout appelant doit obtenir la même sémantique,
     * qu'il passe par HTTP ou non. Le gestionnaire d'erreurs reste en filet, pour les
     * violations qui surviennent au moment du commit, hors de portée d'un try.</p>
     *
     * <p>L'écriture doit être <strong>vidée</strong> vers la base à l'intérieur du bloc
     * — {@code saveAndFlush} et non {@code save} — sans quoi la contrainte ne se
     * déclencherait qu'au commit, une fois le bloc quitté.</p>
     */
    public <T> T enTraduisantLesConflits(java.util.function.Supplier<T> ecriture) {
        try {
            return ecriture.get();
        } catch (DataIntegrityViolationException e) {
            throw traduire(e).map(ExceptionMetier::new).orElseThrow(() -> e);
        }
    }

    /** Le nom de la contrainte vit dans le message de la cause racine, pas en surface. */
    private static String trace(Throwable exception) {
        StringBuilder accumulateur = new StringBuilder();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null) {
                accumulateur.append(cause.getMessage()).append('\n');
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return accumulateur.toString();
    }
}
