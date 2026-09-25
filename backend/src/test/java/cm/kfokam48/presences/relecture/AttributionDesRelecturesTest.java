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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    @DisplayName("RG17 — deux présents : un seul relecteur possible, attribution partielle")
    void devraitAttribuerUnSeulRelecteurADeuxPresents_RG17() {
        Etudiant awa = etudiant(1L, "Awa Njoya");
        Etudiant biloa = etudiant(2L, "Biloa Manga");
        Exercice deAwa = exercice(awa);

        var resultat = attribution.attribuer(List.of(deAwa), List.of(awa, biloa), MIDI);

        // Deux relecteurs distincts exigent trois présents. Refuser d'attribuer
        // serait une régression : en v0.1 cette séance produisait une relecture.
        assertThat(resultat.attribuees()).hasSize(1);
        assertThat(resultat.nonAttribuables()).isEmpty();
        assertThat(resultat.attribuees().getFirst().getRelecteur().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("RG15 — trois présents : chaque exercice reçoit deux relecteurs distincts")
    void devraitAttribuerDeuxRelecteursDesTroisPresents_RG15() {
        List<Etudiant> presents = etudiants(3);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.attribuees()).hasSize(6);   // trois exercices, deux relecteurs
        assertThat(resultat.nonAttribuables()).isEmpty();
        for (Exercice exercice : exercices) {
            List<Long> relecteurs = resultat.attribuees().stream()
                    .filter(r -> r.getExercice() == exercice)
                    .map(r -> r.getRelecteur().getId())
                    .toList();
            assertThat(relecteurs).as("deux relecteurs, et deux personnes différentes")
                    .hasSize(2).doesNotHaveDuplicates()
                    .doesNotContain(exercice.getEtudiant().getId());
        }
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
    @DisplayName("RG14, RG15, RG16, RG19 — sur cinquante tirages, les quatre règles tiennent")
    void devraitToujoursRespecterLesQuatreRegles_RG14_RG15_RG16_RG19() {
        List<Etudiant> presents = etudiants(12);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.nonAttribuables()).isEmpty();
        assertThat(resultat.attribuees()).as("douze exercices, deux relecteurs chacun").hasSize(24);

        Map<Long, Long> relecturesParEtudiant = new HashMap<>();
        Map<Exercice, Set<Long>> relecteursParExercice = new HashMap<>();

        for (Relecture relecture : resultat.attribuees()) {
            Long relecteurId = relecture.getRelecteur().getId();
            Exercice exercice = relecture.getExercice();

            assertThat(relecteurId).as("RG19 — jamais son propre exercice")
                    .isNotEqualTo(exercice.getEtudiant().getId());
            assertThat(presents).as("RG14 — le relecteur est présent")
                    .anyMatch(e -> e.getId().equals(relecteurId));

            relecturesParEtudiant.merge(relecteurId, 1L, Long::sum);
            assertThat(relecteursParExercice.computeIfAbsent(exercice, e -> new HashSet<>()).add(relecteurId))
                    .as("RG15 — les deux relecteurs d'un exercice sont deux personnes").isTrue();
        }

        assertThat(relecteursParExercice.values()).as("RG15 — deux relecteurs par exercice")
                .allMatch(r -> r.size() == 2);
        assertThat(relecturesParEtudiant.values()).as("RG16 — au plus deux relectures par étudiant")
                .allMatch(compte -> compte <= 2);
    }

    @ParameterizedTest(name = "{0} présents, tous déposants")
    @ValueSource(ints = {3, 5, 10, 60})
    @DisplayName("RG15, RG16 — deux relecteurs par exercice, quelle que soit la taille")
    void devraitAttribuerDeuxRelecteursQuelleQueSoitLaTaille_RG15(int taille) {
        List<Etudiant> presents = etudiants(taille);
        List<Exercice> exercices = presents.stream().map(this::exercice).toList();

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        assertThat(resultat.attribuees()).hasSize(taille * 2);
        assertThat(resultat.nonAttribuables()).isEmpty();

        Map<Long, Long> compteParRelecteur = new HashMap<>();
        resultat.attribuees().forEach(r -> compteParRelecteur.merge(r.getRelecteur().getId(), 1L, Long::sum));
        assertThat(compteParRelecteur.values())
                .as("chacun relit exactement deux exercices — la charge est équitable")
                .allMatch(compte -> compte == 2);
    }

    @Test
    @DisplayName("RG14 — les présents qui n'ont rien déposé peuvent tout de même relire")
    void devraitPouvoirTirerUnPresentSansExercice_RG14() {
        List<Etudiant> presents = etudiants(5);
        // Deux exercices seulement, sur cinq présents : les trois autres restent
        // éligibles comme relecteurs. Q7 ne réserve pas la relecture aux déposants.
        List<Exercice> exercices = List.of(exercice(presents.get(0)), exercice(presents.get(1)));

        var resultat = attribution.attribuer(exercices, presents, MIDI);

        // Deux exercices, deux relecteurs chacun, pris parmi les cinq présents.
        assertThat(resultat.attribuees()).hasSize(4);
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
