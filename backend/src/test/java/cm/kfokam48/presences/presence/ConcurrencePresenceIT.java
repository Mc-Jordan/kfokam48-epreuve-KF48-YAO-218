package cm.kfokam48.presences.presence;

import cm.kfokam48.presences.TestPostgres;
import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.etudiant.EtudiantRepository;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.exercice.domaine.ExerciceService;
import cm.kfokam48.presences.partage.erreur.CodeErreur;
import cm.kfokam48.presences.partage.erreur.ExceptionMetier;
import cm.kfokam48.presences.presence.domaine.PresenceRepository;
import cm.kfokam48.presences.presence.domaine.PresenceService;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
import cm.kfokam48.presences.session.domaine.Session;
import cm.kfokam48.presences.session.domaine.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deux envois simultanés d'un même étudiant — le bug signalé à l'étape 3.
 *
 * <p>La donnée n'est jamais perdue : la contrainte d'unicité posée en {@code V1}
 * la protège. Le défaut est dans la <strong>réponse</strong> : la violation de
 * contrainte remontait au client en {@code 500 ERREUR_INTERNE} alors que le
 * contrat impose {@code 409 DEJA_PRESENT}. L'étudiant croyait ne pas être
 * enregistré alors qu'il l'était.</p>
 *
 * <p>Les tests sont répétés : une course qui passe une fois ne prouve rien. Ils
 * échouaient de façon reproductible avant le correctif.</p>
 */
@SpringBootTest
class ConcurrencePresenceIT extends TestPostgres {

    private static final int ENVOIS_SIMULTANES = 8;

    @Autowired private PresenceService presenceService;
    @Autowired private ExerciceService exerciceService;
    @Autowired private PromotionRepository promotions;
    @Autowired private EtudiantRepository etudiants;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;
    @Autowired private ExerciceRepository exercices;

    private Long sessionId;
    private Long etudiantId;
    private String code;

    @BeforeEach
    void preparer() {
        exercices.deleteAll();
        presences.deleteAll();
        sessions.deleteAll();
        etudiants.deleteAll();
        promotions.deleteAll();

        Promotion promotion = promotions.save(new Promotion("Promotion de test"));
        etudiantId = etudiants.save(new Etudiant(promotion, "Awa Njoya")).getId();
        code = "RACE01";
        sessionId = sessions.save(Session.ouvrir(promotion, "Algorithmique", code, Instant.now())).getId();
    }

    @RepeatedTest(value = 5, name = "rafale {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG3 — huit marquages simultanés : une présence, et sept 409, jamais un 500")
    void devraitRepondre409EtNon500SurMarquageSimultane_RG3() {
        List<Object> resultats = enRafale(() -> presenceService.marquer(code, etudiantId));

        assertThat(presences.count()).as("la contrainte protège la donnée").isEqualTo(1);
        assertThat(succes(resultats)).as("un seul marquage aboutit").isEqualTo(1);
        assertThat(codesDErreur(resultats))
                .as("les autres reçoivent le code du contrat, jamais une erreur interne")
                .containsOnly(CodeErreur.DEJA_PRESENT);
    }

    @RepeatedTest(value = 5, name = "rafale {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG3 — l'ajout manuel simultané répond lui aussi 409")
    void devraitRepondre409SurAjoutManuelSimultane_RG3() {
        List<Object> resultats =
                enRafale(() -> presenceService.enregistrerManuellement(sessionId, etudiantId));

        assertThat(presences.count()).isEqualTo(1);
        assertThat(succes(resultats)).isEqualTo(1);
        assertThat(codesDErreur(resultats)).containsOnly(CodeErreur.DEJA_PRESENT);
    }

    @RepeatedTest(value = 5, name = "rafale {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG7 — huit dépôts simultanés : un exercice, et sept EXERCICE_DEJA_DEPOSE")
    void devraitRepondre409SurDepotSimultane_RG7() {
        presenceService.marquer(code, etudiantId);

        List<Object> resultats = enRafale(
                () -> exerciceService.deposer(sessionId, etudiantId, "https://exemple.cm/tp"));

        assertThat(exercices.count()).isEqualTo(1);
        assertThat(succes(resultats)).isEqualTo(1);
        assertThat(codesDErreur(resultats)).containsOnly(CodeErreur.EXERCICE_DEJA_DEPOSE);
    }

    @RepeatedTest(value = 3, name = "rafale {currentRepetition}/{totalRepetitions}")
    @DisplayName("RG3 — huit étudiants distincts et simultanés sont tous enregistrés")
    void devraitEnregistrerTousLesEtudiantsDistincts_RG3() {
        Promotion promotion = promotions.findAll().getFirst();
        List<Long> autres = new ArrayList<>();
        for (int i = 1; i <= ENVOIS_SIMULTANES; i++) {
            autres.add(etudiants.save(new Etudiant(promotion, "Pair " + i + "-" + System.nanoTime())).getId());
        }

        // Le cas nominal ne doit pas régresser : des étudiants différents ne se
        // gênent pas, et le correctif ne doit pas les transformer en conflits.
        List<Object> resultats = enRafale(autres, id -> presenceService.marquer(code, id));

        assertThat(succes(resultats)).isEqualTo(ENVOIS_SIMULTANES);
        assertThat(codesDErreur(resultats)).isEmpty();
    }

    // --- outillage ----------------------------------------------------------

    /** Lance {@link #ENVOIS_SIMULTANES} appels identiques au même instant. */
    private List<Object> enRafale(Supplier<Object> appel) {
        List<Long> memes = java.util.Collections.nCopies(ENVOIS_SIMULTANES, etudiantId);
        return enRafale(memes, ignore -> appel.get());
    }

    /** Lance un appel par identifiant fourni, tous relâchés en même temps. */
    private List<Object> enRafale(List<Long> identifiants, java.util.function.Function<Long, Object> appel) {
        CountDownLatch top = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(identifiants.size());
        List<Future<Object>> lances = new ArrayList<>();
        try {
            for (Long id : identifiants) {
                lances.add(pool.submit(() -> {
                    top.await();
                    try {
                        return appel.apply(id);
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            top.countDown();
            List<Object> resultats = new ArrayList<>();
            for (Future<Object> lance : lances) {
                resultats.add(lance.get(30, TimeUnit.SECONDS));
            }
            return resultats;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static long succes(List<Object> resultats) {
        return resultats.stream().filter(r -> !(r instanceof Exception)).count();
    }

    /**
     * Les codes d'erreur métier renvoyés. Toute exception qui n'est pas une
     * {@link ExceptionMetier} est signalée telle quelle : c'est précisément le
     * défaut que ce test traque.
     */
    private static List<CodeErreur> codesDErreur(List<Object> resultats) {
        return resultats.stream()
                .filter(Exception.class::isInstance)
                .map(r -> {
                    if (r instanceof ExceptionMetier metier) {
                        return metier.code();
                    }
                    throw new AssertionError(
                            "exception non métier remontée au client : " + r.getClass().getName()
                                    + " — le contrat impose un code stable", (Throwable) r);
                })
                .toList();
    }
}
