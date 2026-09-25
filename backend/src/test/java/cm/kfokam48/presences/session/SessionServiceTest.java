package cm.kfokam48.presences.session;

import cm.kfokam48.presences.partage.erreur.CodeErreur;
import cm.kfokam48.presences.partage.erreur.ExceptionMetier;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
import cm.kfokam48.presences.session.domaine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Règles vérifiées ici sans base ni conteneur : l'horloge est fixe, donc RG1 se
 * teste sans attendre quinze minutes.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant MIDI = Instant.parse("2026-09-25T12:00:00Z");

    @Mock
    private SessionRepository sessions;

    @Mock
    private PromotionRepository promotions;

    @Spy
    private GenerateurDeCode generateur = new GenerateurDeCode();

    @Spy
    private Clock horloge = Clock.fixed(MIDI, ZoneOffset.UTC);

    @InjectMocks
    private SessionService service;

    private Promotion promotion;

    @BeforeEach
    void preparer() {
        promotion = new Promotion("Promotion 2026");
    }

    @Test
    @DisplayName("RG1 — le code expire quinze minutes après l'ouverture")
    void devraitFixerLExpirationAQuinzeMinutes_RG1() {
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(sessions.save(any(Session.class))).thenAnswer(appel -> appel.getArgument(0));

        Session session = service.ouvrir("Algorithmique", 1L);

        assertThat(session.getOuvertureAt()).isEqualTo(MIDI);
        assertThat(session.getExpirationAt()).isEqualTo(MIDI.plus(Duration.ofMinutes(15)));
        assertThat(Duration.between(session.getOuvertureAt(), session.getExpirationAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("RG1 — un code est valide avant son expiration et ne l'est plus après")
    void devraitInvaliderLeCodePasseSonExpiration_RG1() {
        Session session = Session.ouvrir(promotion, "Algorithmique", "ABC234", MIDI);

        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(14)))).isFalse();
        // À la quinzième minute pile, le code ne vaut déjà plus rien : la borne est
        // exclusive, sans quoi « quinze minutes » en vaudrait quinze et une fraction.
        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(15)))).isTrue();
        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(16)))).isTrue();
    }

    @Test
    @DisplayName("RG12 — une session naît ouverte, sans date de clôture ni de finalisation")
    void devraitNaitreOuverte_RG12() {
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(sessions.save(any(Session.class))).thenAnswer(appel -> appel.getArgument(0));

        Session session = service.ouvrir("Algorithmique", 1L);

        assertThat(session.getStatut()).isEqualTo(StatutSession.OUVERTE);
        assertThat(session.getStatut().estOuverte()).isTrue();
        assertThat(session.getClotureAt()).isNull();
        assertThat(session.getFinalisationAt()).isNull();
    }

    @Test
    @DisplayName("RG2 — un code déjà pris est écarté et un autre est tiré")
    void devraitRetirerUnCodeDejaPris_RG2() {
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(sessions.save(any(Session.class))).thenAnswer(appel -> appel.getArgument(0));
        // Les deux premiers codes tirés existent déjà, le troisième est libre.
        when(sessions.existsByCode(anyString())).thenReturn(true, true, false);

        Session session = service.ouvrir("Algorithmique", 1L);

        assertThat(session.getCode()).isNotBlank();
        verify(generateur, times(3)).genererCode();
    }

    @Test
    @DisplayName("RG2 — le code ne contient aucun caractère ambigu à l'oral ou à la saisie")
    void devraitProduireUnCodeSansCaractereAmbigu_RG2() {
        GenerateurDeCode generateurReel = new GenerateurDeCode();

        for (int i = 0; i < 500; i++) {
            assertThat(generateurReel.genererCode())
                    .hasSize(6)
                    .doesNotContainAnyWhitespaces()
                    .matches("[A-HJ-NP-Z2-9]{6}");
        }
    }

    @Test
    @DisplayName("une promotion inconnue est refusée avant toute écriture")
    void devraitRefuserUnePromotionInconnue() {
        when(promotions.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ouvrir("Algorithmique", 404L))
                .isInstanceOf(ExceptionMetier.class)
                .extracting(e -> ((ExceptionMetier) e).code())
                .isEqualTo(CodeErreur.PROMOTION_INCONNUE);

        verify(sessions, never()).save(any());
    }

    @Test
    @DisplayName("lister les sessions d'une promotion inconnue est refusé")
    void devraitRefuserDeListerUnePromotionInconnue() {
        when(promotions.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.listerParPromotion(404L))
                .isInstanceOf(ExceptionMetier.class)
                .extracting(e -> ((ExceptionMetier) e).code())
                .isEqualTo(CodeErreur.PROMOTION_INCONNUE);
    }

    @Test
    @DisplayName("le titre est débarrassé de ses espaces de bordure")
    void devraitNettoyerLeTitre() {
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(sessions.save(any(Session.class))).thenAnswer(appel -> appel.getArgument(0));

        assertThat(service.ouvrir("  Algorithmique  ", 1L).getTitre()).isEqualTo("Algorithmique");
    }
}
