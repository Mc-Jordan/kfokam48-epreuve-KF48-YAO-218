# KFOKAM48 — présence, exercices et relecture par les pairs

Application de suivi de formation : un formateur ouvre une session et obtient un code de
présence, les étudiants marquent leur présence puis déposent le lien de leur exercice,
le système assigne chaque exercice à un pair pour relecture, et le formateur suit
l'ensemble dans un tableau récapitulatif.

**Épreuve finale fullstack KFOKAM48** · NANDJO NGOULE Michele Jordan · **KF48-YAO-218** · Centre de Yaoundé

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

<!-- à compléter à l'étape 4 -->

## Démarrage

<!-- à compléter à l'étape 4 -->

## Jeu de données de démonstration

<!-- à compléter à l'étape 4 -->

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
| 1 — Analyse et conception | en cours |
| 2 — Première version `v0.1` | à venir |
| 3 — Enveloppe | à venir |
| 4 — Version finale `v1.0` | à venir |
