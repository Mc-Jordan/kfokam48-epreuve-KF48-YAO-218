package cm.kfokam48.presences.etudiant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    /** Q1 — l'étudiant choisit son nom dans la liste de sa promotion. */
    List<Etudiant> findByPromotionIdOrderByNomAsc(Long promotionId);
}
