package cm.kfokam48.presences.relecture.domaine;

import cm.kfokam48.presences.etudiant.EtudiantService;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.exercice.domaine.ExerciceRepository;
import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.session.domaine.StatutSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Les opérations du relecteur, et la consultation de sa note par l'auteur.
 */
@Service
public class RelectureService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final EtudiantService etudiants;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures, ExerciceRepository exercices,
                            EtudiantService etudiants, Clock horloge) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.etudiants = etudiants;
        this.horloge = horloge;
    }

    /** EF8 — les relectures attribuées à un étudiant. */
    @Transactional(readOnly = true)
    public List<Relecture> listerPourRelecteur(Long relecteurId, StatutRelecture statut) {
        etudiants.parIdentifiant(relecteurId);   // 404 ETUDIANT_INCONNU plutôt qu'une liste vide trompeuse

        List<Relecture> siennes = relectures.findAvecExerciceEtSessionParRelecteur(relecteurId);
        return statut == null ? siennes : siennes.stream().filter(r -> r.getStatut() == statut).toList();
    }

    /**
     * EF9 — rendre une relecture, ou la corriger.
     *
     * <p>RG20 : un second envoi avant la finalisation remplace le premier et
     * renvoie {@code 200}. C'est l'arbitrage de Q10 contre Q15 — la note reste
     * modifiable, mais jusqu'à un acte explicite du formateur.</p>
     *
     * <p>RG24 : l'exercice passe à {@code RELU} quand les deux relecteurs ont rendu,
     * et reste à {@code RELU_PARTIEL} tant qu'il en manque un.</p>
     */
    @Transactional
    public Relecture rendre(Long relectureId, int note, String commentaire) {
        Relecture relecture = relectures.findById(relectureId).orElseThrow(Erreurs::relectureInconnue);

        // RG18 — la borne est vérifiée avant tout le reste : une note hors
        // domaine est une erreur de saisie, pas un conflit d'état.
        if (note < 0 || note > 20) {
            throw Erreurs.noteInvalide();
        }

        // RG21 — après la finalisation, plus rien ne bouge.
        if (relecture.getSession().getStatut() == StatutSession.FINALISEE) {
            throw Erreurs.relectureDejaRendue();
        }

        relecture.rendre((short) note, commentaire, Instant.now(horloge));
        Relecture enregistree = relectures.saveAndFlush(relecture);

        // RG24 — l'exercice n'est pleinement relu que lorsque les deux relecteurs
        // ont rendu. Tant qu'il en manque un, sa note existe mais reste provisoire.
        Exercice exercice = relecture.getExercice();
        NoteDUnExercice etatDeLaNote = NoteDUnExercice.de(relectures.findByExerciceId(exercice.getId()));
        exercice.relectureRendue(etatDeLaNote.relecturesRendues(), etatDeLaNote.relecturesAttendues());
        exercices.save(exercice);

        return enregistree;
    }

    /**
     * EF11 — la relecture reçue par l'auteur d'un exercice.
     *
     * <p>Rend l'exercice et la note qui en découle : moyenne des relectures rendues,
     * caractère provisoire, commentaires. L'anonymat de RG22 est tenu par le DTO, qui
     * ne comporte aucun champ identifiant les relecteurs.</p>
     */
    @Transactional(readOnly = true)
    public ResultatConsultation consulterPourAuteur(Long exerciceId) {
        Exercice exercice = exercices.findById(exerciceId).orElseThrow(Erreurs::exerciceInconnu);
        return new ResultatConsultation(exercice, NoteDUnExercice.de(relectures.findByExerciceId(exerciceId)));
    }

    /** Un exercice et la note qui en découle — moyenne, caractère provisoire, commentaires. */
    public record ResultatConsultation(Exercice exercice, NoteDUnExercice note) {
    }
}
