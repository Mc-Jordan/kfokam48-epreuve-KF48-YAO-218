package cm.kfokam48.presences.relecture;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantRepository;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.exercice.domaine.StatutExercice;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Le rendu d'une relecture et sa consultation, de bout en bout. */
@SpringBootTest
@AutoConfigureMockMvc
class RelectureIT extends TestPostgres {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private EtudiantRepository etudiants;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Long sessionId;
    private Relecture relecture;

    @BeforeEach
    void preparer() throws Exception {
        relectures.deleteAll();
        exercices.deleteAll();
        presences.deleteAll();
        sessions.deleteAll();
        etudiants.deleteAll();
        promotions.deleteAll();

        Promotion promotion = promotions.save(new Promotion("Promotion de test"));
        Instant maintenant = Instant.now();
        Session session = sessions.save(Session.ouvrir(promotion, "Algorithmique", "ABC234", maintenant));
        sessionId = session.getId();

        // Deux étudiants présents, deux exercices : chacun relit celui de l'autre.
        for (int i = 1; i <= 2; i++) {
            Etudiant etudiant = etudiants.save(new Etudiant(promotion, "Étudiant " + i));
            presences.save(Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
            exercices.save(Exercice.deposer(session, etudiant, "https://exemple.cm/tp/" + i, maintenant));
        }

        mockMvc.perform(post("/api/sessions/{id}/cloture", sessionId)).andExpect(status().isOk());
        relecture = relectures.findBySessionId(sessionId).getFirst();
    }

    @Test
    @DisplayName("200 — la relecture est rendue et l'exercice passe à RELU")
    void devraitRendreLaRelecture() throws Exception {
        mockMvc.perform(rendre(relecture.getId(), 15, "Bon travail, structure claire."))
                .andExpect(status().isOk());

        Relecture rendue = relectures.findById(relecture.getId()).orElseThrow();
        assertThat(rendue.getNote()).isEqualTo((short) 15);
        assertThat(exercices.findById(rendue.getExercice().getId()).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.RELU);
    }

    @Test
    @DisplayName("RG20 — un second envoi avant la finalisation corrige, et renvoie 200")
    void devraitPermettreLaCorrectionAvantFinalisation_RG20() throws Exception {
        mockMvc.perform(rendre(relecture.getId(), 12, "Première lecture.")).andExpect(status().isOk());
        mockMvc.perform(rendre(relecture.getId(), 17, "Relu : meilleur que je croyais."))
                .andExpect(status().isOk());

        assertThat(relectures.findById(relecture.getId()).orElseThrow().getNote())
                .isEqualTo((short) 17);
    }

    @Test
    @DisplayName("409 RELECTURE_DEJA_RENDUE — RG21, après la finalisation plus rien ne bouge")
    void devraitRefuserApresFinalisation_RG21() throws Exception {
        mockMvc.perform(rendre(relecture.getId(), 14, "Correct.")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/{id}/finalisation", sessionId)).andExpect(status().isOk());

        mockMvc.perform(rendre(relecture.getId(), 20, "Je change d'avis."))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));

        assertThat(relectures.findById(relecture.getId()).orElseThrow().getNote())
                .isEqualTo((short) 14);
    }

    @Test
    @DisplayName("400 NOTE_INVALIDE — RG18, la note sort des bornes 0 à 20")
    void devraitRefuserUneNoteHorsBornes_RG18() throws Exception {
        for (int note : new int[]{-1, 21, 100}) {
            mockMvc.perform(rendre(relecture.getId(), note, "Commentaire."))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
        }
    }

    @Test
    @DisplayName("RG18 — les bornes 0 et 20 sont acceptées")
    void devraitAccepterLesBornes_RG18() throws Exception {
        mockMvc.perform(rendre(relecture.getId(), 0, "À reprendre entièrement.")).andExpect(status().isOk());
        mockMvc.perform(rendre(relecture.getId(), 20, "Irréprochable.")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("400 NOTE_INVALIDE — RG18, une note non entière est refusée")
    void devraitRefuserUneNoteNonEntiere_RG18() throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", relecture.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": 15.5, \"commentaire\": \"Entre deux.\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404 RELECTURE_INCONNUE — la relecture n'existe pas")
    void devraitRefuserUneRelectureInconnue() throws Exception {
        mockMvc.perform(rendre(999999L, 15, "Commentaire."))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    @Test
    @DisplayName("EF8 — le relecteur voit ce qu'il doit relire, avec le lien")
    void devraitListerLesRelecturesDuRelecteur() throws Exception {
        mockMvc.perform(get("/api/relectures").param("relecteurId", relecture.getRelecteur().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lien").isNotEmpty())
                .andExpect(jsonPath("$[0].sessionTitre").value("Algorithmique"))
                .andExpect(jsonPath("$[0].statut").value("ATTRIBUEE"));
    }

    @Test
    @DisplayName("RG19 — aucun exercice dont on est l'auteur n'apparaît dans sa propre liste")
    void devraitNeJamaisListerSonPropreExercice_RG19() throws Exception {
        for (Relecture sienne : relectures.findAll()) {
            Long relecteurId = sienne.getRelecteur().getId();
            Long auteurId = exercices.findById(sienne.getExercice().getId())
                    .orElseThrow().getEtudiant().getId();
            assertThat(relecteurId).isNotEqualTo(auteurId);
        }
    }

    @Test
    @DisplayName("404 ETUDIANT_INCONNU — plutôt qu'une liste vide, qui serait trompeuse")
    void devraitRefuserUnRelecteurInconnu() throws Exception {
        mockMvc.perform(get("/api/relectures").param("relecteurId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    @DisplayName("RG22 — l'auteur voit sa note, jamais l'identité de son relecteur")
    void devraitMasquerLIdentiteDuRelecteur_RG22() throws Exception {
        mockMvc.perform(rendre(relecture.getId(), 16, "Bien construit.")).andExpect(status().isOk());

        String corps = mockMvc.perform(
                        get("/api/exercices/{id}/relecture", relecture.getExercice().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(16))
                .andExpect(jsonPath("$.commentaire").value("Bien construit."))
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.relecteurId").doesNotExist())
                .andExpect(jsonPath("$.relecteur").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Le nom du relecteur ne doit apparaître sous aucune forme dans la réponse.
        List<Etudiant> tous = etudiants.findAll();
        Long relecteurId = relecture.getRelecteur().getId();
        String nomDuRelecteur = tous.stream()
                .filter(e -> e.getId().equals(relecteurId)).findFirst().orElseThrow().getNom();
        assertThat(corps).doesNotContain(nomDuRelecteur);
    }

    @Test
    @DisplayName("RG23 — un exercice en attente se consulte sans note, et non par une erreur")
    void devraitRendreLAttenteVisible_RG23() throws Exception {
        mockMvc.perform(get("/api/exercices/{id}/relecture", relecture.getExercice().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
                .andExpect(jsonPath("$.note").doesNotExist());
    }

    @Test
    @DisplayName("404 EXERCICE_INCONNU — consulter la relecture d'un exercice qui n'existe pas")
    void devraitRefuserUnExerciceInconnu() throws Exception {
        mockMvc.perform(get("/api/exercices/{id}/relecture", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

    private RequestBuilder rendre(Long relectureId, int note, String commentaire) {
        return post("/api/relectures/{id}", relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"note\": %d, \"commentaire\": \"%s\" }".formatted(note, commentaire));
    }
}
