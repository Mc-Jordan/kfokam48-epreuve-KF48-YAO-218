# Matrice de traçabilité

Chaque exigence se relie à ses règles de gestion, aux opérations d'API qui la portent,
au ticket qui la livre et au diagramme qui la décrit. **Une ligne incomplète signale un
manque réel** : elle est traitée, pas laissée en l'état.

Références : `EFx` et `RGx` renvoient à [`CAHIER_DES_CHARGES.md`](CAHIER_DES_CHARGES.md),
les opérations à [`../api/contrat.yaml`](../api/contrat.yaml), les numéros `#n` aux issues
du dépôt.

## Exigences fonctionnelles

| Exigence | Règles appliquées | Opérations du contrat | Issue | Diagrammes | Priorité |
|---|---|---|---|---|---|
| `EF1` — ouvrir une session, obtenir un code | `RG1`, `RG2`, `RG12`, `RG25` | `POST /api/sessions` *(imposée)* · `GET /api/sessions` | [#3](../../issues/3) | D1 (UC1) | Must |
| `EF2` — marquer sa présence | `RG1`, `RG2`, `RG3`, `RG4`, `RG25` | `POST /api/presences` *(imposée)* · `GET /api/promotions/{id}/etudiants` | [#4](../../issues/4) | D1 (UC2, UC4), **D3** | Must |
| `EF3` — limiter les tentatives | `RG6` | `POST /api/presences` → `429 TROP_DE_TENTATIVES` | [#14](../../issues/14) | **D3** | Should |
| `EF4` — présence ajoutée par le formateur | `RG3`, `RG4`, `RG5` | `POST /api/presences/manuelles` · `GET /api/sessions/{id}/presences` | [#10](../../issues/10) | D1 (UC3) | Should |
| `EF5` — déposer le lien de son exercice | `RG7`, `RG8`, `RG9`, `RG11` | `POST /api/exercices` *(imposée)* → `403 NON_PRESENT` | [#5](../../issues/5) | D1 (UC5), **D4** | Must |
| `EF6` — remplacer le lien | `RG9`, `RG10` | `PUT /api/exercices/{id}` | [#11](../../issues/11) | D1 (UC6), **D4** | Should |
| `EF7` — clôturer et attribuer les relecteurs | `RG13`, `RG14`, `RG16`, `RG17`, `RG19` | `POST /api/sessions/{id}/cloture` | [#6](../../issues/6) | D1 (UC7), **D4** | Must |
| `EF8` — consulter les relectures qui m'incombent | `RG19`, `RG23` | `GET /api/relectures?relecteurId=` | [#7](../../issues/7) | D1 (UC8) | Must |
| `EF9` — rendre et corriger une relecture | `RG18`, `RG19`, `RG20`, `RG21` | `POST /api/relectures/{id}` *(imposée)* | [#8](../../issues/8) | D1 (UC9), **D4** | Must |
| `EF10` — finaliser et figer les relectures | `RG12`, `RG21` | `POST /api/sessions/{id}/finalisation` | [#12](../../issues/12) | D1 (UC10), **D4** | Should |
| `EF11` — consulter la note reçue, sans le relecteur | `RG17`, `RG22`, `RG23` | `GET /api/exercices/{id}/relecture` | [#13](../../issues/13) | D1 (UC11), **D4** | Should |
| `EF12` — tableau récapitulatif | `RG23`, `RG24` | `GET /api/tableau` *(imposée)* · `GET /api/sessions/{id}/presences` | [#9](../../issues/9) | D1 (UC12, UC13) | Must |

## Couverture des règles de gestion

Toute règle doit être portée par au moins une opération **et** vérifiée par au moins un test.

| Règle | Portée par | Vérifiée par | Issue |
|---|---|---|---|
| `RG1` expiration à 15 min | `POST /api/presences` → `410` | test d'intégration `410 CODE_EXPIRE` | #4 |
| `RG2` unicité du code | contrainte `uq_sessions_code` | test d'intégration `400 CODE_INCONNU` | #3, #4 |
| `RG3` présence unique par session | contrainte `uq_presences_session_etudiant` | test d'intégration `409 DEJA_PRESENT` | #4, #10 |
| `RG4` source de la présence | champ `source` du contrat imposé | test d'intégration sur les deux valeurs | #4, #10 |
| `RG5` présence manuelle sans code | `POST /api/presences/manuelles` | test : ajout après expiration du code | #10 |
| `RG6` blocage après 5 échecs | `POST /api/presences` → `429` | **test unitaire**, horloge injectable | #14 |
| `RG7` un exercice par session | contrainte `uq_exercices_session_etudiant` | test d'intégration `409 EXERCICE_DEJA_DEPOSE` | #5 |
| `RG8` présence exigée au dépôt | `POST /api/exercices` → `403` | **test unitaire** | #5 |
| `RG9` lien absolu http(s) | validation d'entrée | test d'intégration `400 LIEN_INVALIDE` | #5, #11 |
| `RG10` remplacement avant clôture | `PUT /api/exercices/{id}` | test : `200` avant, `409` après | #11 |
| `RG11` dépôt après expiration du code | `POST /api/exercices` | test : dépôt à la 20e minute | #5 |
| `RG12` trois états de session | contrainte `ck_sessions_statut` | test : transitions et refus | #6, #12 |
| `RG13` la clôture ferme les dépôts | `POST /api/sessions/{id}/cloture` | test : dépôt refusé après clôture | #6 |
| `RG14` tirage parmi les présents | service d'attribution | **test unitaire** | #6 |
| `RG15` un relecteur par exercice | contrainte `uq_relectures_exercice` | test d'intégration | #6 |
| `RG16` au plus une relecture par étudiant | service d'attribution | **test unitaire**, cas 3 présents 3 exercices | #6 |
| `RG17` `NON_ATTRIBUABLE` | statut de l'exercice | **test unitaire**, cas à 1 présent | #6 |
| `RG18` note entière 0–20 | contrainte `ck_relectures_note` + validation | test d'intégration `400 NOTE_INVALIDE` | #8 |
| `RG19` jamais son propre exercice | service, vérifié à l'attribution et au rendu | **test unitaire** + `403 AUTO_RELECTURE` | #6, #8 |
| `RG20` relecture modifiable avant finalisation | `POST /api/relectures/{id}` → `200` | test : deux envois successifs | #8 |
| `RG21` figement à la finalisation | `POST /api/sessions/{id}/finalisation` | test : `409` après finalisation | #8, #12 |
| `RG22` anonymat du relecteur | schéma `RelectureRecue` du contrat | test : aucun identifiant dans la réponse | #13 |
| `RG23` attente visible | statut `EN_ATTENTE_RELECTURE`, champ `relecturesEnAttente` | test d'intégration sur le tableau | #9, #13 |
| `RG24` moyenne calculée par le serveur | `GET /api/tableau` | test : moyenne vide sans note | #9 |
| `RG25` format d'erreur imposé | `@RestControllerAdvice` | test parcourant les 21 codes | #16 |

**Vingt-cinq règles, vingt-cinq lignes portées et vérifiées. Aucune ligne incomplète.**

## Exigences non fonctionnelles

| Exigence | Vérifiée par | Issue |
|---|---|---|
| `ENF1` utilisable sur téléphone | revue manuelle à 360 px | #4 |
| `ENF2` tableau sous 2 s pour 60 étudiants | mesure sur le jeu de démonstration | #9, #15 |
| `ENF3` format d'erreur sur tous les chemins | test parcourant les 21 codes | #16 |
| `ENF4` volumétrie d'une année | index posés en `V1__schema_initial.sql` | #2 |
| `ENF5` démarrage depuis un clone vierge | procédure rejouée dans un dossier vide | #15, #17 |
| `ENF6` schéma versionné et rejouable | Flyway, `ddl-auto=validate` | #2 |
| `ENF7` deux tests sur poste vierge | `./mvnw verify` avec Testcontainers | #2 |
| `ENF8` absence d'authentification assumée | §3 et §7 du cahier des charges | #1 |

## Couverture inverse des opérations du contrat

Aucune opération ne doit exister sans exigence qui la réclame.

| Opération | Exigence | Statut |
|---|---|---|
| `POST /api/sessions` | `EF1` | imposée |
| `GET /api/sessions` | `EF1`, `EF5`, `EF12` | ajoutée |
| `POST /api/presences` | `EF2`, `EF3` | imposée |
| `POST /api/exercices` | `EF5` | imposée |
| `POST /api/relectures/{id}` | `EF9` | imposée |
| `GET /api/tableau` | `EF12` | imposée |
| `GET /api/promotions/{id}/etudiants` | `EF2` | ajoutée |
| `POST /api/sessions/{id}/cloture` | `EF7` | ajoutée |
| `POST /api/sessions/{id}/finalisation` | `EF10` | ajoutée |
| `GET /api/sessions/{id}/presences` | `EF4`, `EF12` | ajoutée |
| `POST /api/presences/manuelles` | `EF4` | ajoutée |
| `PUT /api/exercices/{id}` | `EF6` | ajoutée |
| `GET /api/exercices/{id}/relecture` | `EF11` | ajoutée |
| `GET /api/relectures` | `EF8` | ajoutée |

**Treize chemins, quatorze opérations. Aucune sans exigence.**
