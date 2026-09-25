import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Etat, Requete, Succes, Vide } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';
import type { Exercice, RelectureRecue, SessionResume } from '../api/types';

/**
 * Écran étudiant (contrainte F2) — marquer sa présence, déposer son exercice,
 * remplacer le lien, consulter la note reçue.
 */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

export default function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  return (
    <section>
      <h1>Étudiant</h1>

      <div className="carte">
        <ChoixEtudiant promotionId={PROMOTION} valeur={etudiantId} onChange={setEtudiantId} />
        {etudiantId === null && (
          <Vide>Choisissez votre nom pour marquer votre présence et déposer votre exercice.</Vide>
        )}
      </div>

      {etudiantId !== null && (
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
    <div className="carte">
      <h2>Marquer ma présence</h2>
      <form onSubmit={envoyer}>
        <div className="ligne-champs">
          <div className="champ champ--court" style={{ marginBottom: 0 }}>
            <label htmlFor="code">Code dicté par le formateur</label>
            <input
              id="code" value={code} onChange={(e) => setCode(e.target.value)}
              /* Le code est dicté à l'oral et saisi sur un téléphone : on
                 désactive la correction automatique, qui remplacerait volontiers
                 ABC234. La normalisation reste faite par le serveur. */
              autoCapitalize="characters" autoCorrect="off" spellCheck={false}
              maxLength={6} placeholder="ABC234" required
              style={{ fontFamily: 'ui-monospace, monospace', letterSpacing: '0.12em' }}
            />
          </div>
          <button type="submit" disabled={enCours || code.trim() === ''}>
            {enCours ? 'Envoi…' : 'Je suis présent'}
          </button>
        </div>
      </form>

      {erreur && <Erreur erreur={erreur} />}
      {confirme && <Succes>Présence enregistrée.</Succes>}
    </div>
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
    <div className="carte">
      <h2>Déposer mon exercice</h2>

      <Requete etat={sessions} quoi="des séances">
        {(liste) => {
          const ouvertes = liste.filter((s) => s.statut === 'OUVERTE');
          return ouvertes.length === 0 ? (
            <Vide>Aucune séance n'accepte de dépôt pour l'instant.</Vide>
          ) : (
            <form onSubmit={envoyer}>
              <div className="champ champ--moyen">
                <label htmlFor="session">Séance</label>
                <select id="session" value={sessionId ?? ''}
                        onChange={(e) => setSessionId(e.target.value === '' ? null : Number(e.target.value))}>
                  <option value="">— choisir la séance —</option>
                  {ouvertes.map((session) => (
                    <option key={session.id} value={session.id}>{session.titre}</option>
                  ))}
                </select>
              </div>
              <div className="ligne-champs">
                <div className="champ" style={{ flex: '1 1 22rem', marginBottom: 0 }}>
                  <label htmlFor="lien">Lien de mon exercice</label>
                  <input id="lien" type="url" value={lien} onChange={(e) => setLien(e.target.value)}
                         placeholder="https://github.com/mon-compte/mon-exercice" required />
                </div>
                <button type="submit" disabled={enCours || sessionId === null || lien.trim() === ''}>
                  {enCours ? 'Envoi…' : 'Déposer'}
                </button>
              </div>
            </form>
          );
        }}
      </Requete>

      {erreur && <Erreur erreur={erreur} />}
      {depose && (
        <Succes>
          Exercice n° <strong>{depose.id}</strong> déposé, état {depose.statut}.
          {' '}Notez ce numéro : il vous servira à corriger le lien et à consulter votre note.
        </Succes>
      )}

      <RemplacementDuLien />
    </div>
  );
}

function RemplacementDuLien() {
  const [exerciceId, setExerciceId] = useState('');
  const [lien, setLien] = useState('');
  const [confirme, setConfirme] = useState(false);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer() {
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
      <div className="ligne-champs">
        <div className="champ champ--court" style={{ marginBottom: 0 }}>
          <label htmlFor="exercice-a-corriger">N° d'exercice</label>
          <input id="exercice-a-corriger" type="number" min={1} value={exerciceId}
                 onChange={(e) => setExerciceId(e.target.value)} />
        </div>
        <div className="champ" style={{ flex: '1 1 18rem', marginBottom: 0 }}>
          <label htmlFor="nouveau-lien">Nouveau lien</label>
          <input id="nouveau-lien" type="url" value={lien}
                 onChange={(e) => setLien(e.target.value)} />
        </div>
        <button type="button" className="bouton--discret" onClick={envoyer}
                disabled={enCours || exerciceId === '' || lien.trim() === ''}>
          {enCours ? 'Envoi…' : 'Remplacer'}
        </button>
      </div>

      {erreur && <Erreur erreur={erreur} />}
      {confirme && <Succes>Lien remplacé.</Succes>}
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
    <div className="carte">
      <h2>Ma note</h2>
      <form onSubmit={consulter}>
        <div className="ligne-champs">
          <div className="champ champ--court" style={{ marginBottom: 0 }}>
            <label htmlFor="exercice">N° de mon exercice</label>
            <input id="exercice" type="number" min={1} value={exerciceId}
                   onChange={(e) => setExerciceId(e.target.value)} required />
          </div>
          <button type="submit" disabled={enCours || exerciceId === ''}>
            {enCours ? 'Recherche…' : 'Voir ma note'}
          </button>
        </div>
      </form>

      {erreur && <Erreur erreur={erreur} />}

      {recue && (
        <div role="status">
          {/* RG22 — l'identité des relecteurs n'est ni affichée, ni même reçue :
              le schéma de réponse ne la comporte pas. */}
          {recue.note !== null && recue.note !== undefined ? (
            <>
              <p style={{ fontSize: '1.5rem', fontWeight: 650, margin: 'var(--e3) 0 var(--e1)' }}>
                <span className="nombre">{recue.note}</span> / 20
                {recue.provisoire && <> <Etat variante="provisoire">provisoire</Etat></>}
              </p>
              {recue.provisoire && (
                <p>
                  <small>
                    {recue.relecturesRendues} relecture sur {recue.relecturesAttendues} rendue.
                    {' '}La note changera quand la seconde arrivera.
                  </small>
                </p>
              )}
              <h3>Commentaires reçus</h3>
              <ul className="commentaires">
                {recue.commentaires.map((commentaire, rang) => (
                  // L'index sert de clé faute d'identifiant : en donner un
                  // permettrait de rapprocher un commentaire de son auteur (RG22).
                  <li key={rang}>{commentaire}</li>
                ))}
              </ul>
            </>
          ) : recue.statut === 'NON_ATTRIBUABLE' ? (
            <div className="message message--info">
              <p>
                <Etat variante="impossible">non attribuable</Etat>{' '}
                Aucun relecteur ne pouvait être désigné pour cet exercice.
              </p>
            </div>
          ) : (
            <Vide>Votre exercice attend encore sa relecture.</Vide>
          )}
        </div>
      )}
    </div>
  );
}
