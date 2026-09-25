import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Requete } from '../composants/Etat';
import type { SessionOuverte, SessionResume } from '../api/types';

/**
 * Écran formateur (contrainte F2) — ouvrir une séance, la clôturer, la finaliser.
 *
 * Le tableau récapitulatif vient avec le ticket #9, l'ajout manuel de présence
 * avec le ticket #10.
 */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

export default function EcranFormateur() {
  const sessions = useRequete<SessionResume[]>(() => operations.listerSessions(PROMOTION));

  return (
    <section>
      <h1>Formateur</h1>
      <OuvertureDeSeance onOuverte={sessions.recharger} />
      <h2>Séances de la promotion</h2>
      <Requete etat={sessions} quoi="des séances">
        {(liste) =>
          liste.length === 0 ? (
            <p>Aucune séance pour l'instant.</p>
          ) : (
            <table>
              <caption className="sr-only">Séances, de la plus récente à la plus ancienne</caption>
              <thead>
                <tr>
                  <th scope="col">Titre</th>
                  <th scope="col">État</th>
                  <th scope="col">Ouverte à</th>
                  <th scope="col">Code valable jusqu'à</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              <tbody>
                {liste.map((session) => (
                  <LigneSeance key={session.id} session={session} onChange={sessions.recharger} />
                ))}
              </tbody>
            </table>
          )
        }
      </Requete>
    </section>
  );
}

function OuvertureDeSeance({ onOuverte }: { onOuverte: () => void }) {
  const [titre, setTitre] = useState('');
  const [ouverture, setOuverture] = useState<SessionOuverte | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault();
    setEnCours(true);
    setErreur(null);
    try {
      setOuverture(await operations.ouvrirSession(titre, PROMOTION));
      setTitre('');
      onOuverte();
    } catch (e) {
      setErreur(e as ErreurApi);
      setOuverture(null);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <>
      <form onSubmit={envoyer}>
        <h2>Ouvrir une séance</h2>
        <label htmlFor="titre">Titre</label>{' '}
        <input id="titre" value={titre} onChange={(e) => setTitre(e.target.value)}
               placeholder="Algorithmique" required />{' '}
        <button type="submit" disabled={enCours || titre.trim() === ''}>
          {enCours ? 'Ouverture…' : 'Ouvrir'}
        </button>
      </form>

      {erreur && <Erreur erreur={erreur} />}

      {ouverture && (
        <div role="status">
          <h3>Code de présence</h3>
          {/* Le code se dicte à voix haute : il doit être lisible de loin. */}
          <p style={{ fontSize: '2.5rem', fontFamily: 'monospace', letterSpacing: '0.2em' }}>
            {ouverture.code}
          </p>
          <p>
            Valable jusqu'à {new Date(ouverture.expirationAt).toLocaleTimeString('fr-FR')}
            {' '}— quinze minutes après l'ouverture.
          </p>
        </div>
      )}
    </>
  );
}

function LigneSeance({ session, onChange }: { session: SessionResume; onChange: () => void }) {
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function cloturer() {
    setEnCours(true);
    setErreur(null);
    try {
      const resultat = await operations.cloturerSession(session.id);
      // Le formateur doit savoir tout de suite que certains exercices ne seront
      // jamais relus : le découvrir plus tard au tableau serait trop tard (RG17).
      setMessage(
        `${resultat.exercicesAttribues} exercice(s) attribué(s)` +
        (resultat.exercicesNonAttribuables > 0
          ? `, ${resultat.exercicesNonAttribuables} sans relecteur possible`
          : ''),
      );
      onChange();
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  async function finaliser() {
    setEnCours(true);
    setErreur(null);
    try {
      const resultat = await operations.finaliserSession(session.id);
      setMessage(`${resultat.relecturesFigees} relecture(s) figée(s) définitivement.`);
      onChange();
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <>
      <tr>
        <td>{session.titre}</td>
        <td>{session.statut}</td>
        <td>{new Date(session.ouvertureAt).toLocaleTimeString('fr-FR')}</td>
        <td>{new Date(session.expirationAt).toLocaleTimeString('fr-FR')}</td>
        <td>
          {/* L'action proposée découle du statut renvoyé par l'API, jamais d'une
              date recalculée ici : la clôture est une décision, pas un délai (F3). */}
          {session.statut === 'OUVERTE' && (
            <button type="button" onClick={cloturer} disabled={enCours}>
              Clôturer et attribuer
            </button>
          )}
          {session.statut === 'CLOTUREE' && (
            <button type="button" onClick={finaliser} disabled={enCours}>
              Finaliser et figer
            </button>
          )}
          {session.statut === 'FINALISEE' && <span>Terminée</span>}
        </td>
      </tr>
      {(message || erreur) && (
        <tr>
          <td colSpan={5}>
            {message && <span role="status">{message}</span>}
            {erreur && <Erreur erreur={erreur} />}
          </td>
        </tr>
      )}
    </>
  );
}
