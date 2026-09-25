package cm.kfokam48.presences.exercice.domaine;

import cm.kfokam48.presences.partage.erreur.Erreurs;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Contrôle du lien déposé (RG9).
 *
 * <p>Le contrat impose {@code format: uri}, et RG9 précise : une adresse
 * <strong>absolue</strong> de schéma {@code http} ou {@code https}. Une annotation
 * de validation ne suffirait pas — {@code @URL} accepte des formes que la règle
 * refuse, et {@code mailto:} ou {@code javascript:} sont des URI valides.</p>
 */
@Component
public class ValidateurDeLien {

    private static final Set<String> SCHEMAS_AUTORISES = Set.of("http", "https");
    private static final int LONGUEUR_MAXIMALE = 2000;

    /** Rend le lien nettoyé, ou lève {@code 400 LIEN_INVALIDE}. */
    public String valider(String lien) {
        if (lien == null || lien.isBlank() || lien.length() > LONGUEUR_MAXIMALE) {
            throw Erreurs.lienInvalide();
        }

        URI uri;
        try {
            uri = new URI(lien.trim());
        } catch (URISyntaxException e) {
            throw Erreurs.lienInvalide();
        }

        boolean absolu = uri.isAbsolute() && uri.getScheme() != null;
        boolean schemaValide = absolu && SCHEMAS_AUTORISES.contains(uri.getScheme().toLowerCase());
        boolean hotePresent = uri.getHost() != null && !uri.getHost().isBlank();

        if (!schemaValide || !hotePresent) {
            throw Erreurs.lienInvalide();
        }
        return uri.toString();
    }
}
