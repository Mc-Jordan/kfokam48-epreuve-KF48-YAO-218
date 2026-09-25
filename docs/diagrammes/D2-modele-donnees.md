# D2 — Modèle de données

Les tables, colonnes et contraintes sont nommées **telles qu'elles seront créées par les
migrations Flyway** : `snake_case`, pluriel pour les tables, suffixe `_id` pour les clés
étrangères, `_at` pour les horodatages. Le diagramme et `backend/src/main/resources/db/migration/`
doivent rester identiques — c'est un critère noté.

```mermaid
erDiagram
    PROMOTIONS ||--o{ ETUDIANTS : "regroupe"
    PROMOTIONS ||--o{ SESSIONS : "accueille"
    SESSIONS   ||--o{ PRESENCES : "constate"
    ETUDIANTS  ||--o{ PRESENCES : "est presente a"
    SESSIONS   ||--o{ EXERCICES : "recoit"
    ETUDIANTS  ||--o{ EXERCICES : "depose"
    EXERCICES  ||--o| RELECTURES : "est relu par au plus une"
    ETUDIANTS  ||--o{ RELECTURES : "relit"
    SESSIONS   ||--o{ RELECTURES : "porte"
    ETUDIANTS  ||--o{ TENTATIVES_PRESENCE : "tente"

    PROMOTIONS {
        bigserial id PK
        varchar   nom "NOT NULL, UNIQUE"
    }

    ETUDIANTS {
        bigserial id PK
        bigint    promotion_id FK "NOT NULL"
        varchar   nom "NOT NULL"
    }

    SESSIONS {
        bigserial   id PK
        bigint      promotion_id FK "NOT NULL"
        varchar     titre "NOT NULL"
        varchar     code "NOT NULL, UNIQUE -- RG2"
        timestamptz ouverture_at "NOT NULL"
        timestamptz expiration_at "NOT NULL -- ouverture_at + 15 min, RG1"
        varchar     statut "NOT NULL, CHECK in OUVERTE CLOTUREE FINALISEE -- RG12"
        timestamptz cloture_at "NULL"
        timestamptz finalisation_at "NULL"
    }

    PRESENCES {
        bigserial   id PK
        bigint      session_id FK "NOT NULL"
        bigint      etudiant_id FK "NOT NULL"
        varchar     source "NOT NULL, CHECK in ETUDIANT FORMATEUR -- RG4"
        timestamptz enregistree_at "NOT NULL"
    }

    EXERCICES {
        bigserial   id PK
        bigint      session_id FK "NOT NULL"
        bigint      etudiant_id FK "NOT NULL"
        text        lien "NOT NULL -- RG9"
        varchar     statut "NOT NULL, CHECK in DEPOSE EN_ATTENTE_RELECTURE RELU NON_ATTRIBUABLE"
        timestamptz depose_at "NOT NULL"
        timestamptz modifie_at "NULL"
    }

    RELECTURES {
        bigserial   id PK
        bigint      exercice_id FK "NOT NULL, UNIQUE -- RG15"
        bigint      session_id FK "NOT NULL -- denormalise, verrouille par FK composite -- RG16"
        bigint      relecteur_id FK "NOT NULL -- jamais expose en JSON, RG22"
        smallint    note "NULL, CHECK between 0 and 20 -- RG18"
        text        commentaire "NULL"
        varchar     statut "NOT NULL, CHECK in ATTRIBUEE RENDUE FIGEE"
        timestamptz attribuee_at "NOT NULL"
        timestamptz rendue_at "NULL"
    }

    TENTATIVES_PRESENCE {
        bigserial   id PK
        bigint      etudiant_id FK "NOT NULL"
        varchar     code_saisi "NOT NULL"
        boolean     reussie "NOT NULL"
        timestamptz tentee_at "NOT NULL -- RG6"
    }
```

## Contraintes qui portent une règle de gestion

Une règle d'unicité absente d'ici sera absente de la migration. Chacune est donc nommée.

| Contrainte | Table | Définition | Règle |
|---|---|---|---|
| `uq_sessions_code` | `sessions` | `UNIQUE (code)` | `RG2` — le code identifie la session à lui seul |
| `uq_presences_session_etudiant` | `presences` | `UNIQUE (session_id, etudiant_id)` | `RG3` — une présence par étudiant et par session, source du `409 DEJA_PRESENT` |
| `uq_exercices_session_etudiant` | `exercices` | `UNIQUE (session_id, etudiant_id)` | `RG7` — un exercice par étudiant et par session, source du `409 EXERCICE_DEJA_DEPOSE` |
| `uq_relectures_exercice` | `relectures` | `UNIQUE (exercice_id)` | `RG15` — un exercice reçoit au plus un relecteur |
| `uq_relectures_session_relecteur` | `relectures` | `UNIQUE (session_id, relecteur_id)` | `RG16` — un étudiant relit au plus un exercice par session |
| `fk_relectures_exercice_session` | `relectures` | `FOREIGN KEY (exercice_id, session_id) REFERENCES exercices (id, session_id)` | verrouille la dénormalisation : `relectures.session_id` ne peut pas diverger de celle de son exercice |
| `uq_exercices_id_session` | `exercices` | `UNIQUE (id, session_id)` | cible de la clé étrangère composite ci-dessus |
| `ck_exercices_statut` | `exercices` | `CHECK (statut IN ('DEPOSE','EN_ATTENTE_RELECTURE','RELU','NON_ATTRIBUABLE'))` | `RG13`, `RG17` — les quatre états de `D4`, et eux seuls |
| `ck_relectures_statut` | `relectures` | `CHECK (statut IN ('ATTRIBUEE','RENDUE','FIGEE'))` | `RG20`, `RG21` — attribution, rendu, figement |
| `ck_relectures_note` | `relectures` | `CHECK (note IS NULL OR note BETWEEN 0 AND 20)` | `RG18` — note entière de 0 à 20 |
| `ck_sessions_statut` | `sessions` | `CHECK (statut IN ('OUVERTE','CLOTUREE','FINALISEE'))` | `RG12` — les trois états, sans retour en arrière |
| `ck_presences_source` | `presences` | `CHECK (source IN ('ETUDIANT','FORMATEUR'))` | `RG4` — traçabilité de l'ajout manuel |
| `uq_promotions_nom` | `promotions` | `UNIQUE (nom)` | deux promotions homonymes rendraient la liste de `Q1` inutilisable |
| `ck_sessions_expiration` | `sessions` | `CHECK (expiration_at > ouverture_at)` | `RG1` — garde-fou : une session ne peut pas naître déjà expirée |
| `ck_relectures_note_si_rendue` | `relectures` | `CHECK (statut = 'ATTRIBUEE' OR (note IS NOT NULL AND rendue_at IS NOT NULL))` | `RG20` — une relecture rendue ou figée porte nécessairement une note |
| *applicative* | `relectures` | `relecteur_id <> (SELECT etudiant_id FROM exercices WHERE id = exercice_id)` | `RG19` — jamais relire son propre exercice, vérifiée au service et couverte par un test unitaire |

Les clés étrangères ne sont pas listées une à une : elles suivent exactement les
relations du diagramme et se nomment `fk_<table>_<cible>`, à l'exception de
`fk_relectures_exercice_session`, composite, qui figure ci-dessus parce qu'elle
porte une garantie que le diagramme ne montre pas.

## Index

| Index | Motif |
|---|---|
| `idx_presences_session` sur `presences(session_id)` | Vivier des présents au moment du tirage (`RG14`) |
| `idx_exercices_session` sur `exercices(session_id)` | Exercices à attribuer à la clôture |
| `idx_relectures_relecteur` sur `relectures(relecteur_id)` | Liste des relectures dues à un étudiant (`EF8`, `Q16`) |
| `idx_relectures_session` sur `relectures(session_id)` | Figement de toutes les relectures d'une session à la finalisation (`RG21`) |
| `idx_tentatives_etudiant_date` sur `tentatives_presence(etudiant_id, tentee_at DESC)` | Cinq dernières tentatives d'un étudiant (`RG6`) |
| `idx_etudiants_promotion` sur `etudiants(promotion_id)` | Tableau récapitulatif en une requête, exigé par `ENF2` |

## Choix expliqués

- **Pas de table `relecteur`.** Le rôle naît de `relectures.relecteur_id`, conformément à la section 2 du cahier des charges. L'anonymat de `Q8` est alors tenu par le format de réponse : `relecteur_id` n'est exposé par aucun DTO destiné à l'auteur.
- **`note` et `commentaire` sont nullables.** Une relecture existe dès l'attribution, avant d'être rendue : c'est cet état `ATTRIBUEE` qui matérialise l'attente visible du formateur exigée par `Q11` et `RG23`.
- **Le statut de l'exercice est dérivé mais stocké.** `EN_ATTENTE_RELECTURE`, `RELU` et `NON_ATTRIBUABLE` pourraient se calculer, mais `NON_ATTRIBUABLE` n'est pas déductible après coup — l'absence de relecteur éligible est un fait daté de la clôture (`RG17`).
- **`relectures` porte `session_id`, en connaissance de cause.** La colonne est redondante — elle se déduit de `exercices.session_id` — mais sans elle `RG16` n'est exprimable par aucune contrainte : `UNIQUE (exercice_id, relecteur_id)` ne contraint rien de plus que `UNIQUE (exercice_id)`, qui existe déjà. La dénormalisation ne peut pas se désynchroniser : la clé étrangère composite `(exercice_id, session_id)` vers `exercices (id, session_id)` interdit à la base d'accepter une relecture dont la session contredirait celle de son exercice. Une règle tenue par le schéma survit à un bogue du service ; une règle tenue par le service seul, non.
- **`tentatives_presence` est une table à part.** Compter les échecs récents suppose de les conserver ; un compteur sur `etudiants` ne permettrait pas la remise à zéro par fenêtre glissante de `RG6`, ni un test reproductible.
- **`timestamptz` partout.** L'expiration à quinze minutes (`RG1`) doit être insensible au fuseau du serveur.

**Correspondance migrations :** `backend/src/main/resources/db/migration/` — `V1__schema_initial.sql` crée les sept tables, les contraintes et les index ci-dessus ; `V2__donnees_demonstration.sql` charge le jeu de démonstration, séparé du schéma.
