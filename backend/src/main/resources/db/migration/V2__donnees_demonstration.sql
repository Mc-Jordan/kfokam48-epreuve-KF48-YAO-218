-- =============================================================================
--  V2 — Jeu de donnees de demonstration
--
--  Migration DISTINCTE du schema : elle ne cree aucune structure, et le schema
--  ne depend pas d'elle. Les deux evoluent separement.
--
--  Le sujet est explicite : « prevois quelques donnees de demonstration chargees
--  au demarrage, sinon le correcteur ouvre une application vide et ne peut rien
--  verifier ». Ce jeu vise donc les CAS LIMITES, pas le volume :
--
--    * une seance FINALISEE, avec des relectures figees et une moyenne calculable
--    * une seance CLOTUREE, avec une relecture rendue et une EN ATTENTE (RG23)
--    * un exercice NON_ATTRIBUABLE, seul moyen de voir RG17 a l'ecran
--    * une presence de source FORMATEUR, pour Q14 et RG4
--    * un etudiant SANS AUCUNE NOTE, pour que la moyenne vide se distingue d'un zero
--    * une seance OUVERTE, pour que le correcteur puisse agir lui-meme
--
--  Les horodatages sont relatifs a l'instant du demarrage : un jeu date en dur
--  aurait tous ses codes expires des le lendemain.
-- =============================================================================

-- --- Promotions ---------------------------------------------------------------
INSERT INTO promotions (nom) VALUES
    ('KFOKAM48 - Promotion 2026'),
    ('KFOKAM48 - Promotion 2025');

-- --- Etudiants ----------------------------------------------------------------
-- Douze etudiants dans la promotion 1 : assez pour que le tirage au sort de la
-- cloture soit observable, assez peu pour que le tableau se lise d'un coup d'oeil.
INSERT INTO etudiants (promotion_id, nom) VALUES
    (1, 'Awa NJOYA'),
    (1, 'Biloa MANGA'),
    (1, 'Cedric FOTSO'),
    (1, 'Danielle EBANDA'),
    (1, 'Eric TCHINDA'),
    (1, 'Francine MBALLA'),
    (1, 'Gaston NKOLO'),
    (1, 'Hortense ATANGANA'),
    (1, 'Ibrahim BOUBA'),
    (1, 'Josiane KAMGA'),
    (1, 'Kevin ESSOMBA'),
    (1, 'Laure NGUEMA'),
    (2, 'Marc ONANA');

-- --- Seances ------------------------------------------------------------------
-- 1 : FINALISEE  — relectures figees, moyennes visibles au tableau
-- 2 : CLOTUREE   — une relecture rendue, une en attente, un NON_ATTRIBUABLE
-- 3 : OUVERTE    — le correcteur peut marquer une presence et deposer
INSERT INTO sessions (promotion_id, titre, code, ouverture_at, expiration_at, statut, cloture_at, finalisation_at) VALUES
    (1, 'Algorithmique - tris et complexite', 'DEMO01',
     now() - interval '7 days',  now() - interval '7 days'  + interval '15 minutes', 'FINALISEE',
     now() - interval '6 days',  now() - interval '5 days'),
    (1, 'Bases de donnees - modelisation',    'DEMO02',
     now() - interval '2 days',  now() - interval '2 days'  + interval '15 minutes', 'CLOTUREE',
     now() - interval '1 day',   NULL),
    (1, 'Programmation web - API REST',       'DEMO03',
     now() - interval '2 minutes', now() - interval '2 minutes' + interval '15 minutes', 'OUVERTE',
     NULL, NULL);

-- --- Presences ----------------------------------------------------------------
-- Seance 1 : six presents, dont un ajoute par le formateur (Q14, RG4).
INSERT INTO presences (session_id, etudiant_id, source, enregistree_at)
SELECT 1, id, 'ETUDIANT', now() - interval '7 days' + interval '3 minutes'
FROM etudiants WHERE id IN (1, 2, 3, 4, 5);

INSERT INTO presences (session_id, etudiant_id, source, enregistree_at) VALUES
    (1, 6, 'FORMATEUR', now() - interval '7 days' + interval '40 minutes');

-- Seance 2 : quatre presents, plus Josiane SEULE a avoir depose sans pair
-- disponible au moment du tirage (voir plus bas).
INSERT INTO presences (session_id, etudiant_id, source, enregistree_at)
SELECT 2, id, 'ETUDIANT', now() - interval '2 days' + interval '5 minutes'
FROM etudiants WHERE id IN (1, 2, 7, 8);

-- Seance 3, encore ouverte : deux presents, le correcteur peut en ajouter.
INSERT INTO presences (session_id, etudiant_id, source, enregistree_at)
SELECT 3, id, 'ETUDIANT', now() - interval '1 minute'
FROM etudiants WHERE id IN (1, 2);

-- --- Exercices ----------------------------------------------------------------
-- Seance 1 : cinq exercices, tous relus (statut RELU).
INSERT INTO exercices (session_id, etudiant_id, lien, statut, depose_at)
SELECT 1, id,
       'https://github.com/kfokam48-demo/tri-' || lower(split_part(nom, ' ', 1)),
       'RELU', now() - interval '7 days' + interval '2 hours'
FROM etudiants WHERE id IN (1, 2, 3, 4, 5);

-- Seance 2 : trois exercices. Deux sont attribues, le troisieme ne l'est pas.
INSERT INTO exercices (session_id, etudiant_id, lien, statut, depose_at) VALUES
    (2, 1, 'https://github.com/kfokam48-demo/mcd-awa',   'RELU',                 now() - interval '2 days' + interval '3 hours'),
    (2, 2, 'https://github.com/kfokam48-demo/mcd-biloa', 'EN_ATTENTE_RELECTURE', now() - interval '2 days' + interval '3 hours'),
    (2, 7, 'https://github.com/kfokam48-demo/mcd-gaston','NON_ATTRIBUABLE',      now() - interval '2 days' + interval '4 hours');

-- --- Relectures ---------------------------------------------------------------
-- Seance 1 : cinq relectures FIGEES, en rotation circulaire — exactement ce que
-- produit l'algorithme d'attribution. Notes variees pour que les moyennes du
-- tableau ne soient pas toutes identiques.
INSERT INTO relectures (exercice_id, session_id, relecteur_id, note, commentaire, statut, attribuee_at, rendue_at) VALUES
    (1, 1, 2, 16, 'Tri fusion correct et bien commente. La complexite aurait merite une justification ecrite.', 'FIGEE', now() - interval '6 days', now() - interval '6 days' + interval '2 hours'),
    (2, 1, 3, 12, 'Le tri fonctionne mais le cas du tableau vide n''est pas traite.',                            'FIGEE', now() - interval '6 days', now() - interval '6 days' + interval '3 hours'),
    (3, 1, 4, 18, 'Travail remarquable, y compris les tests aux bornes.',                                        'FIGEE', now() - interval '6 days', now() - interval '6 days' + interval '1 hour'),
    (4, 1, 5, 9,  'Le code compile mais ne trie pas dans tous les cas. A reprendre.',                            'FIGEE', now() - interval '6 days', now() - interval '6 days' + interval '5 hours'),
    (5, 1, 1, 14, 'Bonne structure. Quelques noms de variables trop courts.',                                    'FIGEE', now() - interval '6 days', now() - interval '6 days' + interval '4 hours');

-- Seance 2 : une relecture RENDUE et une ATTRIBUEE jamais rendue. Cette
-- derniere est la seule facon de voir RG23 a l'ecran — le relecteur defaillant
-- de Q11, que le formateur doit reperer dans son tableau.
INSERT INTO relectures (exercice_id, session_id, relecteur_id, note, commentaire, statut, attribuee_at, rendue_at) VALUES
    (6, 2, 8, 15, 'Le modele est juste. Les cardinalites de la relation N-N sont a preciser.', 'RENDUE', now() - interval '1 day', now() - interval '20 hours'),
    (7, 2, 1, NULL, NULL,                                                                      'ATTRIBUEE', now() - interval '1 day', NULL);

-- --- Ce que le correcteur doit voir -------------------------------------------
--  Awa NJOYA          : 3 presences, 2 exercices, moyenne (16+15)/2 = 15.50, 1 relecture DUE
--  Biloa MANGA        : 3 presences, 2 exercices, moyenne 12.00, 0 relecture due
--  Gaston NKOLO       : 1 presence,  1 exercice,  moyenne VIDE — son exercice est NON_ATTRIBUABLE
--  Hortense ATANGANA  : 1 presence,  0 exercice,  moyenne VIDE, 0 relecture due
--  Francine MBALLA    : 1 presence AJOUTEE PAR LE FORMATEUR, 0 exercice, moyenne VIDE
--  Laure NGUEMA       : aucune presence — la ligne existe quand meme, a zero
