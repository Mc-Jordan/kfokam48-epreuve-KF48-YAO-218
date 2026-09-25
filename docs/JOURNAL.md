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

**Bloqué :** 39 minutes pour toute l'étape, dont une bonne moitié sur un seul point. Ce n'est pas la contradiction `Q10` / `Q15` qui a coûté — elle se tranche vite — mais une **collision entre deux de mes propres arbitrages** : j'avais décidé que le tirage des relecteurs aurait lieu à la clôture, et que la relecture resterait modifiable « tant que la session n'est pas clôturée ». Les deux sont incompatibles — si les relectures naissent à la clôture, elles ne peuvent pas être modifiables avant elle. C'est en construisant D4 que la contradiction est apparue : le diagramme n'avait pas de transition possible. Résolu en séparant deux actes que le client désigne du même mot « clôturer » : la **clôture** ferme les dépôts et déclenche le tirage, la **finalisation** fige les relectures. `Q15` n'est pas abandonnée, elle est déplacée.

**IA :** je lui ai demandé de dépouiller les seize réponses du client, de repérer ce qui se contredit et ce qui manque, puis de me proposer deux options chiffrées pour chaque point à trancher — pas de trancher à ma place. J'ai gardé la main sur les arbitrages, parce que c'est dessus que la section 7 est notée et que c'est la partie qui m'engage. Ce que j'ai vérifié, et comment :

- **Le rendu des diagrammes**, plutôt que de faire confiance à la syntaxe proposée : les quatre blocs Mermaid ont été extraits et rendus avec `mmdc` en local. 4 sur 4 produisent un SVG. Les SVG ne sont pas versionnés — le sujet impose du texte diffable.
- **L'intégrité du contrat imposé**, parce que c'est le point où une erreur ne se rattrape pas : `diff` entre le contrat d'origine et le mien — zéro ligne retirée — puis comparaison sémantique opération par opération en Python, qui confirme que les cinq opérations imposées gardent leurs corps de requête, leurs paramètres et leurs codes de statut à l'identique. Deux codes sont ajoutés, `429` et `403`, tous deux justifiés en section 7.
- **La validité du YAML**, par `redocly lint` et par `yaml.safe_load`. Le premier jet contenait **une clé `/api/sessions` en double** : ma nouvelle opération `GET` aurait silencieusement écrasé le `POST` imposé, et `safe_load` ne l'aurait pas signalé. Détecté par un comptage explicite des clés de chemin, corrigé en fusionnant le verbe dans le bloc existant. C'est l'erreur la plus grave de la session, et elle était invisible à la relecture.
- **Les 14 erreurs restantes du lint** : plutôt que de les corriger, j'ai soumis le contrat imposé **seul** au même lint. Il en produit 5 — une par opération. La règle `security-defined` est donc incompatible avec l'absence d'authentification voulue par `Q1`, pas avec mon travail.
- **La cohérence des références**, par la matrice de traçabilité : chaque `RGx` doit apparaître dans au moins une opération et un test, chaque opération doit avoir une exigence. La table de couverture inverse a été écrite pour cela, et non pour décorer.

**Ce que je n'ai pas vérifié :** l'estimation « moins de deux secondes » de `ENF2` n'est qu'une cible, elle ne sera mesurée qu'à l'étape 2 sur le jeu de démonstration.

---

## Étape 2 — Première version

**Fait :** les douze exigences fonctionnelles sont livrées, pas seulement les sept `Must`. Quatorze pull requests fusionnées en `--no-ff`, seize issues fermées, cinquante-neuf commits sur `main`. Les vingt-cinq règles de gestion sont portées par du code et couvertes par des tests : 36 méthodes de test unitaire et 82 d'intégration, réparties en dix-sept classes. Le contrat est couvert intégralement — quatorze opérations déclarées, quatorze servies, aucune route hors contrat, vérifié par un script qui croise `contrat.yaml` et les contrôleurs. Socle conteneurisé, intégration continue verte sur quatre workflows, porte de qualité SonarCloud franchie.

**Bloqué :** 4 h 20 pour l'étape entière. Trois blocages valent d'être notés, et aucun n'était fonctionnel — c'est l'outillage qui a coûté, jamais le métier. À vue de nez, une heure et demie sur les trois :

1. **`JAVA_HOME` pointait vers un JRE sans compilateur.** Maven échouait sur « release version 21 not supported » alors que `javac -version` répondait 21. Le `java` du `PATH` venait d'un paquet JRE, le `javac` d'un autre JDK. Diagnostic long parce que le message d'erreur désigne la version, pas l'absence de compilateur.
2. **Testcontainers 1.x est incompatible avec Docker 29.** Le client Docker embarqué négocie en API 1.32, que le moteur refuse désormais. Ni la variable d'environnement `DOCKER_API_VERSION` ni la configuration Surefire n'y changent rien : la négociation a lieu avant la lecture de la configuration. Résolu en passant à Testcontainers 2.0.5, piloté par `@DynamicPropertySource` plutôt que par `spring-boot-testcontainers` — ce qui supprime au passage tout arrimage entre les versions de Spring Boot et de Testcontainers.
3. **SonarCloud, deux fois.** D'abord l'organisation, que j'avais devinée `mc-jordan` au lieu de `mcjordan` ; relevée sur l'API plutôt que devinée une seconde fois. Ensuite une impasse : l'analyse d'une pull request exige que la branche cible ait déjà été analysée, or `main` ne l'est qu'au premier push, qui n'arrive que par fusion — bloquée par Sonar. Sortie en retirant temporairement Sonar des vérifications obligatoires, le temps d'une fusion.

**IA :** je lui ai demandé de livrer une issue à la fois, dans l'ordre que j'avais fixé, et de ne jamais enchaîner sans me montrer le résultat. Pour chaque ticket je lui demandais d'écrire le test avant le code, et de citer la règle `RGx` dans le nom du test — sinon je ne peux pas vérifier qu'elle est réellement couverte. Ce que la vérification a trouvé :

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

**Fait :** l'enveloppe apportait deux choses, traitées séparément — deux branches, deux séries de pull requests, comme elle l'exige. Le bug d'abord : issue #38, un test qui échoue poussé **avant** le correctif, puis le correctif. Le changement de besoin ensuite : issue parapluie #40, découpée en #41 analyse et schéma, #43 contrat et API, #44 frontend. Cinq pull requests, six issues fermées, deux migrations ajoutées — `V3`, `V4` — et une troisième pour le jeu de démonstration.

**Bloqué :** 1 h 41 pour les étapes 3 et 4 réunies. Le temps n'est pas parti où je l'attendais : le changement de besoin, que je croyais le gros morceau, a été plus rapide que le bug, que je croyais anecdotique.

Le bug d'abord. Le client décrit deux étudiants côte à côte dont un seul apparaît. J'ai mesuré avant de corriger, et la mesure a démenti la lecture évidente : **deux étudiants distincts passent toujours**, dix rafales sur dix, huit fils en parallèle. Le défaut est ailleurs — un même étudiant qui envoie deux fois reçoit `500 ERREUR_INTERNE` au lieu de `409`. Aucune présence n'est perdue, la contrainte de `V1` protège la donnée ; c'est la **réponse** qui ment, et c'est pour ça que l'incident est incompréhensible des deux côtés. Sans la sonde, j'aurais corrigé un problème qui n'existait pas.

Le découpage ensuite. J'avais prévu quatre issues pour le changement, j'en ai livré trois. `CoherenceD2MigrationTest` rend le diagramme `D2` et les migrations **indissociables** : mettre à jour l'un sans l'autre rend le build rouge. Les séparer imposait soit de fusionner une branche rouge, soit de désactiver le garde-fou. J'ai regroupé et écrit le motif dans les deux issues, plutôt que de le subir en silence.

**IA :** pour le bug, je lui ai demandé de **ne pas corriger** avant d'avoir écrit une sonde qui reproduise ce que le client décrit — et de me montrer la mesure. C'est cette consigne qui a payé : la mesure a démenti la lecture évidente. Pour le changement, je lui ai demandé de mettre à jour le cahier des charges et les diagrammes **avant** de toucher au schéma, et de me soumettre les trous que le client n'avait pas comblés au lieu de les combler seule. Ce que la vérification a rapporté, et qui n'aurait pas été vu autrement :

- **La moyenne d'un étudiant moyennait les notes brutes.** Depuis que les exercices reçoivent deux relectures, un exercice relu deux fois pesait **double** face à ceux des séances d'avant le changement. `RG24` dit que la note d'un exercice est la moyenne de ses relectures, et que la moyenne de l'étudiant porte sur ses notes d'exercice : deux agrégations imbriquées, que JPQL ne sait pas exprimer. Requête passée en natif, toujours en un seul appel pour tenir `ENF2`.
- **`exercicesAttribues` comptait les relectures.** Le formateur aurait vu dix exercices attribués là où il y en a cinq. Un test d'intégration l'a attrapé à la seconde où l'algorithme a changé.
- **Le test de cohérence s'est révélé faux deux fois de suite.** Il ignorait d'abord les suppressions de contraintes ; corrigé, il considérait ensuite une contrainte supprimée puis recréée sous le même nom comme absente — exactement ce que fait `V4` pour élargir un `CHECK`. J'avais documenté cette limite en la créant ; elle a mordu à la migration suivante. Il rejoue désormais les instructions dans l'ordre, comme Flyway le fera.
- **`D4` annonçait un statut que la contrainte n'autorisait pas.** Écart introduit par mon propre commit d'analyse, rattrapé par `V4`. Le garde-fou ne l'avait pas vu parce qu'il ne compare que les noms de contraintes, pas leur contenu — limite qui reste, et qui est maintenant écrite.
- **Le décor d'un test ne créait que deux étudiants présents**, donc un seul relecteur par `RG17` : les nouvelles règles n'y étaient pas observables. Les tests passaient sans rien prouver.
- **J'ai refusé une proposition.** L'IA plaçait la traduction des conflits de contrainte dans le seul `@RestControllerAdvice`. Elle n'aurait alors valu que pour les appels HTTP, et le test qui appelle le service directement serait resté rouge. La traduction vit au service ; le gestionnaire la garde en filet pour les violations qui surviennent au commit.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** **aucune reprise de l'existant.** Pas de réattribution rétroactive des séances déjà clôturées — le client écrit « à partir de maintenant », et réattribuer modifierait des moyennes déjà communiquées. Pas de réattribution manuelle par le formateur, qui aurait augmenté le périmètre alors qu'il faut le réduire. Pas de refonte de l'écran relecteur au-delà du minimum. Les douze exigences étant livrées à `v0.1`, le coût réel de ce changement était dans l'analyse, la migration et le contrat : c'est là que le temps est allé.

---

## Étape 4 — Version finale

**Fait :** journal, `CHANGELOG` section `[1.0]`, `README` **retesté depuis un clone vierge** après les trois migrations de l'étape 3. Jalon `[JALON] v1.0` poussé, étiquette annotée `v1.0` posée, Release GitHub créée depuis le `CHANGELOG`, images publiées sur `ghcr.io`. Backlog vide : vingt-six issues fermées, aucune ouverte.

**Bloqué :** rien. L'étape a tenu dans le temps de la recette, l'essentiel étant d'attendre que la pile se reconstruise depuis zéro.

**IA :** je lui ai demandé de **rejouer** la procédure du `README`, pas de la relire — clone neuf, dossier vide, base vierge. C'est la seule vérification du barème qui ne peut pas se faire sur pièces. Les cinq migrations s'appliquent, les trois services passent `healthy`, et j'ai vérifié à la main les deux apports de l'étape 3 : l'exercice 7 rend bien une note provisoire 1 sur 2, et six envois simultanés du même étudiant rendent `201` puis cinq `409`, aucun `500`. Le correctif tient donc en conditions réelles et pas seulement en test.

**Ce que le `CHANGELOG` dit et que je tiens à assumer :** une section « Hors périmètre » énumère ce que le changement de l'étape 3 n'a pas entraîné. Un journal des modifications qui ne listerait que les ajouts laisserait croire que tout a été fait.

---

## Étape 5 — Soumission

**Fait :** `SOUMISSION.md` rempli et téléversé. Dépôt et hash vérifiés **sans jeton d'authentification**, pour être sûr qu'un correcteur non connecté y accède : `HTTP 200` sur les deux. Le hash déclaré est bien la tête de `main`, contrôlé par commande plutôt que recopié à l'œil. Les trois `[JALON]` sont poussés et dans l'ordre, et aucun commit de code ne précède le premier.

**Ce que je referais autrement avec une journée de plus :**

1. **Des tests de bout en bout sur l'interface.** C'est le seul endroit du projet qui ne soit vérifié que par des appels HTTP et par mes yeux. Le sujet ne note pas le rendu visuel, ce qui m'a servi d'excuse pour ne pas le faire ; ce n'est pas une raison suffisante.
2. **Une pull request par issue, strictement.** Quatre de mes vingt-deux en ferment plusieurs. À chaque fois j'avais une raison écrite — un test qui rend deux livrables indissociables, un écran qui casse si on sépare — mais un regroupement justifié reste un regroupement.
3. **Le vocabulaire fixé dès le premier commit.** J'ai employé « ticket » et « issue » pour la même chose pendant deux étapes, puis j'ai dû repasser sur quatre fichiers. Une convention se décide une fois, au début.
4. **`RG16` tenue par le schéma plutôt que par l'algorithme.** Après le changement de l'étape 3, « au plus deux relectures par étudiant et par séance » n'est plus garanti que par le code. Avec du temps, j'aurais cherché une contrainte — un compteur matérialisé, un déclencheur — parce qu'une règle tenue par le schéma survit à un bogue du service.
