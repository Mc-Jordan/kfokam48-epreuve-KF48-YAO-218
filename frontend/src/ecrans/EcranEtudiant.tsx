import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Requete } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';
import type { Exercice, RelectureRecue, SessionResume } from '../api/types';

/**
 * Écran étudiant (contrainte F2) — marquer sa présence, déposer son exercice,
 * consulter la note reçue.
 *
 * Le remplacement du lien vient avec le ticket #11.
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
          <MaNote />
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
  const [depose, setDepose] = useState<Exercice | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
    if (sessionId === null) return;
    setEnCours(true);
    setErreur(null);
    setDepose(null);
    try {
      setDepose(await operations.deposerExercice(sessionId, etudiantId, lien));
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
      {depose && (
        <p role="status">
          Exercice n° {depose.id} déposé, état {depose.statut}.
          {' '}Notez ce numéro : il vous servira à consulter votre note.
        </p>
      )}

      <RemplacementDuLien />
    </form>
  );
}

function RemplacementDuLien() {
  const [exerciceId, setExerciceId] = useState('');
  const [lien, setLien] = useState('');
  const [confirme, setConfirme] = useState(false);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
    setEnCours(true);
    setErreur(null);
    setConfirme(false);
    try {
      await operations.remplacerLienExercice(Number(exerciceId), lien);
      setConfirme(true);
      setLien('');
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <fieldset>
      <legend>Je me suis trompé de lien</legend>
      {/* RG10 — possible tant que la séance est ouverte. Le serveur tranche :
          on ne devine pas ici si la clôture a eu lieu. */}
      <label htmlFor="exercice-a-corriger">Numéro de l'exercice</label>{' '}
      <input id="exercice-a-corriger" type="number" min={1} value={exerciceId}
             onChange={(e) => setExerciceId(e.target.value)} />{' '}
      <label htmlFor="nouveau-lien">Nouveau lien</label>{' '}
      <input id="nouveau-lien" type="url" value={lien} size={36}
             onChange={(e) => setLien(e.target.value)} />{' '}
      <button type="button" onClick={envoyer}
              disabled={enCours || exerciceId === '' || lien.trim() === ''}>
        {enCours ? 'Envoi…' : 'Remplacer'}
      </button>

      {erreur && <Erreur erreur={erreur} />}
      {confirme && <p role="status">Lien remplacé.</p>}
    </fieldset>
  );
}

function MaNote() {
  const [exerciceId, setExerciceId] = useState('');
  const [recue, setRecue] = useState<RelectureRecue | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function consulter(evenement: React.FormEvent) {
    evenement.preventDefault();
    setEnCours(true);
    setErreur(null);
    setRecue(null);
    try {
      setRecue(await operations.consulterRelectureRecue(Number(exerciceId)));
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <form onSubmit={consulter}>
      <h2>Ma note</h2>
      <label htmlFor="exercice">Numéro de mon exercice</label>{' '}
      <input id="exercice" type="number" min={1} value={exerciceId}
             onChange={(e) => setExerciceId(e.target.value)} required />{' '}
      <button type="submit" disabled={enCours || exerciceId === ''}>
        {enCours ? 'Recherche…' : 'Voir'}
      </button>

      {erreur && <Erreur erreur={erreur} />}

      {recue && (
        <div role="status">
          {/* RG22 — l'identité du relecteur n'est ni affichée, ni même reçue :
              le schéma de réponse ne la comporte pas. */}
          {recue.note !== null && recue.note !== undefined ? (
            <>
              <p>
                <strong>Note : {recue.note} / 20</strong>
                {/* RG26 — le client a demandé explicitement que la mention figure.
                    Une note provisoire affichée comme définitive serait pire que
                    pas de note du tout : l'étudiant croirait la relecture finie. */}
                {recue.provisoire && <em> — provisoire</em>}
              </p>
              {recue.provisoire && (
                <p>
                  <small>
                    {recue.relecturesRendues} relecture sur {recue.relecturesAttendues} rendue.
                    {' '}La note changera quand la seconde arrivera.
                  </small>
                </p>
              )}
              <ul>
                {recue.commentaires.map((commentaire, rang) => (
                  // L'index sert de clé faute d'identifiant : en donner un
                  // permettrait de rapprocher un commentaire de son auteur (RG22).
                  <li key={rang}>{commentaire}</li>
                ))}
              </ul>
            </>
          ) : recue.statut === 'NON_ATTRIBUABLE' ? (
            <p>Aucun relecteur ne pouvait être désigné pour cet exercice.</p>
          ) : (
            <p>Votre exercice attend encore sa relecture.</p>
          )}
        </div>
      )}
    </form>
  );
}
