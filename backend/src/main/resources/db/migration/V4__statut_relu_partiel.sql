-- =============================================================================
--  V4 — Le statut RELU_PARTIEL
--
--  Suite du changement de l'etape 3. Un exercice recoit deux relectures : entre
--  « personne n'a rendu » et « tout est rendu », il existe desormais un etat
--  intermediaire ou la note existe mais n'est pas definitive. C'est exactement
--  ce que le client demande — « on affiche sa note en attendant, mais marquee
--  comme provisoire ». Voir D4 et RG24, RG26.
--
--  Migration AJOUTEE. V1, V2 et V3 ne sont pas touchees : une base en service
--  l'applique par-dessus. Aucune ligne existante ne devient invalide, la
--  contrainte etant elargie et non restreinte.
-- =============================================================================

ALTER TABLE exercices DROP CONSTRAINT ck_exercices_statut;

ALTER TABLE exercices
    ADD CONSTRAINT ck_exercices_statut
    CHECK (statut IN ('DEPOSE', 'EN_ATTENTE_RELECTURE', 'RELU_PARTIEL', 'RELU', 'NON_ATTRIBUABLE'));
