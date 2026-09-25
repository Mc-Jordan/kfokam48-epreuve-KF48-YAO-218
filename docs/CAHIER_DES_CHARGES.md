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

Douze exigences. Les sept **Must** constituent le périmètre du jalon `v0.1`.

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| **EF1** | Le formateur ouvre une session de cours et obtient un code de présence | Quand je soumets un titre et une promotion existante, alors la session est créée et je reçois un code, une date d'ouverture et une date d'expiration fixée quinze minutes plus tard | Must |
| **EF2** | L'étudiant marque sa présence à l'aide du code | Quand je choisis mon nom dans la liste de ma promotion et que je saisis un code valide et non expiré, alors ma présence est enregistrée avec la source `ETUDIANT` et apparaît dans le tableau du formateur | Must |
| **EF3** | Le système décourage la devinette de code | Quand j'ai saisi cinq codes erronés d'affilée, alors ma sixième tentative est refusée pendant deux minutes, puis redevient possible sans intervention | Should |
| **EF4** | Le formateur ajoute une présence à la main | Quand j'ajoute un étudiant à une session ouverte sans saisir de code, alors sa présence est enregistrée avec la source `FORMATEUR` et se distingue visuellement des autres dans le détail de la session | Should |
| **EF5** | L'étudiant dépose le lien de son exercice | Quand je suis présent à la session et que je soumets une adresse `http` ou `https` absolue, alors mon exercice est enregistré au statut `DEPOSE`, et une seconde tentative sur la même session est refusée | Must |
| **EF6** | L'étudiant remplace le lien de son exercice | Quand la session est encore ouverte, alors je peux remplacer le lien de mon exercice autant de fois que nécessaire ; dès qu'elle est clôturée, le remplacement est refusé | Should |
| **EF7** | Le formateur clôture la session et le système attribue les relecteurs | Quand je clôture une session ouverte, alors les dépôts sont fermés et chaque exercice reçoit au plus un relecteur tiré au hasard parmi les étudiants présents autres que son auteur ; le compte des exercices attribués et non attribuables m'est renvoyé | Must |
| **EF8** | Le relecteur consulte les relectures qui lui incombent | Quand je consulte mes relectures, alors j'obtiens la liste des exercices qui m'ont été attribués avec leur lien et leur état, et rien d'autre | Must |
| **EF9** | Le relecteur rend sa relecture, et la corrige | Quand je soumets une note entière comprise entre 0 et 20 et un commentaire, alors la relecture est enregistrée ; quand je la soumets à nouveau avant la finalisation de la session, alors elle est remplacée ; après la finalisation, la soumission est refusée | Must |
| **EF10** | Le formateur finalise la session et fige les relectures | Quand je finalise une session déjà clôturée, alors toutes ses relectures deviennent définitives et le nombre de relectures figées m'est renvoyé ; finaliser une session non clôturée est refusé | Should |
| **EF11** | L'étudiant consulte la note et le commentaire reçus | Quand mon exercice a été relu, alors je vois la note et le commentaire ; l'identité du relecteur n'apparaît dans aucune réponse ni dans aucun écran | Should |
| **EF12** | Le formateur consulte le tableau récapitulatif de la promotion | Quand j'ouvre le tableau d'une promotion existante, alors j'obtiens pour chaque étudiant son nombre de présences, son nombre d'exercices déposés, la moyenne des notes reçues — vide s'il n'en a aucune — et le nombre de relectures qu'il doit encore rendre ; une promotion inconnue est refusée | Must |

*Chaque critère est formulé « quand … alors … » et se vérifie sans connaître le code : c'est la condition pour qu'un tiers puisse le contrôler.*

*Priorisation assumée :* `EF3`, `EF4`, `EF6`, `EF10` et `EF11` sont des `Should` parce qu'aucune n'est nécessaire pour dérouler le cycle complet d'une séance. Sans `EF10`, les relectures restent modifiables indéfiniment ; sans `EF11`, l'étudiant ne voit pas sa note, mais le formateur la voit. Ce sont des manques acceptables à `v0.1`, pas à `v1.0`.

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| **ENF1** | L'écran de marquage de présence est utilisable sur un téléphone tenu d'une main | Le parcours « choisir son nom, saisir le code, valider » s'effectue sans défilement horizontal sur une fenêtre de 360 px de large, avec des zones tactiles d'au moins 44 px |
| **ENF2** | Le tableau du formateur répond en moins de deux secondes pour une promotion de soixante étudiants ayant suivi trente sessions | Le jeu de démonstration charge cette volumétrie ; on mesure le temps de réponse de `GET /api/tableau` et on vérifie qu'il n'exécute pas une requête par étudiant |
| **ENF3** | Toutes les erreurs, sans exception, respectent le format imposé `{ code, message }` | Un test d'intégration parcourt chaque code d'erreur recensé en section 8 et vérifie le statut HTTP, la présence des deux champs et l'absence de trace d'exécution ; aucune page d'erreur par défaut ne doit apparaître |
| **ENF4** | L'application supporte la volumétrie d'une année de formation | Trois promotions de soixante étudiants, trente sessions par promotion, un exercice et une relecture par étudiant et par session, soit environ cinq mille cinq cents relectures : les index posés sur les clés étrangères et sur le code de session le permettent |
| **ENF5** | Un tiers démarre l'application depuis un clone vierge | `git clone` puis `docker compose up` dans un dossier vide, sur une machine sans base locale, aboutit à une application peuplée du jeu de démonstration |
| **ENF6** | Le schéma de base est versionné et rejouable | Les migrations Flyway s'appliquent sur une base vide comme sur une base déjà migrée ; `ddl-auto` reste à `validate` hors tests |
| **ENF7** | Deux tests prouvent quelque chose, et tournent sur un poste vierge | Un test unitaire sur une règle de gestion réelle et un test d'intégration sur un point d'entrée, exécutés par `./mvnw verify` sans base installée localement, la base de test étant fournie par Testcontainers |
| **ENF8** | L'absence d'authentification est une décision, pas une faille ignorée | Le périmètre l'énonce (§3), la conséquence est écrite : aucune donnée personnelle sensible n'est stockée, et le dispositif n'a pas valeur de registre opposable |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| **RG1** | Le code d'une session expire quinze minutes après l'ouverture de celle-ci | `Q2` |
| **RG2** | Le code d'une session est unique : deux sessions ne peuvent pas porter le même code | hypothèse — imposée par `POST /api/presences`, qui n'identifie la session que par son code |
| **RG3** | Une présence est unique pour un couple session-étudiant : un étudiant ne peut être présent deux fois à la même session | hypothèse — imposée par le `409 DEJA_PRESENT` du contrat |
| **RG4** | Toute présence porte une source, `ETUDIANT` ou `FORMATEUR`, et cette source est visible du formateur | `Q14` |
| **RG5** | Le formateur peut enregistrer une présence sans code, y compris après l'expiration de celui-ci, tant que la session est ouverte | `Q14` |
| **RG6** | Après cinq tentatives de code erronées consécutives, l'étudiant ne peut plus tenter de marquer sa présence pendant deux minutes. Une tentative réussie ou l'écoulement du délai remet le compteur à zéro | `Q4` |
| **RG7** | Un étudiant dépose au plus un exercice par session | contrat — `409 EXERCICE_DEJA_DEPOSE` |
| **RG8** | Le dépôt d'un exercice exige une présence enregistrée sur la session, quelle qu'en soit la source | hypothèse — voir §7, trou n°3 |
| **RG9** | Le lien d'un exercice est une adresse absolue de schéma `http` ou `https` | contrat — `400 LIEN_INVALIDE` |
| **RG10** | Le lien d'un exercice est remplaçable tant que la session est ouverte, et ne l'est plus dès la clôture | `Q13`, lue à la lumière de l'arbitrage sur le moment du tirage (§7, trou n°2) |
| **RG11** | Le dépôt d'un exercice reste possible après l'expiration du code, jusqu'à la clôture de la session | `Q12` |
| **RG12** | Une session parcourt trois états dans cet ordre : `OUVERTE`, `CLOTUREE`, `FINALISEE`. Aucun retour en arrière n'est possible | hypothèse — voir §7, contradiction sur le mot « clôturer » |
| **RG13** | La clôture ferme les dépôts et déclenche l'attribution des relecteurs | hypothèse — arbitrage du trou n°2 |
| **RG14** | L'attribution tire au hasard, pour chaque exercice, un relecteur parmi les étudiants présents à la session, l'auteur exclu | `Q7` et `Q5` |
| **RG15** | Un exercice reçoit au plus un relecteur | `Q6` |
| **RG16** | Un étudiant se voit attribuer au plus une relecture par session | hypothèse — le dépôt exigeant la présence (`RG8`), il y a toujours au moins autant de présents que d'exercices, donc une répartition sans doublon existe dès que deux étudiants sont présents |
| **RG17** | Lorsqu'aucun relecteur éligible n'existe au moment de la clôture, l'exercice prend l'état `NON_ATTRIBUABLE`, qui est terminal et se distingue de l'attente au tableau du formateur | hypothèse — arbitrage du trou n°1 |
| **RG18** | Une note est un nombre entier compris entre 0 et 20 inclus | `Q9` |
| **RG19** | Un étudiant ne peut jamais relire son propre exercice | `Q5` |
| **RG20** | Une relecture rendue reste modifiable par son relecteur tant que la session n'est pas finalisée | `Q10`, retenue contre `Q15` — voir §7 |
| **RG21** | La finalisation fige définitivement toutes les relectures de la session ; aucune modification n'est plus acceptée | `Q10` et `Q15` conciliées |
| **RG22** | L'auteur d'un exercice voit la note et le commentaire reçus, jamais l'identité de son relecteur | `Q8` |
| **RG23** | Un exercice attribué dont la relecture n'a pas été rendue reste en attente, et cette attente est visible du formateur | `Q11` |
| **RG24** | La moyenne d'un étudiant est calculée par le serveur sur les seules relectures rendues le concernant, et vaut « vide » lorsqu'il n'en a aucune | `Q16` et contrainte `F3` |
| **RG25** | Toutes les réponses d'erreur, sans exception, portent le corps `{ code, message }`, le code étant un identifiant stable en majuscules et le message une phrase en français | contrat |

*Ces vingt-cinq règles sont citées par leur référence dans les issues, dans les messages de commit et dans les noms de tests. Une règle qu'on ne peut pas citer est une règle qu'on oublie.*

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
