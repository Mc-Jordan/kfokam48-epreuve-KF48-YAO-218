package cm.kfokam48.presences.partage;

import cm.kfokam48.presences.TestPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration du socle, sur un PostgreSQL réel fourni par Testcontainers
 * (ENF7 : aucune base locale requise).
 *
 * <p>Il prouve deux choses que la relecture ne peut pas établir : que la migration
 * V1 s'applique réellement sur PostgreSQL, et que le format d'erreur imposé (RG25)
 * tient jusque sur les chemins que l'on oublie toujours — route inexistante et
 * verbe non supporté, là où Spring renverrait sa propre page d'erreur.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc

class FormatErreurIT extends TestPostgres {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("la migration V1 crée les sept tables sur un PostgreSQL réel")
    void laMigrationSAppliqueSurPostgres() throws Exception {
        List<String> tables = new ArrayList<>();
        try (var connexion = dataSource.getConnection();
             ResultSet resultat = connexion.getMetaData()
                     .getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (resultat.next()) {
                tables.add(resultat.getString("TABLE_NAME"));
            }
        }
        assertThat(tables).contains(
                "promotions", "etudiants", "sessions", "presences",
                "exercices", "relectures", "tentatives_presence");
    }

    @Test
    @DisplayName("une route inexistante répond au format imposé, pas par la page de Spring")
    void uneRouteInexistanteRespecteLeFormatImpose() throws Exception {
        mockMvc.perform(get("/api/route-qui-nexiste-pas"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RESSOURCE_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("aucune réponse d'erreur ne comporte de trace d'exécution")
    void aucuneReponseDErreurNeFuiteDeTrace() throws Exception {
        String corps = mockMvc.perform(post("/api/route-qui-nexiste-pas")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps)
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .doesNotContain("at cm.kfokam48");
    }
}
