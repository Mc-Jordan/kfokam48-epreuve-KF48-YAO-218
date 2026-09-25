import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Etat, Requete, Vide } from '../composants/Etat';
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

      <div className="carte">
        <ChoixEtudiant promotionId={PROMOTION} valeur={relecteurId} onChange={setRelecteurId} />
        {relecteurId === null && (
          <Vide>Choisissez votre nom pour voir ce qui vous est attribué.</Vide>
        )}
      </div>

      {relecteurId !== null && <MesRelectures relecteurId={relecteurId} />}
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
          <div className="carte">
            <Vide>
              Rien à relire pour l'instant. Les relectures sont attribuées à la
              clôture d'une séance par le formateur.
            </Vide>
          </div>
        ) : (
          <ul className="liste-nue">
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
    <form className="carte" onSubmit={envoyer}>
      <h2>
        {relecture.sessionTitre}{' '}
        {figee
          ? <Etat variante="finalise">figée</Etat>
          : relecture.statut === 'RENDUE'
            ? <Etat variante="ouvert">rendue</Etat>
            : <Etat variante="cloture">à rendre</Etat>}
      </h2>

      <p>
        {/* L'exercice s'ouvre ailleurs : on ne quitte pas la page de relecture en
            cours de saisie. rel="noreferrer" — le lien vient d'un pair, pas de nous. */}
        <a href={relecture.lien} target="_blank" rel="noreferrer">
          Ouvrir l'exercice dans un nouvel onglet
        </a>
      </p>

      <div className="champ champ--court">
        <label htmlFor={`note-${relecture.relectureId}`}>Note sur 20</label>
        <input id={`note-${relecture.relectureId}`} type="number" min={0} max={20} step={1}
               value={note} onChange={(e) => setNote(e.target.value)}
               disabled={figee} required className="nombre" />
      </div>

      <div className="champ">
        <label htmlFor={`commentaire-${relecture.relectureId}`}>Commentaire</label>
        <textarea id={`commentaire-${relecture.relectureId}`} value={commentaire}
                  onChange={(e) => setCommentaire(e.target.value)}
                  disabled={figee} required rows={3}
                  placeholder="Ce qui fonctionne, ce qui ne fonctionne pas, ce que vous feriez autrement." />
      </div>

      {figee ? (
        <p className="message message--info" role="status">
          <span>La séance est finalisée : cette relecture ne peut plus être modifiée.</span>
        </p>
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
