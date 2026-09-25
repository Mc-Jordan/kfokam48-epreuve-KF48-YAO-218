import type { ReactNode } from 'react';
import type { ErreurApi } from '../api/client';

/** Indicateur de chargement, partagé par tous les écrans. */
export function Chargement({ quoi }: { quoi: string }) {
  return <p role="status" aria-live="polite">Chargement {quoi}…</p>;
}

/**
 * Affichage d'erreur. Le message vient du serveur : il est déjà en français et
 * destiné à l'utilisateur (RG25). Le code stable est affiché à côté, parce qu'un
 * correcteur doit pouvoir le rapprocher du contrat.
 */
export function Erreur({ erreur, onReessayer }: { erreur: ErreurApi; onReessayer?: () => void }) {
  return (
    <div role="alert">
      <p>{erreur.message}</p>
      <p><small>{erreur.code}</small></p>
      {onReessayer && <button type="button" onClick={onReessayer}>Réessayer</button>}
    </div>
  );
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
