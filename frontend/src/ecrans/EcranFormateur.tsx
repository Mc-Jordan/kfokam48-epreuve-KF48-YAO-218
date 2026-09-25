import { useState } from 'react';
import { operations } from '../api/operations';
import { ErreurApi } from '../api/client';
import { useRequete } from '../api/useRequete';
import { Erreur, Etat, Requete, Succes, Vide } from '../composants/Etat';
import { ChoixEtudiant } from '../composants/ChoixEtudiant';
import { Revelation } from '../composants/Revelation';
import type { LigneTableau, PresenceDetail, SessionOuverte, SessionResume, StatutSession }
  from '../api/types';

/**
 * Écran formateur (contrainte F2) — ouvrir une séance, ajouter une présence à la
 * main, clôturer, finaliser, et suivre la promotion.
 */

/** Le choix de la promotion relève d'un écran d'administration, hors périmètre (§3). */
const PROMOTION = 1;

const VARIANTE_STATUT = {
  OUVERTE: 'ouvert',
  CLOTUREE: 'cloture',
  FINALISEE: 'finalise',
} as const;

export default function EcranFormateur() {
  const sessions = useRequete<SessionResume[]>(() => operations.listerSessions(PROMOTION));

  return (
    <section>
      <h1>Formateur</h1>
      <OuvertureDeSeance onOuverte={sessions.recharger} />

      <Revelation>
      <div className="carte">
        <h2>Séances de la promotion</h2>
        <Requete etat={sessions} quoi="des séances">
          {(liste) =>
            liste.length === 0 ? (
              <Vide>Aucune séance pour l'instant. Ouvrez-en une ci-dessus.</Vide>
            ) : (
              <div className="tableau-defilant">
                <table>
                  <caption className="sr-only">Séances, de la plus récente à la plus ancienne</caption>
                  <thead>
                    <tr>
                      <th scope="col">Titre</th>
                      <th scope="col">État</th>
                      <th scope="col">Ouverte à</th>
                      <th scope="col">Code jusqu'à</th>
                      <th scope="col">Action</th>
                      <th scope="col">Présences</th>
                    </tr>
                  </thead>
                  <tbody>
                    {liste.map((session) => (
                      <LigneSeance key={session.id} session={session} onChange={sessions.recharger} />
                    ))}
                  </tbody>
                </table>
              </div>
            )
          }
        </Requete>
      </div>
      </Revelation>

      <Revelation retard={90}><TableauRecapitulatif /></Revelation>
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
    <div className="carte">
      <h2>Ouvrir une séance</h2>
      <form onSubmit={envoyer}>
        <div className="ligne-champs">
          <div className="champ champ--moyen" style={{ flex: '1 1 18rem', marginBottom: 0 }}>
            <label htmlFor="titre">Titre de la séance</label>
            <input id="titre" value={titre} onChange={(e) => setTitre(e.target.value)}
                   placeholder="Algorithmique — tris et complexité" required />
          </div>
          <button type="submit" disabled={enCours || titre.trim() === ''}>
            {enCours ? 'Ouverture…' : 'Ouvrir la séance'}
          </button>
        </div>
      </form>

      {erreur && <Erreur erreur={erreur} />}

      {ouverture && (
        <div role="status">
          <h3>Code de présence</h3>
          {/* Le code se dicte à voix haute à une salle : il doit se lire du fond. */}
          <p className="code-presence">{ouverture.code}</p>
          <p>
            Valable jusqu'à <strong>{heure(ouverture.expirationAt)}</strong>, soit quinze
            minutes après l'ouverture.
          </p>
        </div>
      )}
    </div>
  );
}

function LigneSeance({ session, onChange }: { session: SessionResume; onChange: () => void }) {
  const [detailOuvert, setDetailOuvert] = useState(false);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function agir(action: () => Promise<string>) {
    setEnCours(true);
    setErreur(null);
    try {
      setMessage(await action());
      onChange();
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  const cloturer = () => agir(async () => {
    const r = await operations.cloturerSession(session.id);
    // Le formateur doit savoir tout de suite que certains exercices ne seront
    // jamais relus : le découvrir plus tard au tableau serait trop tard (RG17).
    return `${r.exercicesAttribues} exercice(s) attribué(s)` +
      (r.exercicesNonAttribuables > 0
        ? `, ${r.exercicesNonAttribuables} sans relecteur possible`
        : '');
  });

  const finaliser = () => agir(async () => {
    const r = await operations.finaliserSession(session.id);
    return `${r.relecturesFigees} relecture(s) figée(s) définitivement.`;
  });

  return (
    <>
      <tr>
        <th scope="row">{session.titre}</th>
        <td><Etat variante={VARIANTE_STATUT[session.statut as StatutSession]}>{session.statut}</Etat></td>
        <td className="nombre">{heure(session.ouvertureAt)}</td>
        <td className="nombre">{heure(session.expirationAt)}</td>
        <td>
          {/* L'action proposée découle du statut renvoyé par l'API, jamais d'une
              date recalculée ici : la clôture est une décision, pas un délai (F3). */}
          {session.statut === 'OUVERTE' && (
            <button type="button" onClick={cloturer} disabled={enCours}>Clôturer</button>
          )}
          {session.statut === 'CLOTUREE' && (
            <button type="button" onClick={finaliser} disabled={enCours}>Finaliser</button>
          )}
          {session.statut === 'FINALISEE' && <small>Terminée</small>}
        </td>
        <td>
          <button type="button" className="bouton--discret"
                  aria-expanded={detailOuvert}
                  onClick={() => setDetailOuvert(!detailOuvert)}>
            {detailOuvert ? 'Masquer' : 'Voir'}
          </button>
        </td>
      </tr>
      {(message || erreur || detailOuvert) && (
        <tr>
          <td colSpan={6}>
            {message && <Succes>{message}</Succes>}
            {erreur && <Erreur erreur={erreur} />}
            {detailOuvert && (
              <div className="detail-deplie"><DetailDesPresences session={session} /></div>
            )}
          </td>
        </tr>
      )}
    </>
  );
}

function DetailDesPresences({ session }: { session: SessionResume }) {
  const presences = useRequete<PresenceDetail[]>(
    () => operations.listerPresencesDeLaSession(session.id),
    [session.id],
  );
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function ajouter() {
    if (etudiantId === null) return;
    setEnCours(true);
    setErreur(null);
    try {
      await operations.enregistrerPresenceManuelle(session.id, etudiantId);
      setEtudiantId(null);
      presences.recharger();
    } catch (e) {
      setErreur(e as ErreurApi);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <>
      <h3>Présences</h3>
      <Requete etat={presences} quoi="des présences">
        {(liste) =>
          liste.length === 0 ? (
            <Vide>Personne n'est encore déclaré présent.</Vide>
          ) : (
            <ul className="liste-nue">
              {liste.map((presence) => (
                <li key={presence.etudiantId}>
                  {presence.nom}
                  {/* Q14 : l'ajout manuel doit se voir. En toutes lettres plutôt
                      que par une icône, que rien n'expliquerait. */}
                  {presence.source === 'FORMATEUR' && (
                    <> <Etat variante="cloture">ajouté par le formateur</Etat></>
                  )}
                </li>
              ))}
            </ul>
          )
        }
      </Requete>

      {/* L'ajout manuel n'a de sens que tant que la séance accepte des présences :
          après la clôture, sa composition est figée et a servi au tirage (RG13). */}
      {session.statut === 'OUVERTE' && (
        <fieldset>
          <legend>Ajouter une présence à la main</legend>
          <div className="ligne-champs">
            <ChoixEtudiant promotionId={PROMOTION} valeur={etudiantId} onChange={setEtudiantId} />
            <button type="button" onClick={ajouter} disabled={enCours || etudiantId === null}
                    style={{ marginBottom: 'var(--e4)' }}>
              {enCours ? 'Ajout…' : 'Ajouter'}
            </button>
          </div>
        </fieldset>
      )}

      {erreur && <Erreur erreur={erreur} />}
    </>
  );
}

function TableauRecapitulatif() {
  const tableau = useRequete<LigneTableau[]>(() => operations.consulterTableau(PROMOTION));

  return (
    <div className="carte">
      <h2>Suivi de la promotion</h2>
      <Requete etat={tableau} quoi="du tableau">
        {(lignes) =>
          lignes.length === 0 ? (
            <Vide>Aucun étudiant dans cette promotion.</Vide>
          ) : (
            <div className="tableau-defilant">
              <table>
                <caption className="sr-only">Récapitulatif par étudiant</caption>
                <thead>
                  <tr>
                    <th scope="col">Étudiant</th>
                    <th scope="col">Présences</th>
                    <th scope="col">Exercices</th>
                    <th scope="col">Moyenne reçue</th>
                    <th scope="col">Relectures à rendre</th>
                  </tr>
                </thead>
                <tbody>
                  {lignes.map((ligne) => (
                    <tr key={ligne.etudiantId}>
                      <th scope="row">{ligne.nom}</th>
                      <td className="nombre">{ligne.presences}</td>
                      <td className="nombre">{ligne.exercicesDeposes}</td>
                      {/* La moyenne vient de l'API et n'est ni recalculée ni
                          réarrondie ici (RG24, contrainte F3). Vide veut dire
                          « aucune note reçue », ce qui n'est pas zéro. */}
                      <td className="nombre">
                        {ligne.moyenne ?? '—'}
                        {ligne.moyenne !== null && ligne.moyenneProvisoire && (
                          <> <Etat variante="provisoire">provisoire</Etat></>
                        )}
                      </td>
                      <td className="nombre">
                        {ligne.relecturesEnAttente > 0
                          ? <strong>{ligne.relecturesEnAttente}</strong>
                          : 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        }
      </Requete>
    </div>
  );
}

function heure(iso: string) {
  return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}
