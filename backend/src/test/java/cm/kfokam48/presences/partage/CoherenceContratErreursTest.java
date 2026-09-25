package cm.kfokam48.presences.partage;

import cm.kfokam48.presences.partage.erreur.CodeErreur;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le contrat recense les codes d'erreur stables du projet, avec leur statut HTTP.
 * L'énumération {@link CodeErreur} les implémente. Les deux doivent correspondre
 * exactement, sinon l'API renvoie des codes que le contrat ne documente pas — ou
 * documente des codes qu'elle ne renvoie jamais.
 *
 * <p>Vérifie ENF3 et applique RG25.</p>
 */
class CoherenceContratErreursTest {

    private static final Path CONTRAT = Path.of("..", "api", "contrat.yaml");

    /** Une ligne du registre : {@code #  CODE_EXPIRE   410   RG1   POST /api/presences} */
    private static final Pattern LIGNE_REGISTRE =
            Pattern.compile("(?m)^#\\s+([A-Z_]{4,})\\s+(\\d{3})\\s");

    @Test
    @DisplayName("les codes d'erreur du contrat sont exactement ceux de l'énumération")
    void lesCodesDuContratEtDeLEnumerationCorrespondent() throws IOException {
        Map<String, Integer> registreDuContrat = registreDuContrat();
        Map<String, Integer> enumeration = enumeration();

        assertThat(enumeration.keySet())
                .as("codes implémentés, comparés au registre de api/contrat.yaml")
                .containsExactlyInAnyOrderElementsOf(registreDuContrat.keySet());
    }

    @Test
    @DisplayName("chaque code porte le statut HTTP que le contrat lui donne")
    void lesStatutsHttpCorrespondent() throws IOException {
        assertThat(enumeration())
                .as("statut HTTP porté par chaque code, comparé au contrat")
                .containsExactlyInAnyOrderEntriesOf(registreDuContrat());
    }

    private static Map<String, Integer> registreDuContrat() throws IOException {
        Map<String, Integer> registre = new TreeMap<>();
        Matcher m = LIGNE_REGISTRE.matcher(Files.readString(CONTRAT));
        while (m.find()) {
            registre.put(m.group(1), Integer.parseInt(m.group(2)));
        }
        assertThat(registre)
                .as("le registre du contrat doit être lisible — vérifier son format")
                .isNotEmpty();
        return registre;
    }

    private static Map<String, Integer> enumeration() {
        return java.util.Arrays.stream(CodeErreur.values())
                .collect(Collectors.toMap(Enum::name, c -> c.statut().value(), (a, b) -> a, TreeMap::new));
    }
}
