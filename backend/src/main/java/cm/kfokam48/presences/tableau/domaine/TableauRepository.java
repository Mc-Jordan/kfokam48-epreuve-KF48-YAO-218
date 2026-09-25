package cm.kfokam48.presences.tableau.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TableauRepository extends JpaRepository<Etudiant, Long> {

    /**
     * Le tableau du formateur, en <strong>une seule requête</strong> (ENF2).
     *
     * <h2>Pourquoi une requête native</h2>
     *
     * <p>Depuis l'étape 3, la note d'un exercice est la <em>moyenne de ses deux
     * relectures</em> (RG24), et la moyenne d'un étudiant est la moyenne de ses
     * <em>notes d'exercice</em>. Ce sont deux agrégations imbriquées.</p>
     *
     * <p>Moyenner directement toutes les notes brutes donnerait un résultat faux dès
     * que les exercices n'ont pas le même nombre de relectures — ce qui est le cas
     * des séances clôturées avant le changement, qui n'en ont qu'une. Un exercice
     * relu deux fois pèserait alors double. JPQL ne permet pas d'agréger le résultat
     * d'une agrégation : d'où la requête native et sa table dérivée.</p>
     *
     * <h2>Ce que chaque colonne porte</h2>
     * <ul>
     *   <li><strong>RG23</strong> — {@code relecturesEnAttente} compte les relectures
     *       que l'étudiant <em>doit encore rendre</em>, au sens de Q16, et non les
     *       siennes en attente d'être relues. Les deux se disent « en attente ».</li>
     *   <li><strong>RG24</strong> — {@code moyenne} agrège des notes d'exercice, pas
     *       des notes de relecture.</li>
     *   <li><strong>RG26</strong> — {@code moyenneProvisoire} vaut vrai dès qu'un
     *       exercice a reçu une note mais pas toutes. Un exercice dont aucune
     *       relecture n'est rendue ne compte pas : il ne pèse pas sur la moyenne.</li>
     * </ul>
     */
    @Query(nativeQuery = true, value = """
            SELECT e.id                                                   AS etudiantId,
                   e.nom                                                  AS nom,
                   COALESCE(pr.total, 0)                                  AS presences,
                   COALESCE(ex.total, 0)                                  AS exercicesDeposes,
                   moy.moyenne                                            AS moyenne,
                   COALESCE(due.total, 0)                                 AS relecturesEnAttente,
                   COALESCE(prov.provisoire, FALSE)                       AS moyenneProvisoire
            FROM etudiants e
            LEFT JOIN (SELECT etudiant_id, COUNT(*) AS total
                       FROM presences GROUP BY etudiant_id) pr ON pr.etudiant_id = e.id
            LEFT JOIN (SELECT etudiant_id, COUNT(*) AS total
                       FROM exercices GROUP BY etudiant_id) ex ON ex.etudiant_id = e.id
            LEFT JOIN (SELECT relecteur_id, COUNT(*) AS total
                       FROM relectures WHERE statut = 'ATTRIBUEE'
                       GROUP BY relecteur_id) due ON due.relecteur_id = e.id
            LEFT JOIN (SELECT x.etudiant_id, AVG(par_exercice.note) AS moyenne
                       FROM (SELECT exercice_id, AVG(note) AS note
                             FROM relectures WHERE note IS NOT NULL
                             GROUP BY exercice_id) par_exercice
                       JOIN exercices x ON x.id = par_exercice.exercice_id
                       GROUP BY x.etudiant_id) moy ON moy.etudiant_id = e.id
            LEFT JOIN (SELECT x.etudiant_id, TRUE AS provisoire
                       FROM relectures manquante
                       JOIN exercices x ON x.id = manquante.exercice_id
                       WHERE manquante.note IS NULL
                         AND EXISTS (SELECT 1 FROM relectures rendue
                                     WHERE rendue.exercice_id = manquante.exercice_id
                                       AND rendue.note IS NOT NULL)
                       GROUP BY x.etudiant_id) prov ON prov.etudiant_id = e.id
            WHERE e.promotion_id = :promotionId
            ORDER BY e.nom ASC
            """)
    List<ProjectionLigneTableau> recapitulatifDeLaPromotion(@Param("promotionId") Long promotionId);

    /** Projection des colonnes de la requête native, avant passage au domaine. */
    interface ProjectionLigneTableau {
        Long getEtudiantId();

        String getNom();

        long getPresences();

        long getExercicesDeposes();

        Double getMoyenne();

        long getRelecturesEnAttente();

        boolean getMoyenneProvisoire();
    }
}
