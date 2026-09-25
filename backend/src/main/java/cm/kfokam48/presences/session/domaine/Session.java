package cm.kfokam48.presences.session.domaine;

import cm.kfokam48.presences.promotion.Promotion;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

/**
 * Une séance de cours ouverte par le formateur.
 *
 * <p>L'entité porte les règles qui ne dépendent que d'elle-même : la durée de vie
 * du code (RG1) et les transitions d'état (RG12). Ce qui exige de consulter
 * d'autres tables — l'attribution des relecteurs, par exemple — reste au service.</p>
 */
@Entity
@Table(name = "sessions")
public class Session {

    /** RG1 — le code expire quinze minutes après l'ouverture de la session. */
    public static final Duration DUREE_DE_VIE_DU_CODE = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private Instant ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutSession statut;

    @Column(name = "cloture_at")
    private Instant clotureAt;

    @Column(name = "finalisation_at")
    private Instant finalisationAt;

    protected Session() {
        // requis par JPA
    }

    /**
     * Ouvre une session. L'expiration est calculée une fois et stockée, plutôt que
     * recalculée à chaque lecture : la règle reste vraie même si sa valeur change
     * un jour, et le diagnostic d'un code refusé est lisible en base.
     */
    public static Session ouvrir(Promotion promotion, String titre, String code, Instant maintenant) {
        Session session = new Session();
        session.promotion = promotion;
        session.titre = titre;
        session.code = code;
        session.ouvertureAt = maintenant;
        session.expirationAt = maintenant.plus(DUREE_DE_VIE_DU_CODE);
        session.statut = StatutSession.OUVERTE;
        return session;
    }

    /** RG1 — le code ne vaut plus rien passé son expiration. */
    public boolean codeExpireA(Instant maintenant) {
        return !maintenant.isBefore(expirationAt);
    }

    public Long getId() {
        return id;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public String getTitre() {
        return titre;
    }

    public String getCode() {
        return code;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public StatutSession getStatut() {
        return statut;
    }

    public Instant getClotureAt() {
        return clotureAt;
    }

    public Instant getFinalisationAt() {
        return finalisationAt;
    }
}
