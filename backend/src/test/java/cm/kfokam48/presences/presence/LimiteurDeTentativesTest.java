package cm.kfokam48.presences.presence;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.partage.erreur.CodeErreur;
import cm.kfokam48.presences.partage.erreur.ExceptionMetier;
import cm.kfokam48.presences.presence.domaine.LimiteurDeTentatives;
import cm.kfokam48.presences.presence.domaine.TentativePresence;
import cm.kfokam48.presences.presence.domaine.TentativePresenceRepository;
import cm.kfokam48.presences.promotion.Promotion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RG6 — cinq codes erronés consécutifs bloquent l'étudiant deux minutes.
 *
 * <p>Le deuxième test unitaire attendu par {@code B6}. L'horloge est un paramètre :
 * la fenêtre de deux minutes se franchit sans attendre, et le test est reproductible.</p>
 */
@ExtendWith(MockitoExtension.class)
class LimiteurDeTentativesTest {

    private static final Instant MIDI = Instant.parse("2026-09-25T12:00:00Z");
    private static final Long AWA = 1L;

    @Mock
    private TentativePresenceRepository tentatives;

    private LimiteurDeTentatives limiteur() {
        return new LimiteurDeTentatives(tentatives);
    }

    @Test
    @DisplayName("RG6 — quatre échecs ne bloquent pas : le client a dit cinq")
    void devraitLaisserPasserQuatreEchecs_RG6() {
        when(tentatives.findByEtudiantIdOrderByTenteeAtDesc(eq(AWA), any(Limit.class)))
                .thenReturn(echecs(4, MIDI));

        assertThatNoException().isThrownBy(() -> limiteur().verifier(AWA, MIDI));
    }

    @Test
    @DisplayName("RG6 — au cinquième échec, la tentative suivante est refusée")
    void devraitBloquerApresCinqEchecs_RG6() {
        when(tentatives.findByEtudiantIdOrderByTenteeAtDesc(eq(AWA), any(Limit.class)))
                .thenReturn(echecs(5, MIDI));

        assertThat(assertThrows(ExceptionMetier.class,
                () -> limiteur().verifier(AWA, MIDI.plusSeconds(1))).code())
                .isEqualTo(CodeErreur.TROP_DE_TENTATIVES);
    }

    @Test
    @DisplayName("RG6 — le blocage dure deux minutes, pas une de plus")
    void devraitLeverLeBlocageApresDeuxMinutes_RG6() {
        when(tentatives.findByEtudiantIdOrderByTenteeAtDesc(eq(AWA), any(Limit.class)))
                .thenReturn(echecs(5, MIDI));
        LimiteurDeTentatives limiteur = limiteur();

        // À une seconde près, le blocage tient encore.
        assertThrows(ExceptionMetier.class,
                () -> limiteur.verifier(AWA, MIDI.plus(Duration.ofSeconds(119))));

        // À la deux-centième seconde, il est levé, sans aucune intervention.
        assertThatNoException()
                .isThrownBy(() -> limiteur.verifier(AWA, MIDI.plus(Duration.ofMinutes(2))));
    }

    @Test
    @DisplayName("RG6 — une tentative réussie parmi les cinq dernières remet le compteur à zéro")
    void devraitRemettreLeCompteurAZeroApresUneReussite_RG6() {
        List<TentativePresence> melangees = new ArrayList<>(echecs(4, MIDI));
        melangees.add(TentativePresence.tracer(
                etudiant(), "ABC234", true, MIDI.minus(Duration.ofSeconds(30))));

        when(tentatives.findByEtudiantIdOrderByTenteeAtDesc(eq(AWA), any(Limit.class)))
                .thenReturn(melangees);

        assertThatNoException().isThrownBy(() -> limiteur().verifier(AWA, MIDI.plusSeconds(1)));
    }

    @Test
    @DisplayName("RG6 — un étudiant sans historique n'est jamais bloqué")
    void devraitLaisserPasserSansHistorique_RG6() {
        when(tentatives.findByEtudiantIdOrderByTenteeAtDesc(eq(AWA), any(Limit.class)))
                .thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> limiteur().verifier(AWA, MIDI));
    }

    @Test
    @DisplayName("un code aberrant est tronqué et n'empêche pas de tracer la tentative")
    void devraitTronquerUnCodeAberrant() {
        TentativePresence tracee = TentativePresence.tracer(
                etudiant(), "A".repeat(500), false, MIDI);

        // La colonne fait seize caractères : une saisie démesurée ne doit pas faire
        // échouer l'enregistrement, sans quoi le compteur de RG6 ne monterait pas.
        assertThat(tracee.estUnEchec()).isTrue();
    }

    private List<TentativePresence> echecs(int combien, Instant dernier) {
        List<TentativePresence> liste = new ArrayList<>();
        for (int i = 0; i < combien; i++) {
            liste.add(TentativePresence.tracer(
                    etudiant(), "FAUX" + i, false, dernier.minus(Duration.ofSeconds(i * 5L))));
        }
        return liste;   // du plus récent au plus ancien, comme le fait le dépôt
    }

    private Etudiant etudiant() {
        return new Etudiant(new Promotion("Promotion 2026"), "Awa Njoya");
    }
}
