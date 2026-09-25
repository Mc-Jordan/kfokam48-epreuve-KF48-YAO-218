package cm.kfokam48.presences.tableau;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantRepository;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.presence.domaine.Presence;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
import cm.kfokam48.presences.presence.domaine.SourcePresence;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
import cm.kfokam48.presences.relecture.domaine.Relecture;
import cm.kfokam48.presences.relecture.domaine.RelectureRepository;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le tableau récapitulatif du formateur, sur un PostgreSQL réel. */
@SpringBootTest
@AutoConfigureMockMvc
class TableauIT extends TestPostgres {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private EtudiantRepository etudiants;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Promotion promotion;

    @BeforeEach
    void preparer() {
        relectures.deleteAll();
        exercices.deleteAll();
        presences.deleteAll();
        sessions.deleteAll();
        etudiants.deleteAll();
        promotions.deleteAll();
        promotion = promotions.save(new Promotion("Promotion de test"));
    }

    @Test
    @DisplayName("200 — les six champs du contrat sont présents pour chaque étudiant")
    void devraitRenvoyerLesSixChamps() throws Exception {
        etudiants.save(new Etudiant(promotion, "Awa Njoya"));

        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].etudiantId").isNumber())
                .andExpect(jsonPath("$[0].nom").value("Awa Njoya"))
                .andExpect(jsonPath("$[0].presences").value(0))
                .andExpect(jsonPath("$[0].exercicesDeposes").value(0))
                .andExpect(jsonPath("$[0].relecturesEnAttente").value(0));
    }

    @Test
    @DisplayName("RG24 — sans aucune note reçue, la moyenne est vide et non zéro")
    void devraitRendreUneMoyenneVideSansNote_RG24() throws Exception {
        etudiants.save(new Etudiant(promotion, "Awa Njoya"));

        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                // Une moyenne de zéro se lirait comme un zéro pointé. Le contrat
                // prévoit nullable : le champ doit être absent, pas valoir 0.
                .andExpect(jsonPath("$[0].moyenne").doesNotExist());
    }

    @Test
    @DisplayName("RG24 — la moyenne ne porte que sur les relectures rendues")
    void devraitMoyennerLesSeulesRelecturesRendues_RG24() throws Exception {
        Instant maintenant = Instant.now();
        Etudiant auteur = etudiants.save(new Etudiant(promotion, "Awa Njoya"));
        Etudiant relecteur = etudiants.save(new Etudiant(promotion, "Biloa Manga"));

        // Trois exercices : notés 12 et 18, plus un troisième jamais relu.
        noter(auteur, relecteur, "Séance 1", (short) 12, maintenant);
        noter(auteur, relecteur, "Séance 2", (short) 18, maintenant);
        noter(auteur, relecteur, "Séance 3", null, maintenant);

        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                // Moyenne de 12 et 18, pas de 12, 18 et 0.
                .andExpect(jsonPath("$[0].nom").value("Awa Njoya"))
                .andExpect(jsonPath("$[0].moyenne").value(15.00))
                .andExpect(jsonPath("$[0].exercicesDeposes").value(3));
    }

    @Test
    @DisplayName("RG23 — relecturesEnAttente compte ce que l'étudiant doit rendre, au sens de Q16")
    void devraitCompterLesRelecturesDues_RG23() throws Exception {
        Instant maintenant = Instant.now();
        Etudiant auteur = etudiants.save(new Etudiant(promotion, "Awa Njoya"));
        Etudiant relecteur = etudiants.save(new Etudiant(promotion, "Biloa Manga"));

        noter(auteur, relecteur, "Séance 1", null, maintenant);
        noter(auteur, relecteur, "Séance 2", null, maintenant);

        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                // Biloa doit deux relectures ; Awa attend d'être relue, ce qui
                // n'est pas la même chose et ne doit pas se compter ici.
                .andExpect(jsonPath("$[0].nom").value("Awa Njoya"))
                .andExpect(jsonPath("$[0].relecturesEnAttente").value(0))
                .andExpect(jsonPath("$[1].nom").value("Biloa Manga"))
                .andExpect(jsonPath("$[1].relecturesEnAttente").value(2));
    }

    @Test
    @DisplayName("les compteurs ne se multiplient pas entre eux : pas de produit cartésien")
    void devraitNePasMultiplierLesCompteurs() throws Exception {
        Instant maintenant = Instant.now();
        Etudiant etudiant = etudiants.save(new Etudiant(promotion, "Awa Njoya"));

        // Trois présences et deux exercices. Des jointures cumulées donneraient
        // six présences et six exercices : l'erreur classique de ce tableau.
        for (int i = 1; i <= 3; i++) {
            Session session = sessions.save(Session.ouvrir(promotion, "Séance " + i, "COD" + i + "23", maintenant));
            presences.save(Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
            if (i <= 2) {
                exercices.save(Exercice.deposer(session, etudiant, "https://exemple.cm/tp/" + i, maintenant));
            }
        }

        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presences").value(3))
                .andExpect(jsonPath("$[0].exercicesDeposes").value(2));
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE — la promotion n'existe pas")
    void devraitRefuserUnePromotionInconnue() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le paramètre promotionId est absent")
    void devraitRefuserUnParametreAbsent() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("ENF2 — soixante étudiants et trente séances : réponse en moins de deux secondes")
    void devraitRepondreEnMoinsDeDeuxSecondes_ENF2() throws Exception {
        Instant maintenant = Instant.now();
        List<Etudiant> promo = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            promo.add(etudiants.save(new Etudiant(promotion, "Étudiant %02d".formatted(i))));
        }
        for (int s = 1; s <= 30; s++) {
            Session session = sessions.save(Session.ouvrir(
                    promotion, "Séance " + s, "S%04d".formatted(s),
                    maintenant.minus(Duration.ofDays(30 - s))));
            for (Etudiant etudiant : promo) {
                presences.save(Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
                exercices.save(Exercice.deposer(session, etudiant, "https://exemple.cm/tp", maintenant));
            }
        }

        long depart = System.nanoTime();
        mockMvc.perform(get("/api/tableau").param("promotionId", promotion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(60));
        Duration duree = Duration.ofNanos(System.nanoTime() - depart);

        assertThat(duree).as("ENF2 — le tableau doit répondre en moins de deux secondes")
                .isLessThan(Duration.ofSeconds(2));
    }

    /** Crée une séance, une présence, un exercice, et sa relecture — notée ou non. */
    private void noter(Etudiant auteur, Etudiant relecteur, String titre, Short note, Instant maintenant) {
        Session session = sessions.save(Session.ouvrir(
                promotion, titre, "C" + System.nanoTime() % 100000, maintenant));
        presences.save(Presence.enregistrer(session, auteur, SourcePresence.ETUDIANT, maintenant));
        Exercice exercice = exercices.save(
                Exercice.deposer(session, auteur, "https://exemple.cm/tp", maintenant));
        Relecture relecture = Relecture.attribuer(exercice, relecteur, maintenant);
        if (note != null) {
            relecture.rendre(note, "Commentaire.", maintenant);
        }
        relectures.save(relecture);
    }
}
