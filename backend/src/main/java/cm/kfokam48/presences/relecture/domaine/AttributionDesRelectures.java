package cm.kfokam48.presences.relecture.domaine;

import cm.kfokam48.presences.etudiant.Etudiant;
import cm.kfokam48.presences.exercice.domaine.Exercice;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Le tirage au sort des relecteurs, effectué une fois, à la clôture (RG13).
 *
 * <h2>L'algorithme, et pourquoi il ne peut pas échouer</h2>
 *
 * <p>Une affectation gloutonne — « pour chaque exercice, prendre au hasard un
 * relecteur encore libre qui ne soit pas l'auteur » — se bloque : le dernier
 * exercice peut n'avoir que son propre auteur comme candidat restant. Il faudrait
 * alors détecter l'impasse et recommencer, sans garantie de terminaison.</p>
 *
 * <p>On procède autrement, par <strong>rotation circulaire</strong> :</p>
 * <ol>
 *   <li>les étudiants présents sont mélangés en une liste circulaire
 *       {@code p[0..n-1]} ;</li>
 *   <li>l'exercice dont l'auteur occupe la position {@code k} est confié à
 *       {@code p[(k+1) mod n]}.</li>
 * </ol>
 *
 * <p>Trois propriétés en découlent, sans qu'aucune vérification ne soit nécessaire :</p>
 * <ul>
 *   <li><strong>RG19</strong> — {@code p[k+1] ≠ p[k]} dès que {@code n ≥ 2} :
 *       personne ne relit son propre exercice ;</li>
 *   <li><strong>RG16</strong> — deux exercices ont deux auteurs distincts (RG7),
 *       donc deux positions {@code k} distinctes, donc deux successeurs distincts :
 *       aucun étudiant ne reçoit deux relectures ;</li>
 *   <li><strong>RG14</strong> — le mélange est aléatoire, et le relecteur est
 *       toujours un étudiant présent.</li>
 * </ul>
 *
 * <p>Le seul cas où l'attribution est impossible est {@code n < 2}, c'est-à-dire un
 * unique étudiant présent : il est alors l'auteur du seul exercice possible (RG8).
 * L'exercice devient {@code NON_ATTRIBUABLE} (RG17).</p>
 */
@Component
public class AttributionDesRelectures {

    private final SecureRandom aleatoire = new SecureRandom();

    /** Ce que la clôture a produit : les relectures créées, et ce qui n'a pu l'être. */
    public record Resultat(List<Relecture> attribuees, List<Exercice> nonAttribuables) {
    }

    public Resultat attribuer(List<Exercice> exercices, List<Etudiant> presents, Instant maintenant) {
        if (exercices.isEmpty()) {
            return new Resultat(List.of(), List.of());
        }

        // Moins de deux présents : le seul candidat serait l'auteur lui-même.
        if (presents.size() < 2) {
            return new Resultat(List.of(), List.copyOf(exercices));
        }

        List<Etudiant> cercle = new ArrayList<>(presents);
        Collections.shuffle(cercle, aleatoire);

        Map<Long, Integer> positionParEtudiant = new HashMap<>();
        for (int i = 0; i < cercle.size(); i++) {
            positionParEtudiant.put(cercle.get(i).getId(), i);
        }

        List<Relecture> attribuees = new ArrayList<>();
        List<Exercice> nonAttribuables = new ArrayList<>();

        for (Exercice exercice : exercices) {
            Integer position = positionParEtudiant.get(exercice.getEtudiant().getId());

            // RG8 garantit que l'auteur est présent. Si la donnée dit le contraire,
            // on ne devine pas : l'exercice est signalé plutôt qu'attribué au hasard.
            if (position == null) {
                nonAttribuables.add(exercice);
                continue;
            }

            Etudiant relecteur = cercle.get((position + 1) % cercle.size());
            attribuees.add(Relecture.attribuer(exercice, relecteur, maintenant));
        }

        return new Resultat(attribuees, nonAttribuables);
    }
}
