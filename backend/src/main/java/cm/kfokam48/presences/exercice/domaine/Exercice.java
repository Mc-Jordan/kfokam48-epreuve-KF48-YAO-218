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

    /** RG13 — à la clôture, un relecteur a été tiré : l'exercice attend sa relecture. */
    public void enAttenteDeRelecture() {
        this.statut = StatutExercice.EN_ATTENTE_RELECTURE;
    }

    /**
     * RG17 — à la clôture, aucun relecteur éligible n'existait. État terminal :
     * l'attribution n'ayant lieu qu'une fois, plus rien ne peut le rattraper.
     */
    public void devenirNonAttribuable() {
        this.statut = StatutExercice.NON_ATTRIBUABLE;
    }

    /**
     * Une relecture vient d'être rendue.
     *
     * <p>RG24 : l'exercice n'est pleinement relu que lorsque <em>toutes</em> les
     * relectures attendues le sont. Tant qu'il en manque une, il reste
     * {@link StatutExercice#RELU_PARTIEL} et sa note est provisoire.</p>
     */
    public void relectureRendue(int rendues, int attendues) {
        this.statut = rendues >= attendues ? StatutExercice.RELU : StatutExercice.RELU_PARTIEL;
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
