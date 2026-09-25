/**
 * Types dérivés de `api/contrat.yaml`. Le contrat fait autorité : toute
 * divergence ici est un défaut, pas une adaptation.
 *
 * Aucune règle métier ne vit dans ce fichier — il ne décrit que la forme des
 * échanges (contrainte F3).
 */

export type StatutSession = 'OUVERTE' | 'CLOTUREE' | 'FINALISEE';
export type SourcePresence = 'ETUDIANT' | 'FORMATEUR';
export type StatutExercice = 'DEPOSE' | 'EN_ATTENTE_RELECTURE' | 'RELU' | 'NON_ATTRIBUABLE';
export type StatutRelecture = 'ATTRIBUEE' | 'RENDUE' | 'FIGEE';

/** Corps d'erreur imposé, pour toutes les erreurs sans exception (RG25). */
export interface ReponseErreur {
  code: string;
  message: string;
}

export interface Etudiant {
  id: number;
  nom: string;
}

export interface SessionResume {
  id: number;
  titre: string;
  statut: StatutSession;
  ouvertureAt: string;
  expirationAt: string;
  clotureAt?: string | null;
  finalisationAt?: string | null;
}

export interface SessionOuverte {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: SourcePresence;
}

export interface PresenceDetail {
  etudiantId: number;
  nom: string;
  source: SourcePresence;
  enregistreeAt: string;
}

export interface Exercice {
  id: number;
  statut: StatutExercice;
}

export interface RelectureAssignee {
  relectureId: number;
  exerciceId: number;
  sessionTitre: string;
  lien: string;
  statut: StatutRelecture;
  note?: number | null;
  commentaire?: string | null;
}

/** Aucun champ n'identifie le relecteur : RG22 est tenue par la forme même. */
export interface RelectureRecue {
  exerciceId: number;
  statut: StatutExercice;
  note?: number | null;
  commentaire?: string | null;
}

export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  /** Calculée par le serveur ; jamais recalculée ici (RG24, contrainte F3). */
  moyenne: number | null;
  relecturesEnAttente: number;
}

export interface ResultatCloture {
  id: number;
  statut: 'CLOTUREE';
  exercicesAttribues: number;
  exercicesNonAttribuables: number;
}

export interface ResultatFinalisation {
  id: number;
  statut: 'FINALISEE';
  relecturesFigees: number;
}
