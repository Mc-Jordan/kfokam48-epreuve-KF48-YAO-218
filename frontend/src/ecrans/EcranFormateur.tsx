import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Requete } from '../composants/Etat';
import type { SessionOuverte, SessionResume } from '../api/types';

/**
 * Écran formateur (contrainte F2) — ouvrir une session et suivre celles en cours.
 *
 * La clôture, la finalisation et le tableau viennent avec les tickets #6, #9 et #12.
 */

/** Le choix de la promotion est un écran d'administration exclu du périmètre (§3). */
const PROMOTION = 1;

export default function EcranFormateur() {
  const sessions = useRequete<SessionResume[]>(() => operations.listerSessions(PROMOTION));
  const [titre, setTitre] = useState('');
  const [ouverture, setOuverture] = useState<SessionOuverte | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function ouvrirSession(evenement: React.FormEvent) {
    evenement.preventDefault();
    setEnCours(true);
    setErreur(null);
    try {
      setOuverture(await operations.ouvrirSession(titre, PROMOTION));
      setTitre('');
      sessions.recharger();
    } catch (e) {
      setErreur(e as ErreurApi);
      setOuverture(null);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <section>
      <h1>Formateur</h1>

      <form onSubmit={ouvrirSession}>
        <h2>Ouvrir une session</h2>
        <label htmlFor="titre">Titre de la séance</label>{' '}
        <input
          id="titre"
          value={titre}
          onChange={(e) => setTitre(e.target.value)}
          placeholder="Algorithmique"
          required
        />{' '}
        <button type="submit" disabled={enCours || titre.trim() === ''}>
          {enCours ? 'Ouverture…' : 'Ouvrir'}
        </button>
      </form>

      {erreur && <Erreur erreur={erreur} />}

      {ouverture && (
        <div role="status">
          <h2>Code de présence</h2>
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

      <h2>Sessions de la promotion</h2>
      <Requete etat={sessions} quoi="des sessions">
        {(liste) =>
          liste.length === 0 ? (
            <p>Aucune session ouverte pour l'instant.</p>
          ) : (
            <table>
              <caption className="sr-only">Sessions, de la plus récente à la plus ancienne</caption>
              <thead>
                <tr>
                  <th scope="col">Titre</th>
                  <th scope="col">État</th>
                  <th scope="col">Ouverte à</th>
                  <th scope="col">Code valable jusqu'à</th>
                </tr>
              </thead>
              <tbody>
                {liste.map((session) => (
                  <tr key={session.id}>
                    <td>{session.titre}</td>
                    <td>{session.statut}</td>
                    <td>{new Date(session.ouvertureAt).toLocaleTimeString('fr-FR')}</td>
                    <td>{new Date(session.expirationAt).toLocaleTimeString('fr-FR')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        }
      </Requete>
    </section>
  );
}
