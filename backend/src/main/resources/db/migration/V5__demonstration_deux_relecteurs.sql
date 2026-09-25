-- =============================================================================
--  V5 — Le jeu de demonstration montre le cas provisoire
--
--  Suite du changement de l'etape 3. Sans cette migration, RG24 et RG26 ne sont
--  visibles nulle part a l'ecran : le jeu de V2 datait d'avant le changement et
--  n'a qu'un relecteur par exercice.
--
--  Elle AJOUTE un second relecteur aux exercices de la seance 2, qui est encore
--  CLOTUREE. Elle ne touche pas a la seance 1, deja FINALISEE : reattribuer
--  retroactivement une seance close contredirait la decision ecrite en
--  section 7 du cahier des charges.
-- =============================================================================

-- Exercice 6 (Awa, seance 2) : deuxieme relecture RENDUE.
-- Sa note passe de 15 a la moyenne de 15 et 17, soit 16.00 — DEFINITIVE.
INSERT INTO relectures (exercice_id, session_id, relecteur_id, note, commentaire, statut, attribuee_at, rendue_at)
VALUES (6, 2, 2, 17, 'D''accord avec le premier relecteur. J''ajoute que les index manquent sur les cles etrangeres.',
        'RENDUE', now() - interval '1 day', now() - interval '18 hours');

-- Exercice 7 (Biloa, seance 2) : le premier relecteur a rendu, le second non.
-- C'est le cas que le client decrit : la note s'affiche, marquee PROVISOIRE.
INSERT INTO relectures (exercice_id, session_id, relecteur_id, note, commentaire, statut, attribuee_at, rendue_at)
VALUES (7, 2, 7, 13, 'Le modele tient mais les libelles sont trop courts pour etre relus par un tiers.',
        'RENDUE', now() - interval '1 day', now() - interval '16 hours');

UPDATE exercices SET statut = 'RELU_PARTIEL' WHERE id = 7;
UPDATE exercices SET statut = 'RELU'         WHERE id = 6;

-- --- Ce que le correcteur doit voir -------------------------------------------
--  Exercice 6 : note 16.00, NON provisoire — deux relectures rendues
--  Exercice 7 : note 13.00, PROVISOIRE     — une seule des deux rendue
--  Awa NJOYA  : moyenne definitive ; Biloa MANGA : moyenne PROVISOIRE
