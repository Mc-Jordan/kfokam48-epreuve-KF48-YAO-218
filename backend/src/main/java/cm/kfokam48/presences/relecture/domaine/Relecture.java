package cm.kfokam48.presences.relecture.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.session.domaine.Session;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * La relecture d'un exercice par un pair.
 *
 * <p>L'entité naît à l'attribution, sans note : c'est son existence même, à l'état
 * {@link StatutRelecture#ATTRIBUEE}, qui rend l'attente visible au formateur (RG23).</p>
 *
 * <p>{@code session} est dénormalisé depuis l'exercice. La colonne est redondante,
 * mais sans elle RG16 — un étudiant relit au plus un exercice par session — n'est
 * exprimable par aucune contrainte. Une clé étrangère composite vers
 * {@code exercices (id, session_id)} interdit à la base de laisser les deux diverger.</p>
 */
@Entity
@Table(name = "relectures")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** RG22 — jamais exposé dans une réponse destinée à l'auteur de l'exercice. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    private Short note;

    @Column(columnDefinition = "text")
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutRelecture statut;

    @Column(name = "attribuee_at", nullable = false)
    private Instant attribueeAt;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    protected Relecture() {
        // requis par JPA
    }

    /** RG14, RG19 — le relecteur est tiré parmi les présents, jamais l'auteur. */
    public static Relecture attribuer(Exercice exercice, Etudiant relecteur, Instant maintenant) {
        if (relecteur.getId().equals(exercice.getEtudiant().getId())) {
            throw Erreurs.autoRelecture();
        }
        Relecture relecture = new Relecture();
        relecture.exercice = exercice;
        relecture.session = exercice.getSession();
        relecture.relecteur = relecteur;
        relecture.statut = StatutRelecture.ATTRIBUEE;
        relecture.attribueeAt = maintenant;
        return relecture;
    }

    /** RG20 — rendre ou corriger, tant que la session n'est pas finalisée. */
    public void rendre(short note, String commentaire, Instant maintenant) {
        if (statut == StatutRelecture.FIGEE) {
            throw Erreurs.relectureDejaRendue();
        }
        if (note < 0 || note > 20) {
            throw Erreurs.noteInvalide();
        }
        this.note = note;
        this.commentaire = commentaire;
        this.statut = StatutRelecture.RENDUE;
        this.rendueAt = maintenant;
    }

    /** RG21 — la finalisation fige. Une relecture jamais rendue le reste (RG23). */
    public void figer() {
        if (statut == StatutRelecture.RENDUE) {
            this.statut = StatutRelecture.FIGEE;
        }
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Session getSession() {
        return session;
    }

    public Etudiant getRelecteur() {
        return relecteur;
    }

    public Short getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public StatutRelecture getStatut() {
        return statut;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }
}
