package cm.kfokam48.presences.exercice;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantRepository;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.presence.domaine.Presence;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
import cm.kfokam48.presences.presence.domaine.SourcePresence;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Conformité de {@code POST /api/exercices} au contrat, sur un PostgreSQL réel. */
@SpringBootTest
@AutoConfigureMockMvc
class ExerciceIT extends TestPostgres {

    private static final String LIEN = "https://github.com/awa/exercice-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private EtudiantRepository etudiants;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;
    @Autowired private ExerciceRepository exercices;

    private Long sessionId;
    private Long presentId;
    private Long absentId;

    @BeforeEach
    void preparer() {
        exercices.deleteAll();
        presences.deleteAll();
        sessions.deleteAll();
        etudiants.deleteAll();
        promotions.deleteAll();

        Promotion promotion = promotions.save(new Promotion("Promotion de test"));
        Etudiant present = etudiants.save(new Etudiant(promotion, "Awa Njoya"));
        Etudiant absent = etudiants.save(new Etudiant(promotion, "Biloa Manga"));
        presentId = present.getId();
        absentId = absent.getId();

        Session session = sessions.save(
                Session.ouvrir(promotion, "Algorithmique", "ABC234", Instant.now()));
        sessionId = session.getId();

        presences.save(Presence.enregistrer(session, present, SourcePresence.ETUDIANT, Instant.now()));
    }

    @Test
    @DisplayName("201 — l'exercice est déposé au statut DEPOSE")
    void devraitDeposerLExercice() throws Exception {
        mockMvc.perform(deposer(sessionId, presentId, LIEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    @DisplayName("400 LIEN_INVALIDE — RG9, le lien n'est pas une adresse web absolue")
    void devraitRefuserUnLienInvalide_RG9() throws Exception {
        mockMvc.perform(deposer(sessionId, presentId, "github.com/awa/tp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    @DisplayName("403 NON_PRESENT — RG8, l'étudiant n'a aucune présence sur la session")
    void devraitRefuserUnEtudiantNonPresent_RG8() throws Exception {
        mockMvc.perform(deposer(sessionId, absentId, LIEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NON_PRESENT"));
    }

    @Test
    @DisplayName("409 EXERCICE_DEJA_DEPOSE — RG7, un exercice par étudiant et par session")
    void devraitRefuserUnSecondDepot_RG7() throws Exception {
        mockMvc.perform(deposer(sessionId, presentId, LIEN)).andExpect(status().isCreated());

        mockMvc.perform(deposer(sessionId, presentId, "https://github.com/awa/autre"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    @DisplayName("RG11 — le dépôt reste possible après l'expiration du code")
    void devraitAccepterUnDepotApresExpirationDuCode_RG11() throws Exception {
        Promotion promotion = promotions.findAll().getFirst();
        Etudiant etudiant = etudiants.findById(presentId).orElseThrow();
        // Session ouverte il y a une heure : son code est mort depuis quarante-cinq
        // minutes, mais elle n'est pas clôturée — Q12 autorise le dépôt tardif.
        Session ancienne = sessions.save(Session.ouvrir(
                promotion, "Séance du matin", "OLD234", Instant.now().minus(Duration.ofHours(1))));
        presences.save(Presence.enregistrer(
                ancienne, etudiant, SourcePresence.ETUDIANT, Instant.now().minus(Duration.ofHours(1))));

        mockMvc.perform(deposer(ancienne.getId(), presentId, LIEN))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE — la session n'existe pas")
    void devraitRefuserUneSessionInconnue() throws Exception {
        mockMvc.perform(deposer(999999L, presentId, LIEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le lien est absent du corps")
    void devraitRefuserUnLienAbsent() throws Exception {
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"sessionId\": %d, \"etudiantId\": %d }".formatted(sessionId, presentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    private RequestBuilder deposer(Long sessionId, Long etudiantId, String lien) {
        return post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"sessionId\": %d, \"etudiantId\": %d, \"lien\": \"%s\" }"
                        .formatted(sessionId, etudiantId, lien));
    }
}
