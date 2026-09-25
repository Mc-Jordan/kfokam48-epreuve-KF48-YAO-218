import { useCallback, useEffect, useState } from 'react';
import { ErreurApi } from './client';

/**
 * États de chargement et d'erreur, gérés une fois pour toutes (contrainte F3).
 * Aucun écran ne réinvente ce triplet.
 */
export interface EtatRequete<T> {
  donnees: T | null;
  enCours: boolean;
  erreur: ErreurApi | null;
  recharger: () => void;
}

export function useRequete<T>(appel: () => Promise<T>, dependances: unknown[] = []): EtatRequete<T> {
  const [donnees, setDonnees] = useState<T | null>(null);
  const [enCours, setEnCours] = useState(true);
  const [erreur, setErreur] = useState<ErreurApi | null>(null);
  const [compteur, setCompteur] = useState(0);

  const recharger = useCallback(() => setCompteur((n) => n + 1), []);

  useEffect(() => {
    let abandonne = false;
    setEnCours(true);
    setErreur(null);
    appel()
      .then((resultat) => {
        if (!abandonne) setDonnees(resultat);
      })
      .catch((e: unknown) => {
        if (!abandonne) {
          setErreur(e instanceof ErreurApi ? e : new ErreurApi('ERREUR_INCONNUE', String(e), 0));
        }
      })
      .finally(() => {
        if (!abandonne) setEnCours(false);
      });
    return () => {
      abandonne = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependances, compteur]);

  return { donnees, enCours, erreur, recharger };
}
