import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Requete } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';
import type { RelectureAssignee } from '../api/types';

/** Écran relecteur (contrainte F2) — voir ce qu'on doit relire, et le relire. */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

export default function EcranRelecteur() {
  const [relecteurId, setRelecteurId] = useState<number | null>(null);

  return (
    <section>
      <h1>Relecteur</h1>
      <p><ChoixEtudiant promotionId={PROMOTION} valeur={relecteurId} onChange={setRelecteurId} /></p>

      {relecteurId === null
        ? <p>Choisissez votre nom pour voir ce qui vous est attribué.</p>
        : <MesRelectures relecteurId={relecteurId} />}
    </section>
  );
}

function MesRelectures({ relecteurId }: { relecteurId: number }) {
  const relectures = useRequete<RelectureAssignee[]>(
    () => operations.listerRelecturesDuRelecteur(relecteurId),
    [relecteurId],
  );

  return (
    <Requete etat={relectures} quoi="de vos relectures">
      {(liste) =>
        liste.length === 0 ? (
          // Une liste vide est un état normal, pas une anomalie : les relecteurs
          // ne sont tirés qu'à la clôture de la séance (RG13).
          <p>Rien à relire pour l'instant. Les relectures sont attribuées à la clôture d'une séance.</p>
        ) : (
          <ul>
            {liste.map((relecture) => (
              <li key={relecture.relectureId}>
                <Formulaire relecture={relecture} onRendue={relectures.recharger} />
              </li>
            ))}
          </ul>
        )
      }
    </Requete>
  );
}

function Formulaire({ relecture, onRendue }: { relecture: RelectureAssignee; onRendue: () => void }) {
  const [note, setNote] = useState(relecture.note?.toString() ?? '');
  const [commentaire, setCommentaire] = useState(relecture.commentaire ?? '');
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  const figee = relecture.statut === 'FIGEE';

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
    setEnCours(true);
    setErreur(null);
    try {
      await operations.rendreRelecture(relecture.relectureId, Number(note), commentaire);
      onRendue();
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <form onSubmit={envoyer}>
      <h3>{relecture.sessionTitre}</h3>
      <p>
        {/* L'exercice s'ouvre ailleurs : on ne quitte pas la page de relecture en
            cours de saisie. rel="noreferrer" — le lien vient d'un pair, pas de nous. */}
        <a href={relecture.lien} target="_blank" rel="noreferrer">Ouvrir l'exercice</a>
      </p>

      <p>
        <label htmlFor={`note-${relecture.relectureId}`}>Note sur 20</label>{' '}
        <input
          id={`note-${relecture.relectureId}`}
          type="number"
          min={0}
          max={20}
          step={1}
          value={note}
          onChange={(e) => setNote(e.target.value)}
          disabled={figee}
          required
        />
      </p>

      <p>
        <label htmlFor={`commentaire-${relecture.relectureId}`}>Commentaire</label><br />
        <textarea
          id={`commentaire-${relecture.relectureId}`}
          value={commentaire}
          onChange={(e) => setCommentaire(e.target.value)}
          rows={3}
          cols={50}
          disabled={figee}
          required
        />
      </p>

      {figee ? (
        <p role="status">Relecture figée : la séance est finalisée.</p>
      ) : (
        <button type="submit" disabled={enCours || note === '' || commentaire.trim() === ''}>
          {/* RG20 — tant que la séance n'est pas finalisée, la note reste corrigible. */}
          {enCours ? 'Envoi…' : relecture.statut === 'RENDUE' ? 'Corriger ma relecture' : 'Rendre ma relecture'}
        </button>
      )}

      {erreur && <Erreur erreur={erreur} />}
    </form>
  );
}
