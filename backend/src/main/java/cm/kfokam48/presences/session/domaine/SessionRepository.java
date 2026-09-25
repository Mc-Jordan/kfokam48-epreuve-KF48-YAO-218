package cm.kfokam48.presences.session.domaine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /** RG2 — le code identifie la session à lui seul. */
    Optional<Session> findByCode(String code);

    boolean existsByCode(String code);

    List<Session> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);
}
