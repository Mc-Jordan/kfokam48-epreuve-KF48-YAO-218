package cm.kfokam48.presences.exercice.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.session.domaine.Session;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "exercices")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(nullable = false, columnDefinition = "text")
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StatutExercice statut;

    @Column(name = "depose_at", nullable = false)
    private Instant deposeAt;

    @Column(name = "modifie_at")
    private Instant modifieAt;

    protected Exercice() {
        // requis par JPA
    }

    public static Exercice deposer(Session session, Etudiant etudiant, String lien, Instant maintenant) {
        Exercice exercice = new Exercice();
        exercice.session = session;
        exercice.etudiant = etudiant;
        exercice.lien = lien;
        exercice.statut = StatutExercice.DEPOSE;
        exercice.deposeAt = maintenant;
        return exercice;
    }

    /** RG10 — le remplacement n'est permis que tant que la session est ouverte. */
    public void remplacerLien(String nouveauLien, Instant maintenant) {
        this.lien = nouveauLien;
        this.modifieAt = maintenant;
    }

    public Long getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public String getLien() {
        return lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }

    public Instant getModifieAt() {
        return modifieAt;
    }
}
