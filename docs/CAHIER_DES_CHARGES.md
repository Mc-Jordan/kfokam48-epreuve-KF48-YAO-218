# Cahier des charges — Présences & Relectures KFOKAM48

**Auteur :** NANDJO NGOULE Michele Jordan · KF48-YAO-218
**Centre :** Yaoundé
**Version :** 1 · **Date :** 25 septembre 2026
**Frontend choisi :** React + Vite + TypeScript, parce que le besoin tient en trois écrans sans rendu côté serveur ni référencement : Vite offre le démarrage et le build les plus rapides, TypeScript rend le contrat d'API vérifiable à la compilation, et le tout se sert en production par un simple conteneur de fichiers statiques.

> Les dix sections sont imposées par le sujet, dans cet ordre.
> Les exigences sont numérotées `EFn`, les règles de gestion `RGn`. Elles sont citées
> dans les issues, dans les messages de commit et dans les tests.
> Les décisions prises à la place du client renvoient aux questions `Qn` de `CLIENT.md`.

---

## 1. Contexte et objectif

La formation KFOKAM48 réunit des promotions d'une soixantaine d'étudiants autour de séances animées par un formateur. Trois gestes rythment chaque séance et se font aujourd'hui à la main : constater qui est là, récupérer le travail rendu, et faire circuler ce travail entre pairs pour qu'il soit relu. Le formateur tient ces informations dans des feuilles séparées, ce qui lui interdit de répondre simplement à la seule question qui l'intéresse vraiment : où en est chaque étudiant.

L'application supprime ces trois frictions. Le formateur ouvre une séance et obtient un code éphémère ; les étudiants s'en servent pour se déclarer présents depuis leur téléphone, puis déposent le lien de leur exercice. À la fermeture des dépôts, le système distribue lui-même les exercices entre les étudiants présents, de sorte que chacun en relise un qui n'est pas le sien. Le formateur suit l'ensemble dans un tableau unique : présence, dépôts, moyenne reçue, relectures encore dues.

L'objectif n'est pas d'évaluer les étudiants à la place du formateur, mais de **rendre visible en un écran l'état d'une promotion**, et de faire porter la relecture par le groupe plutôt que par une seule personne.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session et diffuser son code · ajouter une présence à la main · clôturer la session, ce qui ferme les dépôts et déclenche l'attribution des relectures · finaliser la session, ce qui fige les relectures · consulter le tableau récapitulatif et le détail des présences | Déposer un exercice · rendre ou modifier une relecture · modifier une note rendue par un étudiant |
| **Étudiant** | Se déclarer présent à l'aide du code · déposer le lien de son exercice · remplacer ce lien tant que la session est ouverte · consulter la note et le commentaire reçus | Ouvrir, clôturer ou finaliser une session · relire son propre exercice · connaître l'identité de son relecteur · marquer la présence d'un autre étudiant |
| **Relecteur** | Consulter les exercices qui lui sont attribués · rendre une note entière et un commentaire · corriger cette relecture tant que la session n'est pas finalisée | Choisir l'exercice qu'il relit · relire plus d'un exercice par session · revenir sur sa relecture après la finalisation |

**Le relecteur n'est pas un acteur distinct : c'est un étudiant à qui une relecture a été attribuée.** Nous le traitons comme un rôle temporaire, porté par une donnée et non par une identité.

*Conséquence sur le modèle de données :* aucune table `relecteur`, aucun champ de rôle sur l'étudiant. Le rôle naît de l'existence d'une ligne dans `relecture` dont la colonne `relecteur_id` pointe vers l'étudiant. Un étudiant est donc relecteur pour une session donnée et ne l'est plus pour une autre, sans qu'aucun état n'ait à être maintenu. C'est aussi ce qui rend l'anonymat (`Q8`) simple à tenir : il suffit de ne jamais exposer `relecteur_id` dans les réponses destinées à l'auteur de l'exercice.

## 3. Périmètre

**Inclus dans cette version :**

- Ouverture d'une session de cours par le formateur, avec production d'un code de présence à durée de vie limitée
- Marquage de présence par l'étudiant à l'aide de ce code, depuis un téléphone
- Protection contre la devinette de code par limitation des tentatives
- Ajout manuel d'une présence par le formateur, tracé comme tel
- Dépôt par l'étudiant du lien de son exercice, et remplacement de ce lien tant que la session est ouverte
- Clôture de la session par le formateur : fermeture des dépôts et attribution automatique d'un relecteur par exercice, tiré au sort parmi les étudiants présents
- Rendu d'une relecture — note entière sur 20 et commentaire — puis correction de cette relecture jusqu'à la finalisation
- Finalisation de la session par le formateur, qui fige définitivement les relectures
- Consultation par l'étudiant de la note et du commentaire reçus, sans l'identité du relecteur
- Tableau récapitulatif du formateur : présences, exercices déposés, moyenne reçue, relectures encore dues
- Jeu de données de démonstration chargé au démarrage

**Explicitement exclu :**

- **Toute authentification et toute gestion de mot de passe.** L'étudiant choisit son nom dans une liste (`Q1`). La conséquence est assumée : n'importe qui peut agir au nom de n'importe qui. Le dispositif est un outil de séance, pas un registre opposable
- **La création et l'administration des promotions et des étudiants** par une interface : ces données proviennent du jeu de démonstration
- **Le stockage des exercices eux-mêmes** : l'application conserve un lien, jamais un fichier
- **Toute notification** par courriel ou par message, y compris la relance d'un relecteur défaillant
- **La relecture multiple** : un exercice reçoit un relecteur et un seul (`Q6`)
- **La réattribution manuelle d'un relecteur** par le formateur
- **L'export des données** et l'historisation des modifications
- **L'internationalisation** : l'application est en français
- **Le soin apporté à la présentation.** Le sujet ne note pas le rendu visuel ; l'interface vise l'utilisabilité sur téléphone, rien de plus
- **La gestion de plusieurs formateurs** et les droits fins associés

*Ce que nous excluons compte autant que ce que nous incluons : chacune de ces lignes est une décision, pas un oubli.*

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | L'étudiant marque sa présence à l'aide d'un code | Quand je saisis un code valide et non expiré, ma présence apparaît dans le tableau du formateur | Must |
| EF2 | | | |
| EF3 | | | |

*Un critère d'acceptation se formule « quand … alors … ». S'il n'est pas vérifiable par quelqu'un d'autre que toi, ce n'en est pas un.*

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'interface de marquage de présence est utilisable sur un téléphone | |
| ENF2 | Le tableau du formateur répond en moins de 2 s pour une promotion de 60 étudiants | |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Un code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Un étudiant ne peut pas relire son propre exercice | Q5 |
| RG3 | Une note est un entier compris entre 0 et 20 | Q9 |
| RG4 | | |

*Numérote-les. Tu les citeras dans tes issues, tes messages de commit et tes tests. Une règle qu'on ne peut pas citer est une règle qu'on oublie.*

## 7. Zones d'ombre, hypothèses et contradictions

**Points que la demande ne tranche pas :**

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| | | | |

**Contradictions relevées :**

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| | | |

*Une hypothèse écrite est toujours acceptée. Une hypothèse silencieuse est une faute.*

## 8. Contraintes techniques

*Reprends les contraintes B1 à B6 et F1 à F3 du sujet, et ajoute celles que tu t'imposes toi-même (base de données choisie, gestion des migrations, stratégie de tests).*

## 9. Livrables

-

## 10. Démarche prévue

*Comment tu comptes mener les six étapes : dans quel ordre, ce que tu vises à chaque jalon, ce que tu feras si tu prends du retard.*

**Definition of Done — un ticket est terminé quand :**
-
-

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | | Version initiale |

*L'étape 3 rendra une partie de ce document faux. Reviens le corriger et note-le ici — un cahier des charges périmé est un cahier des charges mort.*
