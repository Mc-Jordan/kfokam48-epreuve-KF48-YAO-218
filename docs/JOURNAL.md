# Journal de bord — KF48-YAO-218

**NANDJO NGOULE Michele Jordan** · Centre de Yaounde

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges en dix sections — 12 exigences fonctionnelles, 8 exigences non fonctionnelles, 25 règles de gestion, toutes sourcées par un `Qx` ou signalées comme hypothèse. Les quatre diagrammes en Mermaid, D4 compris (bonus). Contrat d'API complété de 9 opérations et figé. 18 issues créées avec critères d'acceptation, priorité et renvoi aux `EFx` / `RGx`. Matrice de traçabilité dans `docs/TRACABILITE.md`. Commit `[JALON] analyse` poussé.

**Bloqué :** *(à compléter — durée réelle)*. Le point coûteux n'a pas été la contradiction `Q10` / `Q15`, qui se tranche vite, mais une **collision entre deux de mes propres arbitrages** : j'avais décidé que le tirage des relecteurs aurait lieu à la clôture, et que la relecture resterait modifiable « tant que la session n'est pas clôturée ». Les deux sont incompatibles — si les relectures naissent à la clôture, elles ne peuvent pas être modifiables avant elle. C'est en construisant D4 que la contradiction est apparue : le diagramme n'avait pas de transition possible. Résolu en séparant deux actes que le client désigne du même mot « clôturer » : la **clôture** ferme les dépôts et déclenche le tirage, la **finalisation** fige les relectures. `Q15` n'est pas abandonnée, elle est déplacée.

**IA :** *(à compléter — ce que j'ai demandé, dans mes mots)*. Ce que j'ai vérifié, et comment :

- **Le rendu des diagrammes**, plutôt que de faire confiance à la syntaxe proposée : les quatre blocs Mermaid ont été extraits et rendus avec `mmdc` en local. 4 sur 4 produisent un SVG. Les SVG ne sont pas versionnés — le sujet impose du texte diffable.
- **L'intégrité du contrat imposé**, parce que c'est le point où une erreur ne se rattrape pas : `diff` entre le contrat d'origine et le mien — zéro ligne retirée — puis comparaison sémantique opération par opération en Python, qui confirme que les cinq opérations imposées gardent leurs corps de requête, leurs paramètres et leurs codes de statut à l'identique. Deux codes sont ajoutés, `429` et `403`, tous deux justifiés en section 7.
- **La validité du YAML**, par `redocly lint` et par `yaml.safe_load`. Le premier jet contenait **une clé `/api/sessions` en double** : ma nouvelle opération `GET` aurait silencieusement écrasé le `POST` imposé, et `safe_load` ne l'aurait pas signalé. Détecté par un comptage explicite des clés de chemin, corrigé en fusionnant le verbe dans le bloc existant. C'est l'erreur la plus grave de la session, et elle était invisible à la relecture.
- **Les 14 erreurs restantes du lint** : plutôt que de les corriger, j'ai soumis le contrat imposé **seul** au même lint. Il en produit 5 — une par opération. La règle `security-defined` est donc incompatible avec l'absence d'authentification voulue par `Q1`, pas avec mon travail.
- **La cohérence des références**, par la matrice de traçabilité : chaque `RGx` doit apparaître dans au moins une opération et un test, chaque opération doit avoir une exigence. La table de couverture inverse a été écrite pour cela, et non pour décorer.

**Ce que je n'ai pas vérifié** *(à compléter ou à corriger)* : l'estimation « moins de deux secondes » de `ENF2` n'est qu'une cible, elle ne sera mesurée qu'à l'étape 2 sur le jeu de démonstration.

---

## Étape 2 — Première version

**Fait :** les douze exigences fonctionnelles sont livrées, pas seulement les sept `Must`. Quatorze pull requests fusionnées en `--no-ff`, seize issues fermées, cinquante-neuf commits sur `main`. Les vingt-cinq règles de gestion sont portées par du code et couvertes par des tests : 36 méthodes de test unitaire et 82 d'intégration, réparties en dix-sept classes. Le contrat est couvert intégralement — quatorze opérations déclarées, quatorze servies, aucune route hors contrat, vérifié par un script qui croise `contrat.yaml` et les contrôleurs. Socle conteneurisé, intégration continue verte sur quatre workflows, porte de qualité SonarCloud franchie.

**Bloqué :** *(à compléter — durée réelle)*. Trois blocages valent d'être notés, et aucun n'était fonctionnel :

1. **`JAVA_HOME` pointait vers un JRE sans compilateur.** Maven échouait sur « release version 21 not supported » alors que `javac -version` répondait 21. Le `java` du `PATH` venait d'un paquet JRE, le `javac` d'un autre JDK. Diagnostic long parce que le message d'erreur désigne la version, pas l'absence de compilateur.
2. **Testcontainers 1.x est incompatible avec Docker 29.** Le client Docker embarqué négocie en API 1.32, que le moteur refuse désormais. Ni la variable d'environnement `DOCKER_API_VERSION` ni la configuration Surefire n'y changent rien : la négociation a lieu avant la lecture de la configuration. Résolu en passant à Testcontainers 2.0.5, piloté par `@DynamicPropertySource` plutôt que par `spring-boot-testcontainers` — ce qui supprime au passage tout arrimage entre les versions de Spring Boot et de Testcontainers.
3. **SonarCloud, deux fois.** D'abord l'organisation, que j'avais devinée `mc-jordan` au lieu de `mcjordan` ; relevée sur l'API plutôt que devinée une seconde fois. Ensuite une impasse : l'analyse d'une pull request exige que la branche cible ait déjà été analysée, or `main` ne l'est qu'au premier push, qui n'arrive que par fusion — bloquée par Sonar. Sortie en retirant temporairement Sonar des vérifications obligatoires, le temps d'une fusion.

**IA :** *(à compléter — ce que j'ai demandé, dans mes mots)*. Ce que j'ai vérifié, et ce que la vérification a trouvé :

- **J'ai fait tourner les tests plutôt que de les lire.** Trois défauts réels en sont sortis, qu'aucune relecture n'aurait attrapés :
  - une note de `15.5` était **silencieusement tronquée en 15** et l'API répondait `200`, alors que le contrat impose `400` pour une note « non entière ». La note est désormais reçue en `BigDecimal` et son intégralité vérifiée explicitement — j'avais d'abord essayé un réglage Jackson, sans effet, et un test qui dépend d'un réglage invisible est un test fragile ;
  - `GET /api/relectures` répondait `500` **en intégration continue seulement** : chargement paresseux traversé hors transaction, que l'ordre d'exécution des tests suffisait à masquer en local. Corrigé par des jointures explicites ;
  - `src/test/resources/application.properties` **remplaçait** celui de production au lieu de s'y ajouter. Les tests ne validaient donc pas la configuration réelle. Fichier supprimé.
- **J'ai démarré depuis un clone vierge**, et pas seulement relu le `README`. Le conteneur frontend restait `unhealthy` alors qu'il répondait à toutes les requêtes : dans le conteneur, `localhost` résout d'abord en IPv6, or nginx n'écoute qu'en IPv4. Invisible de l'extérieur, invisible à la lecture.
- **J'ai refusé deux propositions structurantes.** Une affectation gloutonne des relecteurs — « prendre au hasard un relecteur libre qui ne soit pas l'auteur » — se bloque sur le dernier exercice et impose de recommencer sans garantie de terminaison. La rotation circulaire évite le problème au lieu de le traiter, et rend les trois règles vraies par construction. J'ai aussi retiré un `ReflectionTestUtils` qui s'était glissé dans du code de production pour forcer un changement d'état : les transitions appartiennent à l'entité.
- **J'ai confronté chaque règle au contrat.** C'est ce croisement, et non l'intuition, qui a révélé que trois opérations déclarées — le détail des présences, la présence manuelle, le remplacement du lien — n'avaient aucune implémentation. Une opération spécifiée qui répond `404` est un défaut de conformité, pas une fonctionnalité à venir.
- **Deux tests de cohérence tournent à chaque `verify`** et me protègent de moi-même : l'un compare le diagramme `D2` aux migrations dans les deux sens, l'autre le registre d'erreurs du contrat à l'énumération. Ils ont déjà servi — écrire le SQL a révélé que `uq_relectures_session_relecteur` ne contraignait rien, et que `RG16` n'était donc garantie nulle part.

**Ce que je n'ai pas fait :** aucun test de bout en bout sur l'interface. Les trois écrans ont été vérifiés à la main et par le parcours complet en HTTP, pas par un test automatisé. C'est un choix de temps assumé : le sujet ne note pas le rendu visuel, et la logique métier est entièrement côté serveur.

---

## Étape 3 — Enveloppe

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
