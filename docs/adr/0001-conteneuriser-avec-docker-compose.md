# ADR 0001 — Conteneuriser l'application et la démarrer par Docker Compose

**Statut :** accepté
**Date :** 2026-09-25
**Décideur :** KF48-YAO-218

## Contexte

Le sujet impose que l'application démarre chez un tiers « par `docker compose up`, ou trois commandes maximum documentées dans le `README` et testées depuis un clone vierge », avec des données de démonstration. Trois points du barème en dépendent, et `ENF5` en fait une exigence non fonctionnelle vérifiable.

Le produit compte trois composants — une base PostgreSQL, un service Spring Boot, une application React servie en statique. Les faire installer un à un par le correcteur suppose qu'il dispose du bon JDK, du bon Node et d'un PostgreSQL local : trois occasions d'échouer avant d'avoir vu la première page.

## Décision

Nous conteneurisons les trois composants et nous faisons de `docker compose up` la commande unique de démarrage.

- Chaque service a un `Dockerfile` **multi-étapes** : les outils de construction ne se retrouvent pas dans l'image d'exécution.
- L'étape de build du backend résout les dépendances Maven **avant** de copier les sources, pour que cette couche reste en cache tant que le `pom.xml` ne change pas.
- Les deux images tournent sous un **utilisateur non privilégié** et déclarent un `HEALTHCHECK`.
- Le backend ne démarre qu'une fois la base réellement prête, via `depends_on: condition: service_healthy` — attendre le démarrage du conteneur ne suffit pas, PostgreSQL accepte les connexions après.
- Le frontend est servi par nginx, qui relaie `/api` vers le backend : le navigateur ne connaît qu'une origine, et la question de CORS ne se pose pas.
- Les identifiants viennent d'un `.env` non versionné, dont `.env.example` donne le modèle. **Aucun secret n'entre dans l'historique.**

## Options écartées

| Option | Pourquoi écartée |
|---|---|
| Trois commandes documentées, sans conteneur | Suppose un JDK 21, un Node 22 et un PostgreSQL correctement configurés chez le correcteur. Le sujet l'autorise, mais chaque prérequis est une occasion d'échouer, et l'échec se paie sur trois points non rattrapables |
| Une image unique contenant les trois composants | Contraire à la séparation des responsabilités, rend le cache de construction inopérant, et interdit de redémarrer un service sans les autres |
| Base de données embarquée (H2) pour simplifier | Le SQL de H2 diffère de celui de PostgreSQL. Le schéma serait validé sur un moteur qui n'est pas celui de production, et l'étape 3 — qui touchera la base — se ferait à l'aveugle |
| Servir le frontend par Spring Boot en ressources statiques | Couple le cycle de vie des deux applications : une correction de style imposerait de reconstruire le backend |

## Conséquences

**Ce que cela rend facile.** Un correcteur clone et lance une commande. Les mêmes images servent en intégration continue et sont publiées sur `ghcr.io` à chaque étiquette de version. Le développement local n'exige aucune installation de base.

**Ce que cela rend difficile.** Docker devient un prérequis — mais il l'était déjà, les tests d'intégration s'appuyant sur Testcontainers (`ENF7`). La première construction est longue ; les suivantes profitent du cache.

**Ce qu'il faudra revoir.** À l'étape 3, un changement de schéma n'impose rien ici : Flyway s'applique au démarrage du backend. En revanche, si le changement de l'étape 3 ajoute un composant, il faudra l'ajouter à la composition et à la chaîne d'intégration.
