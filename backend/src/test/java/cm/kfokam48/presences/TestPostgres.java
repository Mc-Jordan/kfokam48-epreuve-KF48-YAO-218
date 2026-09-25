package cm.kfokam48.presences;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base des tests d'intégration : un PostgreSQL réel, fourni par Testcontainers.
 *
 * <p>Applique ENF7 — les tests tournent sur un poste vierge, sans base installée.
 * Le conteneur est statique et démarré une seule fois pour toute la campagne : le
 * coût de démarrage est payé une fois, pas par classe de test.</p>
 *
 * <p>Le câblage passe par {@code @DynamicPropertySource} plutôt que par
 * {@code spring-boot-testcontainers} : cela évite d'arrimer la version de
 * Testcontainers à celle de Spring Boot, contrainte qui nous a déjà coûté un
 * client Docker incompatible avec les moteurs récents.</p>
 */
public abstract class TestPostgres {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void proprietesDeLaBase(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registre.add("spring.datasource.username", POSTGRES::getUsername);
        registre.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
