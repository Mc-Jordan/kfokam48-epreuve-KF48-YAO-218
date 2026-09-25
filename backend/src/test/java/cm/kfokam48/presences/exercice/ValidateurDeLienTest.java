package cm.kfokam48.presences.exercice;

import cm.kfokam48.presences.exercice.domaine.ValidateurDeLien;
import cm.kfokam48.presences.partage.erreur.CodeErreur;
import cm.kfokam48.presences.partage.erreur.ExceptionMetier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RG9 — le lien d'un exercice est une adresse absolue de schéma http ou https.
 *
 * <p>Une annotation de validation ne suffirait pas : {@code mailto:} et
 * {@code javascript:} sont des URI parfaitement valides que la règle refuse.</p>
 */
class ValidateurDeLienTest {

    private final ValidateurDeLien validateur = new ValidateurDeLien();

    @ParameterizedTest(name = "accepte {0}")
    @ValueSource(strings = {
            "https://github.com/awa/exercice-1",
            "http://gitlab.com/awa/tp",
            "https://drive.google.com/file/d/abc123/view?usp=sharing",
            "https://exemple.cm:8443/tp/rendu.pdf",
    })
    @DisplayName("RG9 — une adresse http ou https absolue est acceptée")
    void devraitAccepterUneAdresseAbsolue_RG9(String lien) {
        assertThat(validateur.valider(lien)).isEqualTo(lien);
    }

    @ParameterizedTest(name = "refuse {0}")
    @ValueSource(strings = {
            "github.com/awa/exercice",          // pas de schéma
            "/tp/rendu.pdf",                     // chemin relatif
            "ftp://serveur/tp.zip",              // schéma non autorisé
            "mailto:awa@exemple.cm",             // URI valide, mais pas un lien web
            "javascript:alert(1)",               // URI valide, et dangereuse
            "file:///home/awa/tp.pdf",           // pointe vers la machine de l'auteur
            "https://",                          // pas d'hôte
            "http:// espace .com",               // syntaxe invalide
    })
    @DisplayName("RG9 — tout ce qui n'est pas une adresse web absolue est refusé")
    void devraitRefuserToutAutreLien_RG9(String lien) {
        assertThat(assertThrows(ExceptionMetier.class, () -> validateur.valider(lien)).code())
                .isEqualTo(CodeErreur.LIEN_INVALIDE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("RG9 — un lien absent ou vide est refusé comme lien invalide, pas comme champ manquant")
    void devraitRefuserUnLienVide_RG9(String lien) {
        assertThat(assertThrows(ExceptionMetier.class, () -> validateur.valider(lien)).code())
                .isEqualTo(CodeErreur.LIEN_INVALIDE);
    }

    @Test
    @DisplayName("RG9 — un lien démesuré est refusé avant d'atteindre la base")
    void devraitRefuserUnLienDemesure_RG9() {
        String tropLong = "https://exemple.cm/" + "a".repeat(2100);

        assertThat(assertThrows(ExceptionMetier.class, () -> validateur.valider(tropLong)).code())
                .isEqualTo(CodeErreur.LIEN_INVALIDE);
    }

    @Test
    @DisplayName("les espaces de bordure sont retirés")
    void devraitNettoyerLesEspaces() {
        assertThat(validateur.valider("  https://github.com/awa/tp  "))
                .isEqualTo("https://github.com/awa/tp");
    }
}
