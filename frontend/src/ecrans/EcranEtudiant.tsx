import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { Erreur } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';

/**
 * Écran étudiant (contrainte F2) — marquer sa présence.
 *
 * Le dépôt d'exercice et la consultation de sa note viennent avec les
 * tickets #5, #11 et #13.
 */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

export default function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [code, setCode] = useState('');
  const [confirme, setConfirme] = useState(false);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function marquerPresence(evenement: React.FormEvent) {
    evenement.preventDefault();
    if (etudiantId === null) return;
    setEnCours(true);
    setErreur(null);
    setConfirme(false);
    try {
      await operations.marquerPresence(code, etudiantId);
      setConfirme(true);
      setCode('');
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <section>
      <h1>Étudiant</h1>

      <form onSubmit={marquerPresence}>
        <h2>Marquer ma présence</h2>

        <p><ChoixEtudiant promotionId={PROMOTION} valeur={etudiantId} onChange={setEtudiantId} /></p>

        <p>
          <label htmlFor="code">Code de présence</label>{' '}
          <input
            id="code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            /* Le code est dicté à l'oral et saisi sur un téléphone : on évite la
               correction automatique et la majuscule initiale, et on annonce un
               clavier de caractères. La normalisation reste faite par le serveur. */
            autoCapitalize="characters"
            autoCorrect="off"
            spellCheck={false}
            inputMode="text"
            maxLength={6}
            placeholder="ABC234"
            required
          />{' '}
          <button type="submit" disabled={enCours || etudiantId === null || code.trim() === ''}>
            {enCours ? 'Envoi…' : 'Je suis présent'}
          </button>
        </p>
      </form>

      {erreur && <Erreur erreur={erreur} />}

      {confirme && (
        <p role="status">Présence enregistrée.</p>
      )}
    </section>
  );
}
