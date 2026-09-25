package cm.kfokam48.presences.exercice.api.dto;

import cm.kfokam48.presences.exercice.domaine.Exercice;
import cm.kfokam48.presences.exercice.domaine.StatutExercice;

/** Réponse {@code 201} de {@code POST /api/exercices} : les deux champs du contrat. */
public record ExerciceReponse(Long id, StatutExercice statut) {

    public static ExerciceReponse de(Exercice exercice) {
        return new ExerciceReponse(exercice.getId(), exercice.getStatut());
    }
}
