import type { ReactNode } from 'react';
import type { ErreurApi } from '../api/client';

/** Indicateur de chargement, partagé par tous les écrans. */
export function Chargement({ quoi }: { quoi: string }) {
  return <p className="chargement" role="status" aria-live="polite">Chargement {quoi}…</p>;
}

/**
 * Affichage d'erreur. Le message vient du serveur : il est déjà en français et
 * destiné à l'utilisateur (RG25). Le code stable est affiché à côté, en petit,
 * parce qu'un correcteur doit pouvoir le rapprocher du contrat — mais il ne
 * doit pas écraser la phrase que l'utilisateur, lui, doit lire.
 */
export function Erreur({ erreur, onReessayer }: { erreur: ErreurApi; onReessayer?: () => void }) {
  return (
    <div className="message message--erreur" role="alert">
      <div>
        <p>{erreur.message}</p>
        <p className="message__code">{erreur.code}</p>
      </div>
      {onReessayer && (
        <button type="button" className="bouton--discret" onClick={onReessayer}>Réessayer</button>
      )}
    </div>
  );
}

/** Confirmation brève d'une action réussie. */
export function Succes({ children }: { children: ReactNode }) {
  return <div className="message message--succes" role="status"><p>{children}</p></div>;
}

/** Ce qu'il faut afficher quand il n'y a rien à afficher, et pourquoi. */
export function Vide({ children }: { children: ReactNode }) {
  return <p className="vide">{children}</p>;
}

/**
 * Étiquette d'état — statut de séance, d'exercice, note provisoire.
 *
 * Le mot est toujours écrit : la couleur seule ne dit rien à qui ne la
 * distingue pas, et rien du tout sur une impression en noir et blanc.
 */
export function Etat({ variante, children }: {
  variante: 'ouvert' | 'cloture' | 'finalise' | 'provisoire' | 'impossible';
  children: ReactNode;
}) {
  return <span className={`etat etat--${variante}`}>{children}</span>;
}

/** Enveloppe les trois états d'une requête, pour qu'aucun écran ne les oublie. */
export function Requete<T>({
  etat, children, quoi,
}: {
  etat: { donnees: T | null; enCours: boolean; erreur: ErreurApi | null; recharger: () => void };
  children: (donnees: T) => ReactNode;
  quoi: string;
}) {
  if (etat.enCours) return <Chargement quoi={quoi} />;
  if (etat.erreur) return <Erreur erreur={etat.erreur} onReessayer={etat.recharger} />;
  if (etat.donnees === null) return null;
  return <>{children(etat.donnees)}</>;
}
