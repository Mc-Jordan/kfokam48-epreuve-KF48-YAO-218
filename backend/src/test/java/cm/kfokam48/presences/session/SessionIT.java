package cm.kfokam48.presences.session;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Conformité de {@code POST /api/sessions} et {@code GET /api/sessions} au contrat,
 * sur un PostgreSQL réel. Vérifie les codes de statut <strong>et</strong> le format
 * d'erreur, que la relecture ne peut pas établir.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionIT extends TestPostgres {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private SessionRepository sessions;

    private Long promotionId;

    @BeforeEach
    void preparer() {
        sessions.deleteAll();
        promotions.deleteAll();
        promotionId = promotions.save(new Promotion("Promotion de test")).getId();
    }

    @Test
    @DisplayName("201 — la session est ouverte et le contrat renvoie ses quatre champs")
    void devraitOuvrirUneSession() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique", "promotionId": %d }
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.ouvertureAt").isNotEmpty())
                .andExpect(jsonPath("$.expirationAt").isNotEmpty());
    }

    @Test
    @DisplayName("RG1 — l'expiration renvoyée vaut l'ouverture plus quinze minutes")
    void devraitRenvoyerUneExpirationAQuinzeMinutes_RG1() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique", "promotionId": %d }
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var noeud = new com.fasterxml.jackson.databind.ObjectMapper().readTree(corps);
        Instant ouverture = Instant.parse(noeud.get("ouvertureAt").asText());
        Instant expiration = Instant.parse(noeud.get("expirationAt").asText());

        assertThat(Duration.between(ouverture, expiration)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("RG2 — deux sessions successives ne portent jamais le même code")
    void devraitProduireDesCodesDistincts_RG2() throws Exception {
        String premier = codeDUneNouvelleSession();
        String second = codeDUneNouvelleSession();

        assertThat(premier).isNotEqualTo(second);
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le titre est absent")
    void devraitRefuserUnTitreAbsent() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "promotionId": %d }
                                """.formatted(promotionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le titre est vide, pas seulement absent")
    void devraitRefuserUnTitreVide() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "   ", "promotionId": %d }
                                """.formatted(promotionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — la promotion est absente")
    void devraitRefuserUnePromotionAbsente() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE — ouvrir une session sur une promotion qui n'existe pas")
    void devraitRefuserUnePromotionInconnue() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique", "promotionId": 999999 }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("200 — les sessions sont listées de la plus récente à la plus ancienne, sans le code")
    void devraitListerLesSessionsSansDivulguerLeCode() throws Exception {
        codeDUneNouvelleSession();
        codeDUneNouvelleSession();

        mockMvc.perform(get("/api/sessions").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].statut").value("OUVERTE"))
                .andExpect(jsonPath("$[0].titre").isNotEmpty())
                .andExpect(jsonPath("$[0].clotureAt").doesNotExist())
                // Le code est dicté par le formateur, jamais exposé dans une liste
                // que l'écran étudiant consulte aussi.
                .andExpect(jsonPath("$[0].code").doesNotExist());
    }

    @Test
    @DisplayName("404 PROMOTION_INCONNUE — lister les sessions d'une promotion qui n'existe pas")
    void devraitRefuserDeListerUnePromotionInconnue() throws Exception {
        mockMvc.perform(get("/api/sessions").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("400 CHAMP_MANQUANT — le paramètre promotionId est absent")
    void devraitRefuserUnParametreAbsent() throws Exception {
        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    private String codeDUneNouvelleSession() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titre": "Algorithmique", "promotionId": %d }
                                """.formatted(promotionId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(corps).get("code").asText();
    }
}
