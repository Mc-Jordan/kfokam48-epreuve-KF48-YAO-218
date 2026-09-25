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
 *       {@code p[(k+1) mod n]} <strong>et</strong> à {@code p[(k+2) mod n]}.</li>
 * </ol>
 *
 * <p>Quatre propriétés en découlent, sans qu'aucune vérification ne soit nécessaire,
 * dès que {@code n ≥ 3} :</p>
 * <ul>
 *   <li><strong>RG19</strong> — ni {@code p[k+1]} ni {@code p[k+2]} n'est
 *       {@code p[k]} : personne ne relit son propre exercice ;</li>
 *   <li><strong>RG15</strong> — {@code p[k+1] ≠ p[k+2]} : les deux relecteurs d'un
 *       même exercice sont bien deux personnes différentes ;</li>
 *   <li><strong>RG16</strong> — deux exercices ont deux auteurs distincts (RG7),
 *       donc deux positions distinctes ; un étudiant est le successeur immédiat d'un
 *       auteur et le second successeur d'un autre, jamais davantage : il reçoit
 *       <strong>au plus deux</strong> relectures ;</li>
 *   <li><strong>RG14</strong> — le mélange est aléatoire, et les relecteurs sont
 *       toujours des étudiants présents.</li>
 * </ul>
 *
 * <h2>Quand deux relecteurs sont impossibles</h2>
 *
 * <p>L'attribution se fait <strong>au mieux</strong> (RG17), décision écrite en
 * section 7 du cahier des charges :</p>
 * <ul>
 *   <li>{@code n ≥ 3} — deux relecteurs, la note sera la moyenne des deux ;</li>
 *   <li>{@code n = 2} — un seul relecteur possible, l'autre étudiant. L'exercice est
 *       attribué partiellement et sa note restera provisoire à titre définitif ;</li>
 *   <li>{@code n < 2} — l'unique présent est l'auteur du seul exercice possible
 *       (RG8). L'exercice devient {@code NON_ATTRIBUABLE}.</li>
 * </ul>
 *
 * <p>Refuser d'attribuer sous trois présents aurait été une régression : en
 * {@code v0.1}, une séance à deux étudiants produisait une relecture.</p>
 */
@Component
public class AttributionDesRelectures {

    private final SecureRandom aleatoire = new SecureRandom();

    /** Ce que la clôture a produit : les relectures créées, et ce qui n'a pu l'être. */
    public record Resultat(List<Relecture> attribuees, List<Exercice> nonAttribuables) {
    }

    /** RG15 — nombre de relecteurs visé par exercice, dès que le vivier le permet. */
    public static final int RELECTEURS_PAR_EXERCICE = 2;

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
        int taille = cercle.size();

        // RG17 — au mieux : deux relecteurs dès trois présents, un seul à deux.
        int relecteursVises = Math.min(RELECTEURS_PAR_EXERCICE, taille - 1);

        Map<Long, Integer> positionParEtudiant = new HashMap<>();
        for (int i = 0; i < taille; i++) {
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

            for (int rang = 1; rang <= relecteursVises; rang++) {
                Etudiant relecteur = cercle.get((position + rang) % taille);
                attribuees.add(Relecture.attribuer(exercice, relecteur, maintenant));
            }
        }

        return new Resultat(attribuees, nonAttribuables);
    }
}
