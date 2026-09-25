package cm.kfokam48.presences.partage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le barème note la correspondance entre le diagramme D2 et les migrations.
 * Cette vérification est automatisée plutôt que confiée à la relecture : l'étape 3
 * modifiera le schéma, et un écart introduit à ce moment-là passerait inaperçu.
 *
 * <p>Les clés étrangères simples sont exclues de la comparaison : elles suivent les
 * relations du diagramme, qui les montre sans les nommer.</p>
 */
class CoherenceD2MigrationTest {

    private static final Path DIAGRAMME = Path.of("..", "docs", "diagrammes", "D2-modele-donnees.md");
    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    private static final Pattern NOM_DANS_D2 = Pattern.compile("`((?:uq|ck|idx|fk)_[a-z_]+)`");
    private static final Pattern NOM_DANS_SQL = Pattern.compile("(?:CONSTRAINT|INDEX)\\s+([a-z_]+)");
    private static final Pattern NOM_SUPPRIME =
            Pattern.compile("DROP\\s+(?:CONSTRAINT|INDEX)\\s+(?:IF EXISTS\\s+)?([a-z_]+)");
    private static final Pattern TABLE_DANS_D2 = Pattern.compile("(?m)^\\s{4}([A-Z_]+) \\{");
    private static final Pattern TABLE_DANS_SQL = Pattern.compile("CREATE TABLE ([a-z_]+)");

    @Test
    @DisplayName("les tables de D2 sont exactement celles que créent les migrations")
    void lesTablesDuDiagrammeEtDesMigrationsCorrespondent() throws IOException {
        Set<String> dansLeDiagramme = extraire(TABLE_DANS_D2, lireDiagramme()).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(extraire(TABLE_DANS_SQL, lireMigrations()))
                .as("tables créées par les migrations, comparées à celles de D2")
                .containsExactlyInAnyOrderElementsOf(dansLeDiagramme);
    }

    @Test
    @DisplayName("toute contrainte nommée dans D2 existe dans les migrations, et réciproquement")
    void lesContraintesDuDiagrammeEtDesMigrationsCorrespondent() throws IOException {
        Set<String> dansLeDiagramme = extraire(NOM_DANS_D2, lireDiagramme());
        Set<String> dansLesMigrations = contraintesEffectives(lireMigrations());

        Set<String> clesEtrangeresSimples = dansLesMigrations.stream()
                .filter(nom -> nom.startsWith("fk_"))
                .filter(nom -> !dansLeDiagramme.contains(nom))
                .collect(Collectors.toSet());

        assertThat(dansLesMigrations)
                .as("contraintes promises par D2 mais absentes des migrations")
                .containsAll(dansLeDiagramme);

        assertThat(dansLesMigrations)
                .as("contraintes présentes dans les migrations mais absentes de D2")
                .isSubsetOf(union(dansLeDiagramme, clesEtrangeresSimples));
    }

    /**
     * Les contraintes que le schéma porte <strong>réellement</strong> : celles que les
     * migrations créent, moins celles qu'elles suppriment.
     *
     * <p>Comparer à l'union de toutes les instructions serait faux dès la première
     * migration qui retire une contrainte — l'étape 3 en retire deux. Une contrainte
     * supprimée n'a plus à figurer dans {@code D2}, et l'y exiger reviendrait à figer
     * le diagramme sur un schéma qui n'existe plus.</p>
     *
     * <p>Limite connue : une contrainte supprimée puis recréée sous le même nom serait
     * considérée comme absente. Le cas ne se présente pas, et le jour où il se
     * présentera, ce commentaire dira quoi corriger.</p>
     */
    private static Set<String> contraintesEffectives(String migrations) {
        Set<String> creees = extraire(NOM_DANS_SQL, migrations);
        creees.removeAll(extraire(NOM_SUPPRIME, migrations));
        return creees;
    }

    private static String lireDiagramme() throws IOException {
        return Files.readString(DIAGRAMME);
    }

    private static String lireMigrations() throws IOException {
        try (var fichiers = Files.list(MIGRATIONS)) {
            return fichiers.filter(f -> f.toString().endsWith(".sql"))
                    .sorted()
                    .map(CoherenceD2MigrationTest::lire)
                    .collect(Collectors.joining("\n"));
        }
    }

    private static String lire(Path fichier) {
        try {
            return Files.readString(fichier);
        } catch (IOException e) {
            throw new IllegalStateException("migration illisible : " + fichier, e);
        }
    }

    private static Set<String> extraire(Pattern motif, String texte) {
        Set<String> trouves = new TreeSet<>();
        Matcher m = motif.matcher(texte);
        while (m.find()) {
            trouves.add(m.group(1));
        }
        return trouves;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> resultat = new TreeSet<>(a);
        resultat.addAll(b);
        return resultat;
    }
}
