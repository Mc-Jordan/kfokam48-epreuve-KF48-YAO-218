import { operations } from '../api/operations';
import { useRequete } from '../api/useRequete';
import { Requete } from './Etat';
import type { Etudiant } from '../api/types';

/**
 * Sélecteur d'identité, partagé par les écrans étudiant et relecteur.
 *
 * Il n'y a pas d'authentification (Q1) : l'étudiant se désigne en choisissant
 * son nom. La conséquence est assumée et écrite au cahier des charges — n'importe
 * qui peut se faire passer pour n'importe qui.
 */
export function ChoixEtudiant({
  promotionId, valeur, onChange,
}: {
  promotionId: number;
  valeur: number | null;
  onChange: (etudiantId: number | null) => void;
}) {
  const etudiants = useRequete<Etudiant[]>(
    () => operations.listerEtudiantsDeLaPromotion(promotionId),
    [promotionId],
  );

  return (
    <Requete etat={etudiants} quoi="de la liste des étudiants">
      {(liste) => (
        <div className="champ champ--moyen">
          <label htmlFor="etudiant">Je suis</label>
          <select
            id="etudiant"
            value={valeur ?? ''}
            onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
          >
            <option value="">— choisir mon nom —</option>
            {liste.map((etudiant) => (
              <option key={etudiant.id} value={etudiant.id}>{etudiant.nom}</option>
            ))}
          </select>
        </div>
      )}
    </Requete>
  );
}
