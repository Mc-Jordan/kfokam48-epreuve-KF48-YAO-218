package cm.kfokam48.presences.presence;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantRepository;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
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

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Conformité de {@code POST /api/presences} au contrat, sur un PostgreSQL réel.
 * Les quatre codes de statut y sont couverts, corps d'erreur compris.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresenceIT extends TestPostgres {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private EtudiantRepository etudiants;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;

    private Long promotionId;
    private Long awaId;
    private Long biloaId;
    private String code;

    @BeforeEach
    void preparer() {
        presences.deleteAll();
        sessions.deleteAll();
        etudiants.deleteAll();
        promotions.deleteAll();

        Promotion promotion = promotions.save(new Promotion("Promotion de test"));
        promotionId = promotion.getId();
        awaId = etudiants.save(new Etudiant(promotion, "Awa Njoya")).getId();
        biloaId = etudiants.save(new Etudiant(promotion, "Biloa Manga")).getId();

        code = "ABC234";
        sessions.save(Session.ouvrir(promotion, "Algorithmique", code, Instant.now()));
    }

    @Test
    @DisplayName("201 — la présence est enregistrée avec la source ETUDIANT")
    void devraitEnregistrerLaPresence() throws Exception {
        mockMvc.perform(marquer(code, awaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(awaId))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    @DisplayName("400 CODE_INCONNU — le code ne désigne aucune session")
    void devraitRefuserUnCodeInconnu() throws Exception {
        mockMvc.perform(marquer("ZZZZZZ", awaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("409 DEJA_PRESENT — RG3, une seconde présence sur la même session")
    void devraitRefuserUneSecondePresence_RG3() throws Exception {
        mockMvc.perform(marquer(code, awaId)).andExpect(status().isCreated());

        mockMvc.perform(marquer(code, awaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    @DisplayName("410 CODE_EXPIRE — RG1, le code a passé sa quinzième minute")
    void devraitRefuserUnCodeExpire_RG1() throws Exception {
        Promotion promotion = promotions.findById(promotionId).orElseThrow();
        // Session ouverte il y a vingt minutes : son code est mort depuis cinq.
        Session perimee = sessions.save(Session.ouvrir(
                promotion, "Séance d'hier", "OLD234", Instant.now().minus(Duration.ofMinutes(20))));

        mockMvc.perform(marquer(perimee.getCode(), awaId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    @DisplayName("RG3 — l'unicité vaut par étudiant, pas par session : un pair peut encore se déclarer")
    void devraitLaisserUnAutreEtudiantSeDeclarer_RG3() throws Exception {
        mockMvc.perform(marquer(code, awaId)).andExpect(status().isCreated());
        mockMvc.perform(marquer(code, biloaId)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le code est absent du corps")
    void devraitRefuserUnCodeAbsent() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"etudiantId\": %d }".formatted(awaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("404 ETUDIANT_INCONNU — l'étudiant n'existe pas")
    void devraitRefuserUnEtudiantInconnu() throws Exception {
        mockMvc.perform(marquer(code, 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    @DisplayName("le code saisi en minuscules est accepté")
    void devraitAccepterLeCodeEnMinuscules() throws Exception {
        mockMvc.perform(marquer(code.toLowerCase(), awaId))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("200 — la liste des étudiants de la promotion alimente le choix du nom")
    void devraitListerLesEtudiantsDeLaPromotion() throws Exception {
        mockMvc.perform(get("/api/promotions/{id}/etudiants", promotionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nom").value("Awa Njoya"))
                .andExpect(jsonPath("$[1].nom").value("Biloa Manga"));
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE — lister les étudiants d'une promotion qui n'existe pas")
    void devraitRefuserUnePromotionInconnue() throws Exception {
        mockMvc.perform(get("/api/promotions/{id}/etudiants", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    // --- Présence ajoutée par le formateur (EF4, Q14) -----------------------

    @Test
    @DisplayName("201 — RG4, la présence manuelle porte la source FORMATEUR")
    void devraitEnregistrerUnePresenceManuelle_RG4() throws Exception {
        mockMvc.perform(manuelle(sessions.findAll().getFirst().getId(), awaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etudiantId").value(awaId))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    @DisplayName("RG5 — l'ajout manuel reste possible après l'expiration du code")
    void devraitAccepterUnAjoutApresExpirationDuCode_RG5() throws Exception {
        Promotion promotion = promotions.findAll().getFirst();
        // Séance ouverte il y a une heure : son code est mort depuis quarante-cinq
        // minutes. C'est précisément la situation de Q14.
        Session ancienne = sessions.save(Session.ouvrir(
                promotion, "Séance du matin", "OLD999", Instant.now().minus(Duration.ofHours(1))));

        mockMvc.perform(manuelle(ancienne.getId(), awaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    @DisplayName("409 DEJA_PRESENT — RG3, l'unicité vaut quelle que soit la source")
    void devraitRefuserUnDoublonQuelleQueSoitLaSource_RG3() throws Exception {
        Long sessionId = sessions.findAll().getFirst().getId();
        mockMvc.perform(marquer(code, awaId)).andExpect(status().isCreated());

        mockMvc.perform(manuelle(sessionId, awaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    @DisplayName("404 ETUDIANT_INCONNU — l'étudiant ajouté n'existe pas")
    void devraitRefuserUnEtudiantInconnuALAjoutManuel() throws Exception {
        mockMvc.perform(manuelle(sessions.findAll().getFirst().getId(), 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    @DisplayName("404 SESSION_INCONNUE — la séance n'existe pas")
    void devraitRefuserUneSessionInconnueALAjoutManuel() throws Exception {
        mockMvc.perform(manuelle(999999L, awaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — la séance est absente du corps")
    void devraitRefuserUnCorpsIncompletALAjoutManuel() throws Exception {
        mockMvc.perform(post("/api/presences/manuelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"etudiantId\": %d }".formatted(awaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("EF12 — le détail des présences distingue les deux sources")
    void devraitDistinguerLesSourcesDansLeDetail() throws Exception {
        Long sessionId = sessions.findAll().getFirst().getId();
        mockMvc.perform(marquer(code, awaId)).andExpect(status().isCreated());
        mockMvc.perform(manuelle(sessionId, biloaId)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/sessions/{id}/presences", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.source == 'ETUDIANT')].nom").value("Awa Njoya"))
                .andExpect(jsonPath("$[?(@.source == 'FORMATEUR')].nom").value("Biloa Manga"));
    }

    private org.springframework.test.web.servlet.RequestBuilder manuelle(Long sessionId, Long etudiantId) {
        return post("/api/presences/manuelles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"sessionId\": %d, \"etudiantId\": %d }".formatted(sessionId, etudiantId));
    }

    // --- Limitation des tentatives (EF3, RG6, Q4) ---------------------------

    @Test
    @DisplayName("429 TROP_DE_TENTATIVES — RG6, après cinq codes erronés")
    void devraitBloquerApresCinqEchecs_RG6() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(marquer("FAUX%02d".formatted(i), awaId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }

        mockMvc.perform(marquer("FAUX99", awaId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"));
    }

    @Test
    @DisplayName("RG6, Q4 — un étudiant bloqué reçoit 429 même avec le BON code")
    void devraitBloquerMemeAvecLeBonCode_RG6() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(marquer("FAUX%02d".formatted(i), awaId))
                    .andExpect(status().isBadRequest());
        }

        // C'est tout l'objet de Q4 : si le bon code passait, l'étudiant bloqué
        // saurait qu'il l'a trouvé, et la limitation ne servirait à rien.
        mockMvc.perform(marquer(code, awaId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"));
    }

    @Test
    @DisplayName("RG6 — le blocage est individuel : un autre étudiant n'est pas affecté")
    void devraitNeBloquerQueLEtudiantFautif_RG6() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(marquer("FAUX%02d".formatted(i), awaId))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(marquer(code, biloaId)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RG6 — une réussite intercalée remet le compteur à zéro")
    void devraitRemettreLeCompteurAZeroApresUneReussite_RG6() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(marquer("FAUX%02d".formatted(i), awaId))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(marquer(code, awaId)).andExpect(status().isCreated());

        // Quatre nouveaux échecs après la réussite : toujours pas bloqué.
        for (int i = 5; i <= 8; i++) {
            mockMvc.perform(marquer("FAUX%02d".formatted(i), awaId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }
    }

    private org.springframework.test.web.servlet.RequestBuilder marquer(String code, Long etudiantId) {
        return post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"code\": \"%s\", \"etudiantId\": %d }".formatted(code, etudiantId));
    }
}
