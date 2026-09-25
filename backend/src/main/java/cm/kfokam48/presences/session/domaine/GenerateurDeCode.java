package cm.kfokam48.presences.session.domaine;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Produit le code de présence dicté à la classe.
 *
 * <p>Six caractères tirés d'un alphabet sans {@code I}, {@code O}, {@code 0} ni
 * {@code 1} : un code se lit à voix haute et se saisit sur un téléphone, et ces
 * caractères se confondent. {@link SecureRandom} plutôt que {@code Random} — Q4
 * indique que les étudiants essaieront de deviner les codes des autres.</p>
 */
@Component
public class GenerateurDeCode {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR = 6;

    private final SecureRandom aleatoire = new SecureRandom();

    public String genererCode() {
        StringBuilder code = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            code.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
