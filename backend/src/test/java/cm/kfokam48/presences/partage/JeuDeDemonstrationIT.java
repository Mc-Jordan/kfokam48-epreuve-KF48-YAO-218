package cm.kfokam48.presences.partage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le jeu de démonstration doit rendre les cas limites <strong>observables</strong>.
 *
 * <p>Trois points du barème portent sur « démarre chez un tiers avec des données de
 * démonstration ». Un jeu qui ne montrerait que le cas nominal laisserait
 * {@code RG17}, {@code RG23} et {@code RG24} invérifiables à l'écran.</p>
 *
 * <p>Ce test vit pour que le jeu ne se dégrade pas en silence : ajouter une
 * migration qui le modifierait sans y penser casserait ici, pas chez le correcteur.</p>
 *
 * <p>Il possède <strong>son propre conteneur</strong>, contrairement aux autres tests
 * d'intégration : ceux-ci vident la base dans leur préparation, et ce qu'on vérifie
 * ici est précisément ce qu'un correcteur voit sur une base vierge. Partager la base
 * rendrait le résultat dépendant de l'ordre d'exécution, donc sans valeur.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class JeuDeDemonstrationIT {

    private static final PostgreSQLContainer POSTGRES_VIERGE =
            new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES_VIERGE.start();
    }

    @DynamicPropertySource
    static void baseVierge(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", POSTGRES_VIERGE::getJdbcUrl);
        registre.add("spring.datasource.username", POSTGRES_VIERGE::getUsername);
        registre.add("spring.datasource.password", POSTGRES_VIERGE::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("le tableau du formateur est peuplé dès le démarrage")
    void devraitPeuplerLeTableau() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    @DisplayName("RG24 — un étudiant sans aucune note montre une moyenne vide, pas un zéro")
    void devraitMontrerUneMoyenneVide_RG24() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                // Hortense ATANGANA : présente, aucun exercice, donc aucune note.
                // Un filtre JSONPath rend une liste : on exige donc une liste
                // contenant null, et non une liste vide.
                .andExpect(jsonPath("$[?(@.nom == 'Hortense ATANGANA')].moyenne")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    @DisplayName("RG24 — une moyenne réelle est visible et calculée par le serveur")
    void devraitMontrerUneMoyenneCalculee_RG24() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                // Awa NJOYA : 16 en séance 1 (un seul relecteur, séance finalisée
                // avant le changement) et moyenne de 15 et 17 en séance 2, soit 16.
                // La moyenne porte sur les notes d'EXERCICE, pas sur les notes brutes.
                .andExpect(jsonPath("$[?(@.nom == 'Awa NJOYA')].moyenne").value(16.00))
                .andExpect(jsonPath("$[?(@.nom == 'Awa NJOYA')].moyenneProvisoire").value(false));
    }

    @Test
    @DisplayName("RG23 — une relecture due est visible dans le tableau")
    void devraitMontrerUneRelectureDue_RG23() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                // Awa doit encore relire l'exercice de Biloa, jamais rendu.
                .andExpect(jsonPath("$[?(@.nom == 'Awa NJOYA')].relecturesEnAttente").value(1));
    }

    @Test
    @DisplayName("RG24, RG26 — une note provisoire est visible et signalée comme telle")
    void devraitMontrerUneNoteProvisoire_RG26() throws Exception {
        // Exercice 7 : un relecteur a rendu 13, l'autre non. La note s'affiche,
        // marquée provisoire — c'est exactement ce que le client a demandé.
        mockMvc.perform(get("/api/exercices/{id}/relecture", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(13.00))
                .andExpect(jsonPath("$.provisoire").value(true))
                .andExpect(jsonPath("$.relecturesRendues").value(1))
                .andExpect(jsonPath("$.relecturesAttendues").value(2))
                .andExpect(jsonPath("$.statut").value("RELU_PARTIEL"));
    }

    @Test
    @DisplayName("RG24 — deux relectures rendues donnent une note définitive, moyenne des deux")
    void devraitMontrerUneNoteDefinitive_RG24() throws Exception {
        // Exercice 6 : 15 et 17 rendus, la note est 16 et elle est définitive.
        mockMvc.perform(get("/api/exercices/{id}/relecture", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(16.00))
                .andExpect(jsonPath("$.provisoire").value(false))
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.commentaires.length()").value(2));
    }

    @Test
    @DisplayName("RG26 — le tableau distingue une moyenne provisoire d'une moyenne acquise")
    void devraitSignalerUneMoyenneProvisoireAuTableau_RG26() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                // Biloa attend encore la seconde relecture de son exercice 7.
                .andExpect(jsonPath("$[?(@.nom == 'Biloa MANGA')].moyenneProvisoire").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'Awa NJOYA')].moyenneProvisoire").value(false));
    }

    @Test
    @DisplayName("RG17 — un exercice NON_ATTRIBUABLE existe, sans quoi la règle serait invisible")
    void devraitMontrerUnExerciceNonAttribuable_RG17() throws Exception {
        mockMvc.perform(get("/api/exercices/{id}/relecture", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_ATTRIBUABLE"))
                .andExpect(jsonPath("$.note").doesNotExist());
    }

    @Test
    @DisplayName("RG4 — une présence ajoutée par le formateur existe, pour Q14")
    void devraitMontrerUnePresenceAjouteeParLeFormateur_RG4() throws Exception {
        mockMvc.perform(get("/api/sessions/{id}/presences", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.source == 'FORMATEUR')].nom").value("Francine MBALLA"));
    }

    @Test
    @DisplayName("les trois états de session sont représentés, le correcteur peut agir")
    void devraitCouvrirLesTroisEtatsDeSession() throws Exception {
        mockMvc.perform(get("/api/sessions").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.statut == 'OUVERTE')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.statut == 'CLOTUREE')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.statut == 'FINALISEE')]").isNotEmpty());
    }

    @Test
    @DisplayName("la séance ouverte a un code encore valable : le correcteur peut marquer une présence")
    void devraitLaisserUneSeanceUtilisable() throws Exception {
        mockMvc.perform(get("/api/promotions/{id}/etudiants", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }
}
