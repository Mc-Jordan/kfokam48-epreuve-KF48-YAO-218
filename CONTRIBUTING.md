# Conventions de travail

Dépôt d'examen — épreuve finale fullstack KFOKAM48 · KF48-YAO-218.
L'historique Git est lu comme une copie. Ces conventions sont tenues du premier au dernier commit.

---

## 1. Messages de commit

Format **Conventional Commits**, rédigés en français.

```
<type>(<portée>): <sujet à l'impératif présent>

<corps : le pourquoi, jamais le comment — le diff dit déjà le comment>

Closes #<n°issue>
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle capacité visible par un utilisateur |
| `fix` | Correction d'un comportement fautif |
| `docs` | Cahier des charges, diagrammes, journal, README |
| `test` | Ajout ou correction de tests |
| `refactor` | Réorganisation sans changement de comportement |
| `chore` | Outillage, structure, configuration de dépôt |
| `build` | Maven, npm, Docker, dépendances |
| `ci` | GitHub Actions, SonarCloud |

Règles :

- Sujet à l'impératif présent, **72 caractères maximum**, **sans point final**.
- Portées utilisées : `session`, `presence`, `exercice`, `relecture`, `tableau`, `api`, `bdd`, `front`, `docs`, `infra`.
- **Tout commit qui met en œuvre une règle de gestion la cite** dans le corps : `Applique RG1.`
- Un commit qui termine une issue la ferme : `Closes #12`.
- **Aucun trailer `Co-Authored-By`.** Un hook `commit-msg` local le refuse.
- Un commit = un changement cohérent. Pas de commit fourre-tout, pas de commit « wip ».

## 2. Branches

Une branche par issue, nommée d'après son numéro.

```
feat/<n°issue>-<slug>      fix/<n°issue>-<slug>
docs/<n°issue>-<slug>      chore/<n°issue>-<slug>
```

Exemple : `feat/7-marquer-presence-avec-code`.

`main` reste toujours sain : il ne reçoit que des branches fusionnées par pull request.
Aucun commit direct sur `main`, à l'exception des trois commits de jalon.

## 3. Pull requests

- **Titre** = le résultat obtenu, pas la tâche effectuée.
- **Corps** = le gabarit `.github/PULL_REQUEST_TEMPLATE.md`, avec `Closes #n`.
- **Stratégie de fusion : `--no-ff`**, une seule pour toute l'épreuve.
  *Justification :* le commit de fusion conserve la topologie des branches — elle prouve à elle seule la règle « une branche par issue » — tout en préservant les commits atomiques, que `--squash` écraserait.
- La branche est supprimée après fusion ; le commit de fusion en garde la trace.

## 4. Étiquettes et jalons

| Famille | Étiquettes |
|---|---|
| Priorité | `must` · `should` · `could` |
| Domaine | `analyse` · `backend` · `frontend` · `infra` · `docs` · `git` · `qualite` |
| Nature | `bug` · `evolution` · `dette` |

Jalons GitHub : `v0.1 — première version`, `v1.0 — version finale`, `Étape 3 — enveloppe`.

## 5. Jalons Git et étiquettes de version

Trois commits vides, aux messages **exacts**, poussés dès leur création :

```bash
git commit --allow-empty -m "[JALON] analyse"
git commit --allow-empty -m "[JALON] v0.1"
git commit --allow-empty -m "[JALON] v1.0"
```

`[JALON] analyse` **précède obligatoirement le premier commit de code**.

Étiquettes annotées, posées sur les commits de jalon correspondants, message = périmètre livré :

```bash
git tag -a v0.1 -m "Première version : <périmètre>"
git push origin v0.1
```

## 6. Definition of Done

Une issue est terminée quand :

- [ ] Ses critères d'acceptation sont vérifiés, un par un
- [ ] Les tests passent en local et en intégration continue
- [ ] Le contrat `api/contrat.yaml` est respecté, codes d'erreur compris
- [ ] La documentation touchée est à jour (cahier des charges, diagrammes, README)
- [ ] La pull request est fusionnée et l'issue fermée
- [ ] `main` est vert

## 7. Cadence

Un push au minimum toutes les trente minutes. Un travail resté en local vaut zéro,
et un historique concentré sur la dernière heure est pénalisé.

## 8. Ce qui ne doit jamais entrer dans l'historique

`target/` · `node_modules/` · `dist/` · `build/` · tout secret · tout fichier généré.
Un hook `pre-commit` local refuse ces fichiers. Il ne remplace pas la vigilance.
