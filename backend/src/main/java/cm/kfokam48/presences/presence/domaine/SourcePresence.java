package cm.kfokam48.presences.presence.domaine;

/**
 * D'où vient une présence (RG4).
 *
 * <p>Le contrat impose ce champ dans la réponse de {@code POST /api/presences} et
 * renvoie à Q14 pour l'expliquer : le formateur peut ajouter une présence à la
 * main, et cet ajout doit se voir.</p>
 */
public enum SourcePresence {

    /** L'étudiant a saisi le code lui-même. */
    ETUDIANT,

    /** Le formateur l'a enregistrée, sans code — Q14, RG5. */
    FORMATEUR
}
