package cm.kfokam48.presences.presence.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.session.domaine.Session;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "presences")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourcePresence source;

    @Column(name = "enregistree_at", nullable = false)
    private Instant enregistreeAt;

    protected Presence() {
        // requis par JPA
    }

    public static Presence enregistrer(Session session, Etudiant etudiant,
                                       SourcePresence source, Instant maintenant) {
        Presence presence = new Presence();
        presence.session = session;
        presence.etudiant = etudiant;
        presence.source = source;
        presence.enregistreeAt = maintenant;
        return presence;
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

    public SourcePresence getSource() {
        return source;
    }

    public Instant getEnregistreeAt() {
        return enregistreeAt;
    }
}
