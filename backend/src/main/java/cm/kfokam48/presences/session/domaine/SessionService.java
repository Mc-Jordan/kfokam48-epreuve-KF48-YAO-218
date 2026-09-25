package cm.kfokam48.presences.session.domaine;

import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.promotion.Promotion;
import cm.kfokam48.presences.promotion.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Les opérations de gestion des sessions.
 *
 * <p>Aucune requête en base ne sort d'ici vers un contrôleur, et aucune entité JPA
 * n'en sort non plus : le contrôleur reçoit des objets du domaine et les traduit
 * en DTO (B3).</p>
 */
@Service
public class SessionService {

    /**
     * Le générateur tire dans un alphabet de 32 caractères sur 6 positions, soit
     * plus d'un milliard de codes : une collision est improbable, mais RG2 exige
     * l'unicité, pas l'improbabilité. On retente donc, et on abandonne plutôt que
     * de boucler indéfiniment si quelque chose d'anormal se produit.
     */
    private static final int TENTATIVES_DE_GENERATION = 10;

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final GenerateurDeCode generateur;
    private final Clock horloge;

    public SessionService(SessionRepository sessions, PromotionRepository promotions,
                          GenerateurDeCode generateur, Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.generateur = generateur;
        this.horloge = horloge;
    }

    /** EF1 — ouvre une session et produit son code de présence. */
    @Transactional
    public Session ouvrir(String titre, Long promotionId) {
        Promotion promotion = promotions.findById(promotionId)
                .orElseThrow(Erreurs::promotionInconnue);

        Instant maintenant = Instant.now(horloge);
        return sessions.save(Session.ouvrir(promotion, titre.trim(), genererCodeUnique(), maintenant));
    }

    /** EF1, EF5, EF12 — les sessions d'une promotion, de la plus récente à la plus ancienne. */
    @Transactional(readOnly = true)
    public List<Session> listerParPromotion(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw Erreurs.promotionInconnue();
        }
        return sessions.findByPromotionIdOrderByOuvertureAtDesc(promotionId);
    }

    @Transactional(readOnly = true)
    public Session parIdentifiant(Long sessionId) {
        return sessions.findById(sessionId).orElseThrow(Erreurs::sessionInconnue);
    }

    /** RG2 — deux sessions ne peuvent pas porter le même code. */
    private String genererCodeUnique() {
        for (int tentative = 0; tentative < TENTATIVES_DE_GENERATION; tentative++) {
            String code = generateur.genererCode();
            if (!sessions.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Impossible de produire un code unique en " + TENTATIVES_DE_GENERATION + " tentatives");
    }
}
