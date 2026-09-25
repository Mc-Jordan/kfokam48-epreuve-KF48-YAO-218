-- =============================================================================
--  V3 — Deux relecteurs par exercice
--
--  Consequence du changement de besoin de l'etape 3 : le client revient sur Q6
--  et demande deux relecteurs par exercice, la note retenue etant la moyenne
--  des deux. Voir la section 7 du cahier des charges et sa revision n 3.
--
--  Cette migration est AJOUTEE. V1 et V2 ne sont pas touchees : une base deja
--  en service applique V3 par-dessus et conserve toutes ses donnees.
--
--  Ce qu'elle ne fait PAS, volontairement :
--    elle ne reattribue aucun second relecteur aux seances deja cloturees.
--    Le client ecrit « a partir de maintenant ». Reattribuer retroactivement
--    designerait des relecteurs pour des seances closes depuis des jours et
--    modifierait des moyennes deja communiquees. Decision ecrite en section 7.
-- =============================================================================

-- --- RG15 : un exercice recoit deux relecteurs, non plus un seul --------------
-- L'unicite ne peut plus porter sur le seul exercice. Elle porte desormais sur
-- le couple : un meme etudiant ne relit pas deux fois le meme exercice.
ALTER TABLE relectures DROP CONSTRAINT uq_relectures_exercice;

ALTER TABLE relectures
    ADD CONSTRAINT uq_relectures_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- --- RG16 : un etudiant relit desormais deux exercices par seance -------------
-- La contrainte interdisait une seconde relecture par seance. Elle devait donc
-- disparaitre. La limite a deux est tenue par l'algorithme d'attribution et
-- verifiee par test : c'est un recul assume, ecrit dans D2 et dans la matrice
-- de tracabilite. « Au plus deux » ne s'exprime pas en SQL declaratif sans
-- compter des lignes.
ALTER TABLE relectures DROP CONSTRAINT uq_relectures_session_relecteur;

-- fk_relectures_exercice_session est conservee : elle interdit toujours qu'une
-- relecture designe une autre seance que celle de son exercice.

-- --- Verification -------------------------------------------------------------
-- Les donnees existantes restent valides : chaque exercice deja attribue garde
-- son unique relecteur, et le couple (exercice_id, relecteur_id) etait deja
-- unique par construction puisque exercice_id l'etait.
