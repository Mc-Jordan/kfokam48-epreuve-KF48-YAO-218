package cm.kfokam48.presences.session;

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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La clôture et la finalisation, de bout en bout sur un PostgreSQL réel.
 *
 * <p>C'est le test qui vérifie que les contraintes du schéma tiennent réellement :
 * {@code uq_relectures_session_relecteur} interdit qu'un étudiant reçoive deux
 * relectures, et la clé étrangère composite interdit qu'une relecture pointe vers
 * une session différente de celle de son exercice.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClotureIT extends TestPostgres {

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
    @DisplayName("RG15 — la clôture attribue deux relecteurs à chaque exercice")
    void devraitAttribuerDeuxRelecteursParExercice_RG15() throws Exception {
        Session session = seance("Algorithmique", 5, 5);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                // Le contrat compte des EXERCICES, pas des relectures : cinq et non dix.
                .andExpect(jsonPath("$.exercicesAttribues").value(5))
                .andExpect(jsonPath("$.exercicesNonAttribuables").value(0));

        assertThat(exercices.findBySessionId(session.getId()))
                .allMatch(e -> e.getStatut() == StatutExercice.EN_ATTENTE_RELECTURE);
        assertThat(relectures.findBySessionId(session.getId()))
                .as("cinq exercices, deux relecteurs chacun").hasSize(10);
    }

    @Test
    @DisplayName("RG17 — deux présents : un seul relecteur, l'exercice est tout de même attribué")
    void devraitAttribuerPartiellementADeuxPresents_RG17() throws Exception {
        Session session = seance("Séance à deux", 2, 2);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercicesAttribues").value(2))
                .andExpect(jsonPath("$.exercicesNonAttribuables").value(0));

        // Un relecteur par exercice, faute de troisième présent : la note restera
        // provisoire, mais refuser d'attribuer serait une régression sur v0.1.
        assertThat(relectures.findBySessionId(session.getId())).hasSize(2);
    }

    @Test
    @DisplayName("RG17 — un seul présent : son exercice devient NON_ATTRIBUABLE")
    void devraitMarquerNonAttribuable_RG17() throws Exception {
        Session session = seance("Séance déserte", 1, 1);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercicesAttribues").value(0))
                .andExpect(jsonPath("$.exercicesNonAttribuables").value(1));

        assertThat(exercices.findBySessionId(session.getId()))
                .allMatch(e -> e.getStatut() == StatutExercice.NON_ATTRIBUABLE);
    }

    @Test
    @DisplayName("RG16 — chaque étudiant reçoit exactement deux relectures, jamais trois")
    void devraitAttribuerAuPlusDeuxRelecturesParEtudiant_RG16() throws Exception {
        Session session = seance("Algorithmique", 8, 8);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk());

        java.util.Map<Long, Long> parRelecteur = new java.util.HashMap<>();
        relectures.findBySessionId(session.getId())
                .forEach(r -> parRelecteur.merge(r.getRelecteur().getId(), 1L, Long::sum));

        assertThat(parRelecteur).as("les huit présents relisent").hasSize(8);
        assertThat(parRelecteur.values()).as("deux relectures chacun, la charge est équitable")
                .allMatch(compte -> compte == 2);
    }

    @Test
    @DisplayName("RG19 — personne ne relit son propre exercice")
    void devraitNeJamaisFaireRelireSonPropreExercice_RG19() throws Exception {
        Session session = seance("Algorithmique", 6, 6);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk());

        // L'exercice est chargé paresseusement : on relit son auteur par une requête
        // plutôt que de traverser le proxy hors transaction.
        assertThat(relectures.findBySessionId(session.getId()))
                .allSatisfy(relecture -> {
                    Exercice relu = exercices.findById(relecture.getExercice().getId()).orElseThrow();
                    assertThat(relecture.getRelecteur().getId())
                            .isNotEqualTo(relu.getEtudiant().getId());
                });
    }

    @Test
    @DisplayName("RG13 — la clôture ferme les dépôts")
    void devraitFermerLesDepots_RG13() throws Exception {
        Session session = seance("Algorithmique", 3, 2);
        Etudiant sansExercice = etudiants.findAll().stream()
                .filter(e -> exercices.findBySessionId(session.getId()).stream()
                        .noneMatch(x -> x.getEtudiant().getId().equals(e.getId())))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"sessionId\": %d, \"etudiantId\": %d, \"lien\": \"https://exemple.cm/tp\" }"
                                .formatted(session.getId(), sansExercice.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_FERMEE"));
    }

    @Test
    @DisplayName("409 SESSION_DEJA_CLOTUREE — RG12, la clôture n'a lieu qu'une fois")
    void devraitRefuserUneSecondeCloture_RG12() throws Exception {
        Session session = seance("Algorithmique", 3, 3);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_CLOTUREE"));
    }

    @Test
    @DisplayName("409 SESSION_NON_CLOTUREE — RG12, on ne finalise que ce qui est clôturé")
    void devraitRefuserDeFinaliserUneSessionOuverte_RG12() throws Exception {
        Session session = seance("Algorithmique", 3, 3);

        mockMvc.perform(post("/api/sessions/{id}/finalisation", session.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_NON_CLOTUREE"));
    }

    @Test
    @DisplayName("409 SESSION_DEJA_FINALISEE — RG12, la finalisation n'a lieu qu'une fois")
    void devraitRefuserUneSecondeFinalisation_RG12() throws Exception {
        Session session = seance("Algorithmique", 3, 3);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId())).andExpect(status().isOk());
        mockMvc.perform(post("/api/sessions/{id}/finalisation", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FINALISEE"));
        mockMvc.perform(post("/api/sessions/{id}/finalisation", session.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_FINALISEE"));
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE — clôturer une session qui n'existe pas")
    void devraitRefuserUneSessionInconnue() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/cloture", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("une séance sans aucun exercice se clôture sans se plaindre")
    void devraitCloturerUneSeanceSansExercice() throws Exception {
        Session session = seance("Séance théorique", 4, 0);

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercicesAttribues").value(0))
                .andExpect(jsonPath("$.exercicesNonAttribuables").value(0));
    }

    /** Crée une séance avec {@code presents} étudiants présents, dont {@code deposants} ont déposé. */
    private Session seance(String titre, int presents, int deposants) {
        Instant maintenant = Instant.now();
        Session session = sessions.save(Session.ouvrir(
                promotion, titre, "C" + System.nanoTime() % 100000, maintenant));

        List<Etudiant> inscrits = new ArrayList<>();
        for (int i = 1; i <= presents; i++) {
            Etudiant etudiant = etudiants.save(new Etudiant(promotion, titre + " étudiant " + i));
            inscrits.add(etudiant);
            presences.save(Presence.enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant));
        }
        for (int i = 0; i < deposants; i++) {
            exercices.save(Exercice.deposer(
                    session, inscrits.get(i), "https://exemple.cm/tp/" + i, maintenant));
        }
        return session;
    }
}
