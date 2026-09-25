package cm.kfokam48.presences.partage.erreur;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Gestion centralisée des erreurs (B4).
 *
 * <p>Applique RG25 : <strong>toute</strong> réponse d'erreur sort au format
 * {@code { code, message }}, y compris les erreurs de routage, de désérialisation
 * et les exceptions imprévues. Aucune trace d'exécution ne part vers le client :
 * elle est journalisée côté serveur et remplacée par un message neutre.</p>
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger journal = LoggerFactory.getLogger(GestionnaireErreurs.class);

    private final TraducteurDeContraintes traducteur;

    public GestionnaireErreurs(TraducteurDeContraintes traducteur) {
        this.traducteur = traducteur;
    }

    /** Les erreurs métier : le code et le statut viennent de l'exception elle-même. */
    @ExceptionHandler(ExceptionMetier.class)
    public ResponseEntity<ReponseErreur> metier(ExceptionMetier e) {
        return ResponseEntity.status(e.code().statut())
                .body(ReponseErreur.de(e.code(), e.getMessage()));
    }

    /** Corps de requête invalide : @Valid sur un DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReponseErreur> corpsInvalide(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return reponse(CodeErreur.CHAMP_MANQUANT, detail.isBlank() ? null : detail);
    }

    /** Validation portée par les paramètres d'une méthode de contrôleur. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ReponseErreur> parametreInvalide(HandlerMethodValidationException e) {
        return reponse(CodeErreur.CHAMP_MANQUANT, null);
    }

    /**
     * Paramètre obligatoire absent, type incompatible, ou corps illisible.
     *
     * <p>Un cas mérite un traitement à part : une note non entière. Le contrat
     * impose {@code NOTE_INVALIDE} pour « note hors 0–20 <em>ou non entière</em> »,
     * et répondre {@code CHAMP_MANQUANT} mentirait sur la nature de l'erreur.
     * Le champ fautif est donc lu dans le chemin de désérialisation.</p>
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
                       MethodArgumentTypeMismatchException.class,
                       HttpMessageNotReadableException.class})
    public ResponseEntity<ReponseErreur> requeteIllisible(Exception e) {
        return reponse(champFautif(e).filter("note"::equals).isPresent()
                ? CodeErreur.NOTE_INVALIDE
                : CodeErreur.CHAMP_MANQUANT, null);
    }

    /** Le nom du champ visé par une erreur de désérialisation, quand Jackson le donne. */
    private static java.util.Optional<String> champFautif(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mismatch) {
                return mismatch.getPath().stream()
                        .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                        .filter(java.util.Objects::nonNull)
                        .reduce((premier, dernier) -> dernier);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Route inexistante. Sans cette prise en charge, Spring renverrait sa propre
     * page d'erreur, ce que le contrat interdit explicitement.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ReponseErreur> routeInconnue(Exception e) {
        return reponse(CodeErreur.RESSOURCE_INCONNUE, null);
    }

    /** Verbe HTTP non supporté sur une route existante. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ReponseErreur> verbeNonSupporte(HttpRequestMethodNotSupportedException e) {
        return reponse(CodeErreur.METHODE_NON_AUTORISEE,
                "Cette opération n'accepte pas le verbe " + e.getMethod() + ".");
    }

    /**
     * Violation d'une contrainte d'unicité.
     *
     * <p>Sous concurrence, deux requêtes peuvent franchir le même contrôle applicatif
     * avant que l'une n'écrive : c'est alors la base qui tranche. Son verdict est
     * traduit dans le langage du contrat — sans quoi le client reçoit
     * {@code 500 ERREUR_INTERNE} là où la règle dit {@code 409}.</p>
     *
     * <p>Une contrainte que nous ne connaissons pas retombe sur l'erreur interne :
     * mieux vaut l'avouer que d'inventer un code qui mentirait sur la cause.</p>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ReponseErreur> contrainteViolee(DataIntegrityViolationException e,
                                                          HttpServletRequest requete) {
        return traducteur.traduire(e)
                .map(code -> reponse(code, null))
                .orElseGet(() -> {
                    journal.error("Contrainte violée sans correspondance sur {} {}",
                            requete.getMethod(), requete.getRequestURI(), e);
                    return reponse(CodeErreur.ERREUR_INTERNE, null);
                });
    }

    /**
     * Filet de sécurité. Une exception imprévue est journalisée intégralement côté
     * serveur, et le client reçoit le format imposé — jamais la trace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> imprevue(Exception e, HttpServletRequest requete) {
        journal.error("Erreur non gérée sur {} {}", requete.getMethod(), requete.getRequestURI(), e);
        return reponse(CodeErreur.ERREUR_INTERNE, null);
    }

    private static ResponseEntity<ReponseErreur> reponse(CodeErreur code, String message) {
        return ResponseEntity.status(code.statut())
                .body(message == null ? ReponseErreur.de(code) : ReponseErreur.de(code, message));
    }
}
