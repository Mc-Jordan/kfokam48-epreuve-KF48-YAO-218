package cm.kfokam48.presences.presence.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Une tentative de marquage de présence, réussie ou non (RG6).
 *
 * <p>Conserver les tentatives plutôt qu'un compteur sur l'étudiant est ce qui rend
 * la fenêtre glissante de deux minutes exprimable, et le comportement testable sans
 * attendre.</p>
 */
@Entity
@Table(name = "tentatives_presence")
public class TentativePresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(name = "code_saisi", nullable = false, length = 16)
    private String codeSaisi;

    @Column(nullable = false)
    private boolean reussie;

    @Column(name = "tentee_at", nullable = false)
    private Instant tenteeAt;

    protected TentativePresence() {
        // requis par JPA
    }

    public static TentativePresence tracer(Etudiant etudiant, String code, boolean reussie, Instant maintenant) {
        TentativePresence tentative = new TentativePresence();
        tentative.etudiant = etudiant;
        // Le code est tronqué : la colonne fait seize caractères, et une saisie
        // aberrante ne doit pas faire échouer l'enregistrement de la tentative.
        tentative.codeSaisi = code.length() > 16 ? code.substring(0, 16) : code;
        tentative.reussie = reussie;
        tentative.tenteeAt = maintenant;
        return tentative;
    }

    public boolean estUnEchec() {
        return !reussie;
    }

    public Instant getTenteeAt() {
        return tenteeAt;
    }
}
