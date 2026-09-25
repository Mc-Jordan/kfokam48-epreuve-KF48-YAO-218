# Journal des modifications

Format [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).
Versionnage : `v0.1` à l'issue de l'étape 2, `v1.0` à l'issue de l'étape 4.

## [Non publié]

Rien pour l'instant. La suite dépend de l'enveloppe de l'étape 3.

## [0.1] — 2026-09-25

Première version. Le cycle complet d'une séance fonctionne de bout en bout :
ouverture, présence, dépôt, attribution des relecteurs, relecture, suivi.

### Ajouté

**Séances**
- Ouverture d'une séance par le formateur, avec production d'un code de présence valable quinze minutes (`EF1`, `RG1`, `RG2`)
- Clôture d'une séance : fermeture des dépôts et attribution automatique des relecteurs (`EF7`, `RG13`)
- Finalisation d'une séance : figement définitif des relectures (`EF10`, `RG21`)
- Consultation des séances d'une promotion et de leur état

**Présences**
- Marquage de présence par l'étudiant à l'aide du code (`EF2`, `RG3`, `RG4`)
- Ajout manuel d'une présence par le formateur, tracé comme tel (`EF4`, `RG5`)
- Blocage de deux minutes après cinq codes erronés consécutifs (`EF3`, `RG6`)
- Consultation du détail des présences d'une séance, source comprise

**Exercices**
- Dépôt du lien d'un exercice par un étudiant présent (`EF5`, `RG7`, `RG8`, `RG9`)
- Remplacement du lien tant que la séance est ouverte (`EF6`, `RG10`)

**Relectures**
- Attribution automatique d'un relecteur par exercice, tiré au sort parmi les présents, l'auteur exclu (`RG14`, `RG16`, `RG19`)
- État `NON_ATTRIBUABLE` lorsqu'aucun relecteur éligible n'existe (`RG17`)
- Consultation par le relecteur des exercices qui lui incombent (`EF8`)
- Rendu d'une note entière sur 20 et d'un commentaire, corrigeable jusqu'à la finalisation (`EF9`, `RG18`, `RG20`)
- Consultation par l'auteur de la note reçue, sans l'identité du relecteur (`EF11`, `RG22`)

**Suivi**
- Tableau récapitulatif par étudiant : présences, exercices déposés, moyenne reçue, relectures dues (`EF12`, `RG23`, `RG24`)

**Interface**
- Trois écrans : formateur, étudiant, relecteur (`F2`)
- Couche d'accès à l'API unique, états de chargement et d'erreur mutualisés (`F3`)

**Socle**
- Schéma versionné par Flyway, conforme au diagramme `D2`
- Gestion centralisée des erreurs : vingt-quatre codes stables, format `{ code, message }` sur tous les chemins (`B4`, `RG25`)
- Conteneurisation complète, démarrage par `docker compose up` (`ENF5`)
- Intégration continue, analyse SonarCloud avec porte de qualité bloquante
- Jeu de données de démonstration couvrant les cas limites

### Corrigé

- Une note non entière était silencieusement tronquée — `15.5` devenait `15` et l'API répondait `200`. Le contrat impose `400 NOTE_INVALIDE` pour une note « hors 0–20 ou non entière »
- `GET /api/relectures` répondait `500` en intégration continue : chargement paresseux traversé hors transaction. Le test passait en local et échouait sur le runner, l'ordre d'exécution suffisant à masquer le défaut
- `src/test/resources/application.properties` masquait entièrement celui de production au lieu de s'y ajouter : les tests ne validaient pas la configuration réelle

### Analyse corrigée

- `uq_relectures_session_relecteur` ne contraignait rien : défini sur `(exercice_id, relecteur_id)`, il n'ajoutait rien au `UNIQUE (exercice_id)` déjà présent, et `RG16` n'était garantie nulle part. `relectures` porte désormais `session_id`, verrouillé par une clé étrangère composite
- `ck_exercices_statut` et `ck_relectures_statut`, cités par `D4`, manquaient à la table des contraintes de `D2`

### Connu, et assumé

- Aucune authentification : l'étudiant choisit son nom dans une liste (`Q1`). N'importe qui peut agir au nom de n'importe qui — le dispositif est un outil de séance, pas un registre opposable
- La promotion est fixée en constante côté frontend : son choix relève d'un écran d'administration explicitement hors périmètre
- L'étudiant désigne son exercice par son numéro, affiché au dépôt : sans authentification, il n'existe pas d'autre moyen de le retrouver

[Non publié]: https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/compare/v0.1...HEAD
[0.1]: https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/releases/tag/v0.1
