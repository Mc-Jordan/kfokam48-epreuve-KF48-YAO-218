package cm.kfokam48.presences.etudiant.api.dto;

import cm.kfokam48.presences.etudiant.Etudiant;

/**
 * Élément de {@code GET /api/promotions/{id}/etudiants}.
 *
 * <p>Deux champs, comme le contrat l'impose. La promotion n'y figure pas : elle est
 * dans le chemin, la répéter n'apprendrait rien.</p>
 */
public record EtudiantReponse(Long id, String nom) {

    public static EtudiantReponse de(Etudiant etudiant) {
        return new EtudiantReponse(etudiant.getId(), etudiant.getNom());
    }
}
