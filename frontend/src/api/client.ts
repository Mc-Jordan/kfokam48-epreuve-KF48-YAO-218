import type { ReponseErreur } from './types';

/**
 * Couche d'accès à l'API — point de passage unique (contrainte F3).
 * Aucun `fetch` ne doit exister ailleurs dans l'application.
 */

const BASE = import.meta.env.VITE_API_BASE ?? '/api';

/**
 * Erreur d'API portant le code stable du contrat. Les écrans se branchent sur
 * `code`, jamais sur le texte du message : le code est stable, le message non.
 */
export class ErreurApi extends Error {
  /** Code stable du contrat, en majuscules. Les écrans se branchent dessus. */
  readonly code: string;
  readonly statut: number;

  constructor(code: string, message: string, statut: number) {
    super(message);
    this.name = 'ErreurApi';
    this.code = code;
    this.statut = statut;
  }
}

async function requete<T>(chemin: string, options: RequestInit = {}): Promise<T> {
  let reponse: Response;
  try {
    reponse = await fetch(`${BASE}${chemin}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    });
  } catch {
    throw new ErreurApi('RESEAU_INDISPONIBLE', "Le serveur est injoignable.", 0);
  }

  if (reponse.status === 204) {
    return undefined as T;
  }

  const corps = await reponse.text();
  const donnees: unknown = corps.length > 0 ? JSON.parse(corps) : null;

  if (!reponse.ok) {
    const erreur = donnees as ReponseErreur | null;
    throw new ErreurApi(
      erreur?.code ?? 'ERREUR_INCONNUE',
      erreur?.message ?? "Une erreur est survenue.",
      reponse.status,
    );
  }
  return donnees as T;
}

export const api = {
  get: <T>(chemin: string) => requete<T>(chemin),
  post: <T>(chemin: string, corps?: unknown) =>
    requete<T>(chemin, { method: 'POST', body: corps === undefined ? undefined : JSON.stringify(corps) }),
  put: <T>(chemin: string, corps: unknown) =>
    requete<T>(chemin, { method: 'PUT', body: JSON.stringify(corps) }),
};
