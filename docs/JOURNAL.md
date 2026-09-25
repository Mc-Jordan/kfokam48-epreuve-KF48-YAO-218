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

**Fait :**

**Bloqué :**

**IA :**

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
