package cm.kfokam48.presences.relecture.api.dto;

import cm.kfokam48.presences.partage.erreur.Erreurs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corps de {@code POST /api/relectures/{id}}.
 *
 * <p>La note est reçue en {@link BigDecimal} et non en {@code Integer}, pour une
 * raison précise : Jackson, avec un {@code Integer}, tronque silencieusement
 * {@code 15.5} en {@code 15}. Le contrat impose {@code 400 NOTE_INVALIDE} pour une
 * note « hors 0–20 <strong>ou non entière</strong> » — accepter 15,5 en le
 * transformant en 15 serait une faute invisible, et le test l'a démontré.</p>
 *
 * <p>Les bornes ne sont pas posées en annotation : elles appartiennent à RG18, et
 * une violation doit produire {@code NOTE_INVALIDE}, pas {@code CHAMP_MANQUANT}.</p>
 */
public record RenduRelectureRequete(

        @NotNull(message = "la note est obligatoire")
        BigDecimal note,

        @NotBlank(message = "le commentaire est obligatoire")
        String commentaire
) {

    /** RG18 — une note est un entier compris entre 0 et 20 inclus. */
    public int noteEntiere() {
        if (note.stripTrailingZeros().scale() > 0) {
            throw Erreurs.noteInvalide();
        }
        try {
            return note.intValueExact();
        } catch (ArithmeticException e) {
            throw Erreurs.noteInvalide();
        }
    }
}
