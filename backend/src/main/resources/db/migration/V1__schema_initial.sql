-- =============================================================================
--  V1 — Schema initial
--
--  Traduction stricte de docs/diagrammes/D2-modele-donnees.md. Toute contrainte
--  nommee dans le diagramme figure ici sous le meme nom, et reciproquement :
--  la correspondance entre D2 et cette migration est un critere note.
--
--  Chaque contrainte porte en commentaire la regle de gestion qu'elle applique.
-- =============================================================================

-- --- Promotions --------------------------------------------------------------
CREATE TABLE promotions (
    id  BIGSERIAL     PRIMARY KEY,
    nom VARCHAR(120)  NOT NULL,
    CONSTRAINT uq_promotions_nom UNIQUE (nom)
);

-- --- Etudiants ---------------------------------------------------------------
CREATE TABLE etudiants (
    id           BIGSERIAL    PRIMARY KEY,
    promotion_id BIGINT       NOT NULL,
    nom          VARCHAR(160) NOT NULL,
    CONSTRAINT fk_etudiants_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE
);

CREATE INDEX idx_etudiants_promotion ON etudiants (promotion_id);

-- --- Sessions ----------------------------------------------------------------
CREATE TABLE sessions (
    id              BIGSERIAL    PRIMARY KEY,
    promotion_id    BIGINT       NOT NULL,
    titre           VARCHAR(200) NOT NULL,
    code            VARCHAR(16)  NOT NULL,
    ouverture_at    TIMESTAMPTZ  NOT NULL,
    expiration_at   TIMESTAMPTZ  NOT NULL,
    statut          VARCHAR(16)  NOT NULL,
    cloture_at      TIMESTAMPTZ,
    finalisation_at TIMESTAMPTZ,
    CONSTRAINT fk_sessions_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE,
    -- RG2 : le code identifie la session a lui seul, POST /api/presences ne
    -- transporte rien d'autre.
    CONSTRAINT uq_sessions_code UNIQUE (code),
    -- RG12 : trois etats, dans cet ordre, sans retour en arriere.
    CONSTRAINT ck_sessions_statut
        CHECK (statut IN ('OUVERTE', 'CLOTUREE', 'FINALISEE')),
    -- RG1 : le code expire quinze minutes apres l'ouverture.
    CONSTRAINT ck_sessions_expiration CHECK (expiration_at > ouverture_at)
);

-- --- Presences ---------------------------------------------------------------
CREATE TABLE presences (
    id             BIGSERIAL   PRIMARY KEY,
    session_id     BIGINT      NOT NULL,
    etudiant_id    BIGINT      NOT NULL,
    source         VARCHAR(16) NOT NULL,
    enregistree_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_presences_session
        FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_presences_etudiant
        FOREIGN KEY (etudiant_id) REFERENCES etudiants (id) ON DELETE CASCADE,
    -- RG3 : une presence par etudiant et par session. C'est cette contrainte
    -- qui produit le 409 DEJA_PRESENT du contrat.
    CONSTRAINT uq_presences_session_etudiant UNIQUE (session_id, etudiant_id),
    -- RG4 : toute presence porte sa source, pour repondre a Q14.
    CONSTRAINT ck_presences_source CHECK (source IN ('ETUDIANT', 'FORMATEUR'))
);

CREATE INDEX idx_presences_session ON presences (session_id);

-- --- Exercices ---------------------------------------------------------------
CREATE TABLE exercices (
    id          BIGSERIAL   PRIMARY KEY,
    session_id  BIGINT      NOT NULL,
    etudiant_id BIGINT      NOT NULL,
    lien        TEXT        NOT NULL,
    statut      VARCHAR(24) NOT NULL,
    depose_at   TIMESTAMPTZ NOT NULL,
    modifie_at  TIMESTAMPTZ,
    CONSTRAINT fk_exercices_session
        FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_exercices_etudiant
        FOREIGN KEY (etudiant_id) REFERENCES etudiants (id) ON DELETE CASCADE,
    -- RG7 : un exercice par etudiant et par session. Produit le 409
    -- EXERCICE_DEJA_DEPOSE du contrat.
    CONSTRAINT uq_exercices_session_etudiant UNIQUE (session_id, etudiant_id),
    -- Cible de la cle etrangere composite posee par relectures : elle verrouille
    -- la denormalisation de session_id. Voir RG16.
    CONSTRAINT uq_exercices_id_session UNIQUE (id, session_id),
    -- RG13, RG17 : les quatre etats de D4, et eux seuls.
    CONSTRAINT ck_exercices_statut
        CHECK (statut IN ('DEPOSE', 'EN_ATTENTE_RELECTURE', 'RELU', 'NON_ATTRIBUABLE'))
);

CREATE INDEX idx_exercices_session ON exercices (session_id);

-- --- Relectures --------------------------------------------------------------
CREATE TABLE relectures (
    id           BIGSERIAL   PRIMARY KEY,
    exercice_id  BIGINT      NOT NULL,
    session_id   BIGINT      NOT NULL,
    relecteur_id BIGINT      NOT NULL,
    note         SMALLINT,
    commentaire  TEXT,
    statut       VARCHAR(16) NOT NULL,
    attribuee_at TIMESTAMPTZ NOT NULL,
    rendue_at    TIMESTAMPTZ,
    -- RG15 : un exercice recoit au plus un relecteur.
    CONSTRAINT uq_relectures_exercice UNIQUE (exercice_id),
    -- RG16 : un etudiant relit au plus un exercice par session. Cette regle
    -- exige session_id ici : UNIQUE (exercice_id, relecteur_id) ne contraindrait
    -- rien de plus que uq_relectures_exercice.
    CONSTRAINT uq_relectures_session_relecteur UNIQUE (session_id, relecteur_id),
    -- Verrouille la denormalisation : la base refuse une relecture dont la
    -- session contredirait celle de son exercice.
    CONSTRAINT fk_relectures_exercice_session
        FOREIGN KEY (exercice_id, session_id)
        REFERENCES exercices (id, session_id) ON DELETE CASCADE,
    CONSTRAINT fk_relectures_relecteur
        FOREIGN KEY (relecteur_id) REFERENCES etudiants (id) ON DELETE CASCADE,
    -- RG18 : une note est un entier de 0 a 20. Nulle tant qu'elle n'est pas
    -- rendue : la relecture existe des l'attribution (RG23).
    CONSTRAINT ck_relectures_note CHECK (note IS NULL OR note BETWEEN 0 AND 20),
    -- RG20, RG21 : attribution, rendu, puis figement a la finalisation.
    CONSTRAINT ck_relectures_statut
        CHECK (statut IN ('ATTRIBUEE', 'RENDUE', 'FIGEE')),
    -- Une relecture rendue ou figee porte necessairement une note.
    CONSTRAINT ck_relectures_note_si_rendue
        CHECK (statut = 'ATTRIBUEE' OR (note IS NOT NULL AND rendue_at IS NOT NULL))
);

CREATE INDEX idx_relectures_relecteur ON relectures (relecteur_id);
CREATE INDEX idx_relectures_session   ON relectures (session_id);

-- --- Tentatives de presence ---------------------------------------------------
-- RG6 : cinq echecs consecutifs bloquent l'etudiant deux minutes. Compter les
-- echecs recents suppose de les conserver ; un compteur sur etudiants ne
-- permettrait ni la fenetre glissante ni un test reproductible.
CREATE TABLE tentatives_presence (
    id          BIGSERIAL   PRIMARY KEY,
    etudiant_id BIGINT      NOT NULL,
    code_saisi  VARCHAR(16) NOT NULL,
    reussie     BOOLEAN     NOT NULL,
    tentee_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_tentatives_etudiant
        FOREIGN KEY (etudiant_id) REFERENCES etudiants (id) ON DELETE CASCADE
);

CREATE INDEX idx_tentatives_etudiant_date
    ON tentatives_presence (etudiant_id, tentee_at DESC);
