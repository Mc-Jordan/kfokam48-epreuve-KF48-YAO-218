package cm.kfokam48.presences.tableau.domaine;

import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.promotion.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableauService {

    private final TableauRepository tableau;
    private final PromotionRepository promotions;

    public TableauService(TableauRepository tableau, PromotionRepository promotions) {
        this.tableau = tableau;
        this.promotions = promotions;
    }

    /** EF12 — le récapitulatif par étudiant d'une promotion. */
    @Transactional(readOnly = true)
    public List<LigneTableau> recapitulatif(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw Erreurs.promotionInconnue();
        }
        return tableau.recapitulatifDeLaPromotion(promotionId);
    }
}
