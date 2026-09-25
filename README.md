# KFOKAM48 — présence, exercices et relecture par les pairs

Application de suivi de formation : un formateur ouvre une session et obtient un code de
présence, les étudiants marquent leur présence puis déposent le lien de leur exercice,
le système assigne chaque exercice à un pair pour relecture, et le formateur suit
l'ensemble dans un tableau récapitulatif.

**Épreuve finale fullstack KFOKAM48** · NANDJO NGOULE Michele Jordan · **KF48-YAO-218** · Centre de Yaoundé

[![Intégration continue](https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/actions/workflows/ci.yml/badge.svg)](https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/actions/workflows/ci.yml)
[![Qualité](https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/actions/workflows/sonar.yml/badge.svg)](https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/actions/workflows/sonar.yml)

---

## Pile technique

| Couche | Choix |
|---|---|
| Backend | Java 17+, Spring Boot, Maven (wrapper `mvnw` commité) |
| Base de données | PostgreSQL, schéma versionné par Flyway |
| Frontend | React + Vite + TypeScript |
| Conteneurisation | Docker et Docker Compose |
| Intégration continue | GitHub Actions, analyse SonarCloud |

**Pourquoi React + Vite + TypeScript :** le besoin est une application à trois écrans sans
rendu côté serveur ni référencement ; Vite donne le démarrage et le build les plus rapides,
TypeScript rend le contrat d'API vérifiable à la compilation, et l'ensemble se sert en
production par un simple conteneur de fichiers statiques.

## Structure du dépôt

```
docs/        cahier des charges, journal de bord, diagrammes UML, décisions d'architecture
api/         contrat.yaml — contrat d'API imposé, complété puis figé avant le premier code
backend/     service Spring Boot
frontend/    application React
.github/     gabarits d'issue et de pull request, intégration continue
```

## Installation depuis un clone vierge

**Prérequis : Docker et Docker Compose.** Rien d'autre — ni JDK, ni Node, ni PostgreSQL.

```bash
git clone https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218.git
cd kfokam48-epreuve-KF48-YAO-218
cp .env.example .env
docker compose up
```

Trois commandes après le clone. L'application est ensuite disponible sur :

| | |
|---|---|
| Interface | <http://localhost:8081> |
| API | <http://localhost:8080/api> |
| État du service | <http://localhost:8080/actuator/health> |

Pour tout arrêter et repartir d'une base vide : `docker compose down -v`.

> `.env` n'est pas versionné. `.env.example` fournit des valeurs de démonstration ;
> changez-les pour tout autre usage.

## Développement sans Docker

Utile pour itérer, mais ce n'est pas le chemin documenté pour un tiers.

```bash
# Base seule
docker compose up db

# Backend — exige un JDK 21 et un JAVA_HOME qui pointe vers un JDK, pas un JRE
cd backend
DB_URL=jdbc:postgresql://localhost:5432/presences DB_USER=presences DB_PASSWORD=... ./mvnw spring-boot:run

# Frontend — exige Node 22 ; /api est relayé vers le port 8080
cd frontend && npm ci && npm run dev
```

## Tests

```bash
cd backend && ./mvnw verify
```

`verify` enchaîne les tests unitaires puis les tests d'intégration, qui démarrent un
PostgreSQL par Testcontainers. **Aucune base locale n'est requise**, mais Docker doit
être disponible.

## Jeu de données de démonstration

Il se charge tout seul, par une migration Flyway. Il vise les **cas limites**, parce
qu'un jeu qui ne montrerait que le cas nominal laisserait plusieurs règles
invérifiables à l'écran.

Ouvrez <http://localhost:8081/formateur> et vous verrez :

| Ce qui est visible | Pourquoi c'est là |
|---|---|
| Trois séances, une par état — `OUVERTE`, `CLOTUREE`, `FINALISEE` | Le cycle de vie complet |
| Un exercice `NON_ATTRIBUABLE` (Gaston NKOLO) | `RG17` — aucun relecteur éligible à la clôture |
| Une relecture attribuée jamais rendue | `RG23` — le relecteur défaillant de `Q11` |
| Une présence « ajoutée par le formateur » (Francine MBALLA) | `RG4` et `Q14` |
| Un étudiant sans aucune note (Hortense ATANGANA) | `RG24` — moyenne vide, qui n'est pas un zéro |
| Une séance ouverte au code encore valable | Pour que vous puissiez agir vous-même |

**Pour essayer le parcours complet :** le code de la séance ouverte s'obtient en
ouvrant une nouvelle séance depuis l'écran formateur, puis en le saisissant depuis
l'écran étudiant.

## Ce que fait l'application

| Acteur | Ce qu'il peut faire |
|---|---|
| **Formateur** | Ouvrir une séance et obtenir son code · ajouter une présence à la main · clôturer, ce qui ferme les dépôts et attribue les relecteurs · finaliser, ce qui fige les relectures · suivre la promotion |
| **Étudiant** | Marquer sa présence · déposer le lien de son exercice · remplacer ce lien tant que la séance est ouverte · consulter la note reçue |
| **Relecteur** | Consulter les exercices qui lui sont attribués · rendre une note et un commentaire · les corriger jusqu'à la finalisation |

Le relecteur n'est pas un acteur distinct : c'est un étudiant à qui une relecture a
été attribuée.

## Limites connues

- **Aucune authentification.** L'étudiant choisit son nom dans une liste (`Q1`).
  N'importe qui peut agir au nom de n'importe qui : le dispositif est un outil de
  séance, pas un registre opposable. C'est une décision, écrite au §3 du cahier des
  charges, pas un oubli.
- **La promotion est fixée** côté interface. Son choix relève d'un écran
  d'administration explicitement hors périmètre.
- **L'étudiant désigne son exercice par son numéro**, affiché au moment du dépôt.
  Sans authentification, il n'existe pas d'autre moyen de le retrouver.

## Documentation

| Document | Contenu |
|---|---|
| [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md) | Exigences `EFx`, règles de gestion `RGx`, contradictions tranchées |
| [`docs/diagrammes/`](docs/diagrammes) | D1 cas d'utilisation · D2 modèle de données · D3 séquence · D4 états |
| [`docs/JOURNAL.md`](docs/JOURNAL.md) | Journal de bord, une entrée par étape |
| [`api/contrat.yaml`](api/contrat.yaml) | Contrat d'API — cinq opérations imposées |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Conventions de commit, de branche et de pull request |
| [`CHANGELOG.md`](CHANGELOG.md) | Journal des modifications |

## État d'avancement

| Étape | État |
|---|---|
| 0 — Environnement | fait |
| 1 — Analyse et conception | fait — jalon `[JALON] analyse` |
| 2 — Première version | **fait** — jalon `[JALON] v0.1`, étiquette [`v0.1`](https://github.com/Mc-Jordan/kfokam48-epreuve-KF48-YAO-218/releases/tag/v0.1) |
| 3 — Enveloppe | à venir — l'enveloppe se demande au surveillant, le jalon `v0.1` étant poussé |
| 4 — Version finale `v1.0` | à venir |

Les douze exigences fonctionnelles sont livrées, et les vingt-cinq règles de gestion
sont portées par du code et couvertes par des tests. Le détail est dans le
[`CHANGELOG`](CHANGELOG.md), la correspondance exigence par exigence dans
[`docs/TRACABILITE.md`](docs/TRACABILITE.md).
