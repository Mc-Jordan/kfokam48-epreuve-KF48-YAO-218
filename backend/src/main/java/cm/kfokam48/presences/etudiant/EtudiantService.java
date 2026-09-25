package cm.kfokam48.presences.etudiant;

import cm.kfokam48.presences.partage.erreur.Erreurs;
import cm.kfokam48.presences.promotion.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EtudiantService {

    private final EtudiantRepository etudiants;
    private final PromotionRepository promotions;

    public EtudiantService(EtudiantRepository etudiants, PromotionRepository promotions) {
        this.etudiants = etudiants;
        this.promotions = promotions;
    }

    /** EF2 — la liste dans laquelle l'étudiant choisit son nom, puisqu'il n'y a pas d'authentification (Q1). */
    @Transactional(readOnly = true)
    public List<Etudiant> listerParPromotion(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw Erreurs.promotionInconnue();
        }
        return etudiants.findByPromotionIdOrderByNomAsc(promotionId);
    }

    @Transactional(readOnly = true)
    public Etudiant parIdentifiant(Long etudiantId) {
        return etudiants.findById(etudiantId).orElseThrow(Erreurs::etudiantInconnu);
    }
}
