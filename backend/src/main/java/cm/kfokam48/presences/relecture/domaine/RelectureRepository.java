package cm.kfokam48.presences.relecture.domaine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    List<Relecture> findBySessionId(Long sessionId);

    Optional<Relecture> findByExerciceId(Long exerciceId);

    /**
     * Les relectures d'un relecteur, <strong>avec</strong> leur exercice et leur
     * séance.
     *
     * <p>Le {@code JOIN FETCH} n'est pas une optimisation : le DTO lit le lien de
     * l'exercice et le titre de la séance, tous deux chargés paresseusement, et la
     * conversion a lieu dans le contrôleur — hors transaction, {@code open-in-view}
     * étant désactivé. Sans ces jointures, la réponse est un 500.</p>
     */
    @Query("""
            SELECT r FROM Relecture r
                JOIN FETCH r.exercice
                JOIN FETCH r.session
            WHERE r.relecteur.id = :relecteurId
            ORDER BY r.attribueeAt DESC
            """)
    List<Relecture> findAvecExerciceEtSessionParRelecteur(@Param("relecteurId") Long relecteurId);
}
