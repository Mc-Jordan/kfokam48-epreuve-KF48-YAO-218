import { api } from './client';
import type {
  Etudiant, Exercice, LigneTableau, Presence, PresenceDetail,
  RelectureAssignee, RelectureRecue, ResultatCloture, ResultatFinalisation,
  SessionOuverte, SessionResume,
} from './types';

/**
 * Une fonction par opération du contrat, nommée comme son `operationId`.
 * Les écrans appellent ces fonctions et rien d'autre : ils ignorent les chemins,
 * les verbes et le format d'erreur (contrainte F3).
 */

export const operations = {
  // --- Sessions -------------------------------------------------------------
  ouvrirSession: (titre: string, promotionId: number) =>
    api.post<SessionOuverte>('/sessions', { titre, promotionId }),

  listerSessions: (promotionId: number) =>
    api.get<SessionResume[]>(`/sessions?promotionId=${promotionId}`),

  cloturerSession: (sessionId: number) =>
    api.post<ResultatCloture>(`/sessions/${sessionId}/cloture`),

  finaliserSession: (sessionId: number) =>
    api.post<ResultatFinalisation>(`/sessions/${sessionId}/finalisation`),

  listerPresencesDeLaSession: (sessionId: number) =>
    api.get<PresenceDetail[]>(`/sessions/${sessionId}/presences`),

  // --- Etudiants ------------------------------------------------------------
  listerEtudiantsDeLaPromotion: (promotionId: number) =>
    api.get<Etudiant[]>(`/promotions/${promotionId}/etudiants`),

  // --- Presences ------------------------------------------------------------
  marquerPresence: (code: string, etudiantId: number) =>
    api.post<Presence>('/presences', { code, etudiantId }),

  enregistrerPresenceManuelle: (sessionId: number, etudiantId: number) =>
    api.post<Presence>('/presences/manuelles', { sessionId, etudiantId }),

  // --- Exercices ------------------------------------------------------------
  deposerExercice: (sessionId: number, etudiantId: number, lien: string) =>
    api.post<Exercice>('/exercices', { sessionId, etudiantId, lien }),

  remplacerLienExercice: (exerciceId: number, lien: string) =>
    api.put<Exercice>(`/exercices/${exerciceId}`, { lien }),

  consulterRelectureRecue: (exerciceId: number) =>
    api.get<RelectureRecue>(`/exercices/${exerciceId}/relecture`),

  // --- Relectures -----------------------------------------------------------
  listerRelecturesDuRelecteur: (relecteurId: number) =>
    api.get<RelectureAssignee[]>(`/relectures?relecteurId=${relecteurId}`),

  rendreRelecture: (relectureId: number, note: number, commentaire: string) =>
    api.post<void>(`/relectures/${relectureId}`, { note, commentaire }),

  // --- Tableau --------------------------------------------------------------
  consulterTableau: (promotionId: number) =>
    api.get<LigneTableau[]>(`/tableau?promotionId=${promotionId}`),
};
