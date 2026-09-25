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
     * <p>Chaque agrégat vit dans sa propre sous-requête corrélée plutôt que dans
     * des jointures cumulées : trois {@code LEFT JOIN} sur présences, exercices et
     * relectures produiraient un produit cartésien, et les comptes seraient faux —
     * un étudiant présent à trois séances et ayant déposé deux exercices
     * compterait six présences. C'est l'erreur classique de ce genre de tableau,
     * et elle ne se voit qu'avec des données réalistes.</p>
     *
     * <p>Les règles portées :</p>
     * <ul>
     *   <li><strong>RG24</strong> — la moyenne ne porte que sur les relectures
     *       <em>rendues</em> ou <em>figées</em> concernant les exercices de
     *       l'étudiant, et vaut {@code null} s'il n'en a aucune ;</li>
     *   <li><strong>RG23</strong> — {@code relecturesEnAttente} compte les
     *       relectures que l'étudiant <em>doit encore rendre</em>, au sens de Q16,
     *       et non les siennes en attente d'être relues. Les deux se disent
     *       « en attente » et désignent des choses différentes.</li>
     * </ul>
     */
    @Query("""
            SELECT new cm.kfokam48.presences.tableau.domaine.LigneTableau(
                e.id,
                e.nom,
                (SELECT COUNT(p) FROM Presence p WHERE p.etudiant = e),
                (SELECT COUNT(x) FROM Exercice x WHERE x.etudiant = e),
                (SELECT AVG(CAST(r.note AS double)) FROM Relecture r
                     WHERE r.exercice.etudiant = e AND r.note IS NOT NULL),
                (SELECT COUNT(r2) FROM Relecture r2
                     WHERE r2.relecteur = e AND r2.statut = cm.kfokam48.presences.relecture.domaine.StatutRelecture.ATTRIBUEE)
            )
            FROM Etudiant e
            WHERE e.promotion.id = :promotionId
            ORDER BY e.nom ASC
            """)
    List<LigneTableau> recapitulatifDeLaPromotion(@Param("promotionId") Long promotionId);
}
