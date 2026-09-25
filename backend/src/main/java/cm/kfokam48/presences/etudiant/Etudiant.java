package cm.kfokam48.presences.etudiant;

import cm.kfokam48.presences.promotion.Promotion;
import jakarta.persistence.*;

@Entity
@Table(name = "etudiants")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false)
    private String nom;

    protected Etudiant() {
        // requis par JPA
    }

    public Etudiant(Promotion promotion, String nom) {
        this.promotion = promotion;
        this.nom = nom;
    }

    public Long getId() {
        return id;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public String getNom() {
        return nom;
    }
}
