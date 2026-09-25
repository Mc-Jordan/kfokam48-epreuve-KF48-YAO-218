package cm.kfokam48.presences.partage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * L'horloge est un bean, jamais {@code Instant.now()} en dur.
 *
 * <p>Deux règles dépendent du temps qui passe — RG1, quinze minutes d'expiration,
 * et RG6, deux minutes de blocage. Sans horloge injectable, les vérifier exigerait
 * d'attendre réellement : les tests seraient lents et, surtout, intermittents.</p>
 */
@Configuration
public class ConfigurationTemps {

    /** UTC partout : l'expiration ne doit pas dépendre du fuseau du serveur. */
    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
