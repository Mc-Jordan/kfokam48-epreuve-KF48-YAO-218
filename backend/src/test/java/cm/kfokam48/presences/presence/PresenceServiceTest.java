package cm.kfokam48.presences.presence;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.partage.erreur.CodeErreur;
import cm.kfokam48.presences.partage.erreur.ExceptionMetier;
import cm.kfokam48.presences.presence.domaine.*;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Le marquage de présence, éprouvé sans base ni conteneur.
 *
 * <p>L'horloge fixe permet de franchir l'expiration de RG1 sans attendre quinze
 * minutes. L'ordre des contrôles de D3 est vérifié explicitement : c'est une
 * garantie de sécurité, et rien dans le code ne l'imposerait sinon.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceServiceTest {

    private static final Instant MIDI = Instant.parse("2026-09-25T12:00:00Z");
    private static final String CODE = "ABC234";

    @Mock
    private PresenceRepository presences;

    @Mock
    private SessionRepository sessions;

    @Mock
    private EtudiantService etudiants;

    private Etudiant etudiant;
    private Session session;

    private PresenceService serviceA(Instant maintenant) {
        return new PresenceService(presences, sessions, etudiants,
                Clock.fixed(maintenant, ZoneOffset.UTC));
    }

    @BeforeEach
    void preparer() {
        Promotion promotion = new Promotion("Promotion 2026");
        etudiant = new Etudiant(promotion, "Awa Njoya");
        session = Session.ouvrir(promotion, "Algorithmique", CODE, MIDI);

        when(etudiants.parIdentifiant(anyLong())).thenReturn(etudiant);
        when(sessions.findByCode(CODE)).thenReturn(Optional.of(session));
        when(presences.save(any(Presence.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    @DisplayName("cas nominal — la présence est enregistrée avec la source ETUDIANT")
    void devraitEnregistrerLaPresence_RG4() {
        Presence presence = serviceA(MIDI.plus(Duration.ofMinutes(5))).marquer(CODE, 1L);

        assertThat(presence.getSource()).isEqualTo(SourcePresence.ETUDIANT);
        assertThat(presence.getEtudiant()).isSameAs(etudiant);
        assertThat(presence.getSession()).isSameAs(session);
        assertThat(presence.getEnregistreeAt()).isEqualTo(MIDI.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("RG2 — un code qui ne désigne aucune session est refusé")
    void devraitRefuserUnCodeInconnu_RG2() {
        when(sessions.findByCode("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatCodeErreur(() -> serviceA(MIDI).marquer("ZZZZZZ", 1L))
                .isEqualTo(CodeErreur.CODE_INCONNU);
        verify(presences, never()).save(any());
    }

    @Test
    @DisplayName("RG1 — un code passé sa quinzième minute est refusé")
    void devraitRefuserUnCodeExpire_RG1() {
        assertThatCodeErreur(() -> serviceA(MIDI.plus(Duration.ofMinutes(15))).marquer(CODE, 1L))
                .isEqualTo(CodeErreur.CODE_EXPIRE);
        verify(presences, never()).save(any());
    }

    @Test
    @DisplayName("RG1 — à la quatorzième minute, le code vaut encore")
    void devraitAccepterUnCodeEncoreValide_RG1() {
        assertThat(serviceA(MIDI.plus(Duration.ofMinutes(14))).marquer(CODE, 1L)).isNotNull();
    }

    @Test
    @DisplayName("RG3 — une seconde présence sur la même session est refusée")
    void devraitRefuserUneSecondePresence_RG3() {
        when(presences.existsBySessionIdAndEtudiantId(any(), any())).thenReturn(true);

        assertThatCodeErreur(() -> serviceA(MIDI).marquer(CODE, 1L))
                .isEqualTo(CodeErreur.DEJA_PRESENT);
        verify(presences, never()).save(any());
    }

    @Test
    @DisplayName("D3 — l'expiration est vérifiée avant l'unicité : un code périmé renvoie 410, pas 409")
    void devraitPrivilegierLExpirationSurLUnicite_D3() {
        // L'étudiant est déjà présent ET le code est expiré : les deux règles
        // s'appliquent. D3 tranche en faveur de l'expiration, parce que
        // l'information utile est que le code ne vaut plus rien.
        when(presences.existsBySessionIdAndEtudiantId(any(), any())).thenReturn(true);

        assertThatCodeErreur(() -> serviceA(MIDI.plus(Duration.ofMinutes(20))).marquer(CODE, 1L))
                .isEqualTo(CodeErreur.CODE_EXPIRE);
    }

    @Test
    @DisplayName("D3 — un code inconnu est détecté avant toute autre vérification")
    void devraitDetecterLeCodeInconnuEnPremier_D3() {
        when(sessions.findByCode("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatCodeErreur(() -> serviceA(MIDI.plus(Duration.ofHours(3))).marquer("ZZZZZZ", 1L))
                .isEqualTo(CodeErreur.CODE_INCONNU);
        // L'unicité n'est jamais consultée : il n'y a pas de session à consulter.
        verify(presences, never()).existsBySessionIdAndEtudiantId(any(), any());
    }

    @Test
    @DisplayName("le code est accepté en minuscules et avec des espaces autour")
    void devraitNormaliserLeCodeSaisi() {
        assertThat(serviceA(MIDI).marquer("  abc234  ", 1L)).isNotNull();
        verify(sessions).findByCode(CODE);
    }

    @Test
    @DisplayName("un étudiant inconnu est refusé avant toute écriture")
    void devraitRefuserUnEtudiantInconnu() {
        when(etudiants.parIdentifiant(404L))
                .thenThrow(new ExceptionMetier(CodeErreur.ETUDIANT_INCONNU));

        assertThatCodeErreur(() -> serviceA(MIDI).marquer(CODE, 404L))
                .isEqualTo(CodeErreur.ETUDIANT_INCONNU);
        verify(presences, never()).save(any());
    }

    /** Exécute l'action, exige une exception métier, et rend son code pour l'assertion. */
    private static org.assertj.core.api.AbstractComparableAssert<?, CodeErreur> assertThatCodeErreur(
            Runnable action) {
        ExceptionMetier levee = org.junit.jupiter.api.Assertions.assertThrows(
                ExceptionMetier.class, action::run);
        return assertThat(levee.code());
    }
}
