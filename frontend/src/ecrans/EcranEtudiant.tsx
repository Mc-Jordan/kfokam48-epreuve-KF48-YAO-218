import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Requete } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';
import type { SessionResume } from '../api/types';

/**
 * Écran étudiant (contrainte F2) — marquer sa présence et déposer son exercice.
 *
 * Le remplacement du lien et la consultation de sa note viennent avec les
 * tickets #11 et #13.
 */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

export default function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  return (
    <section>
      <h1>Étudiant</h1>
      <p><ChoixEtudiant promotionId={PROMOTION} valeur={etudiantId} onChange={setEtudiantId} /></p>

      {etudiantId === null ? (
        <p>Choisissez votre nom pour continuer.</p>
      ) : (
        <>
          <MarquageDePresence etudiantId={etudiantId} />
          <DepotDExercice etudiantId={etudiantId} />
        </>
      )}
    </section>
  );
}

function MarquageDePresence({ etudiantId }: { etudiantId: number }) {
  const [code, setCode] = useState('');
  const [confirme, setConfirme] = useState(false);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
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
    <form onSubmit={envoyer}>
      <h2>Marquer ma présence</h2>
      <label htmlFor="code">Code de présence</label>{' '}
      <input
        id="code"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        /* Le code est dicté à l'oral et saisi sur un téléphone : on désactive la
           correction automatique, qui remplacerait volontiers ABC234. La
           normalisation reste faite par le serveur. */
        autoCapitalize="characters"
        autoCorrect="off"
        spellCheck={false}
        maxLength={6}
        placeholder="ABC234"
        required
      />{' '}
      <button type="submit" disabled={enCours || code.trim() === ''}>
        {enCours ? 'Envoi…' : 'Je suis présent'}
      </button>

      {erreur && <Erreur erreur={erreur} />}
      {confirme && <p role="status">Présence enregistrée.</p>}
    </form>
  );
}

function DepotDExercice({ etudiantId }: { etudiantId: number }) {
  const sessions = useRequete<SessionResume[]>(() => operations.listerSessions(PROMOTION));
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [lien, setLien] = useState('');
  const [statut, setStatut] = useState<string | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
    if (sessionId === null) return;
    setEnCours(true);
    setErreur(null);
    setStatut(null);
    try {
      const exercice = await operations.deposerExercice(sessionId, etudiantId, lien);
      setStatut(exercice.statut);
      setLien('');
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <form onSubmit={envoyer}>
      <h2>Déposer mon exercice</h2>

      <Requete etat={sessions} quoi="des sessions">
        {(liste) => {
          const ouvertes = liste.filter((s) => s.statut === 'OUVERTE');
          return ouvertes.length === 0 ? (
            <p>Aucune session n'accepte de dépôt pour l'instant.</p>
          ) : (
            <>
              <p>
                <label htmlFor="session">Séance</label>{' '}
                <select
                  id="session"
                  value={sessionId ?? ''}
                  onChange={(e) => setSessionId(e.target.value === '' ? null : Number(e.target.value))}
                >
                  <option value="">— choisir la séance —</option>
                  {ouvertes.map((session) => (
                    <option key={session.id} value={session.id}>{session.titre}</option>
                  ))}
                </select>
              </p>
              <p>
                <label htmlFor="lien">Lien de mon exercice</label>{' '}
                <input
                  id="lien"
                  type="url"
                  value={lien}
                  onChange={(e) => setLien(e.target.value)}
                  placeholder="https://github.com/mon-compte/mon-exercice"
                  size={40}
                  required
                />{' '}
                <button type="submit" disabled={enCours || sessionId === null || lien.trim() === ''}>
                  {enCours ? 'Envoi…' : 'Déposer'}
                </button>
              </p>
            </>
          );
        }}
      </Requete>

      {erreur && <Erreur erreur={erreur} />}
      {statut && <p role="status">Exercice déposé. État : {statut}.</p>}
    </form>
  );
}
