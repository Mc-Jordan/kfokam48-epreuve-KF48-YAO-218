package cm.kfokam48.presences.relecture;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.relecture.domaine.AttributionDesRelectures;
import cm.kfokam48.presences.relecture.domaine.Relecture;
import cm.kfokam48.presences.session.domaine.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le tirage au sort des relecteurs — la règle métier la plus riche du projet, et
 * celle que le barème attend en test unitaire (B6).
 *
 * <p>Les cas limites sont éprouvés nommément : un seul présent, deux présents, et
 * la répétition, parce qu'un algorithme aléatoire qui passe une fois ne prouve rien.</p>
 */
class AttributionDesRelecturesTest {

    private static final Instant MIDI = Instant.parse("2026-09-25T12:00:00Z");

    private final AttributionDesRelectures attribution = new AttributionDesRelectures();
    private final Promotion promotion = new Promotion("Promotion 2026");
    private final Session session = Session.ouvrir(promotion, "Algorithmique", "ABC234", MIDI);

    @Test
    @DisplayName("RG17 — un seul étudiant présent : son exercice est NON_ATTRIBUABLE")
    void devraitMarquerNonAttribuableAvecUnSeulPresent_RG17() {
        Etudiant seul = etudiant(1L, "Awa Njoya");
        Exercice exercice = exercice(seul);

        var resultat = attribution.attribuer(List.of(exercice), List.of(seul), MIDI);

        assertThat(resultat.attribuees()).isEmpty();
        assertThat(resultat.nonAttribuables()).containsExactly(exercice);
    }

    @Test
    @DisplayName("RG19 — deux présents : chacun relit l'autre, jamais lui-même")
    void devraitCroiserLesDeuxSeulsPresents_RG19() {
        Etudiant awa = etudiant(1L, "Awa Njoya");
        Etudiant biloa = etudiant(2L, "Biloa Manga");
        Exercice deAwa = exercice(awa);
        Exercice deBiloa = exercice(biloa);

        var resultat = attribution.attribuer(List.of(deAwa, deBiloa), List.of(awa, biloa), MIDI);

        assertThat(resultat.nonAttribuables()).isEmpty();
        assertThat(resultat.attribuees()).hasSize(2);
        for (Relecture relecture : resultat.attribuees()) {
            assertThat(relecture.getRelecteur().getId())
                    .isNotEqualTo(relecture.getExercice().getEtudiant().getId());
        }
    }

    @RepeatedTest(value = 50, name = "tirage {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG14, RG16, RG19 — sur cinquante tirages, aucune auto-relecture ni doublon")
    void devraitToujoursRespecterLesTroisRegles_RG14_RG16_RG19() {
        List<Etudiant> presents = etudiants(12);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.nonAttribuables()).isEmpty();
        assertThat(resultat.attribuees()).hasSize(12);

        Set<Long> relecteursUtilises = new HashSet<>();
        for (Relecture relecture : resultat.attribuees()) {
            Long relecteurId = relecture.getRelecteur().getId();
            Long auteurId = relecture.getExercice().getEtudiant().getId();

            assertThat(relecteurId).as("RG19 — jamais son propre exercice").isNotEqualTo(auteurId);
            assertThat(relecteursUtilises.add(relecteurId))
                    .as("RG16 — au plus une relecture par étudiant").isTrue();
            assertThat(presents).as("RG14 — le relecteur est présent")
                    .anyMatch(e -> e.getId().equals(relecteurId));
        }
    }

    @ParameterizedTest(name = "{0} présents, tous déposants")
    @ValueSource(ints = {2, 3, 5, 10, 60})
    @DisplayName("RG16 — chaque exercice trouve un relecteur distinct, quelle que soit la taille")
    void devraitAttribuerTousLesExercices_RG16(int taille) {
        List<Etudiant> presents = etudiants(taille);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.attribuees()).hasSize(taille);
        assertThat(resultat.nonAttribuables()).isEmpty();
        assertThat(resultat.attribuees().stream().map(r -> r.getRelecteur().getId()).distinct())
                .hasSize(taille);
    }

    @Test
    @DisplayName("RG14 — les présents qui n'ont rien déposé peuvent tout de même relire")
    void devraitPouvoirTirerUnPresentSansExercice_RG14() {
        List<Etudiant> presents = etudiants(5);
        // Deux exercices seulement, sur cinq présents : les trois autres restent
        // éligibles comme relecteurs. Q7 ne réserve pas la relecture aux déposants.
        List<Exercice> exercices = List.of(exercice(presents.get(0)), exercice(presents.get(1)));

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.attribuees()).hasSize(2);
        assertThat(resultat.nonAttribuables()).isEmpty();
    }

    @Test
    @DisplayName("aucun exercice déposé : la clôture n'attribue rien et ne se plaint pas")
    void devraitNeRienFaireSansExercice() {
        var resultat = attribution.attribuer(List.of(), etudiants(5), MIDI);

        assertThat(resultat.attribuees()).isEmpty();
        assertThat(resultat.nonAttribuables()).isEmpty();
    }

    @Test
    @DisplayName("RG8 démentie par la donnée : l'exercice est signalé, pas attribué au hasard")
    void devraitSignalerUnAuteurAbsentPlutotQueDeviner() {
        List<Etudiant> presents = etudiants(3);
        Etudiant intrus = etudiant(99L, "Auteur absent");

        var resultat = attribution.attribuer(List.of(exercice(intrus)), presents, MIDI);

        assertThat(resultat.attribuees()).isEmpty();
        assertThat(resultat.nonAttribuables()).hasSize(1);
    }

    @RepeatedTest(value = 20, name = "tirage {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG14 — le tirage varie réellement d'une clôture à l'autre")
    void devraitVarierDUnTirageALAutre_RG14() {
        List<Etudiant> presents = etudiants(8);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        Set<List<Long>> combinaisons = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            combinaisons.add(attribution.attribuer(exercices, presents, MIDI).attribuees().stream()
                    .map(r -> r.getRelecteur().getId())
                    .toList());
        }
        // Un tirage figé produirait une seule combinaison sur vingt essais.
        assertThat(combinaisons).as("le tirage doit être aléatoire").hasSizeGreaterThan(1);
    }

    // --- fabriques ----------------------------------------------------------

    private List<Etudiant> etudiants(int combien) {
        List<Etudiant> liste = new ArrayList<>();
        for (long i = 1; i <= combien; i++) {
            liste.add(etudiant(i, "Étudiant " + i));
        }
        return liste;
    }

    private Etudiant etudiant(Long id, String nom) {
        Etudiant etudiant = new Etudiant(promotion, nom);
        ReflectionTestUtils.setField(etudiant, "id", id);
        return etudiant;
    }

    private Exercice exercice(Etudiant auteur) {
        Exercice exercice = Exercice.deposer(session, auteur, "https://exemple.cm/tp", MIDI);
        ReflectionTestUtils.setField(exercice, "id", auteur.getId());
        return exercice;
    }
}
