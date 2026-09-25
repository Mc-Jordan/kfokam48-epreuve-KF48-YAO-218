# D1 — Diagramme de cas d'utilisation

Mermaid n'offre pas de diagramme de cas d'utilisation natif. Le formalisme UML est
rendu par un graphe orienté : les acteurs à gauche et à droite, les cas d'utilisation
groupés par phase du cycle de vie d'une session, les relations `<<include>>` en
pointillés.

Conforme à la section 2 du cahier des charges : le **relecteur n'est pas un acteur
distinct**, c'est un étudiant à qui une relecture a été attribuée. Il figure donc en
acteur grisé, dérivé de l'étudiant.

```mermaid
graph LR
    F(("Formateur"))
    E(("Étudiant"))
    R(("Relecteur<br/>= étudiant attribué"))

    subgraph SESSION["Ouverture de la séance"]
        UC1["UC1 — Ouvrir une session<br/>et obtenir un code"]
        UC2["UC2 — Marquer sa présence"]
        UC3["UC3 — Ajouter une présence<br/>à la main"]
        UC4["UC4 — Lister les étudiants<br/>de la promotion"]
    end

    subgraph TRAVAIL["Dépôt des exercices"]
        UC5["UC5 — Déposer le lien<br/>de son exercice"]
        UC6["UC6 — Remplacer le lien<br/>de son exercice"]
    end

    subgraph RELECTURE["Relecture par les pairs"]
        UC7["UC7 — Clôturer la session<br/>et attribuer les relecteurs"]
        UC8["UC8 — Consulter les relectures<br/>qui m'incombent"]
        UC9["UC9 — Rendre ou corriger<br/>une relecture"]
        UC10["UC10 — Finaliser la session<br/>et figer les relectures"]
    end

    subgraph SUIVI["Suivi"]
        UC11["UC11 — Consulter la note<br/>et le commentaire reçus"]
        UC12["UC12 — Consulter le tableau<br/>récapitulatif"]
        UC13["UC13 — Consulter le détail<br/>des présences d'une session"]
    end

    F --> UC1
    F --> UC3
    F --> UC7
    F --> UC10
    F --> UC12
    F --> UC13

    E --> UC2
    E --> UC5
    E --> UC6
    E --> UC11

    R --> UC8
    R --> UC9

    E -. "devient" .-> R

    UC2 -. "«include»" .-> UC4
    UC3 -. "«include»" .-> UC4
    UC5 -. "«include»" .-> UC2
    UC7 -. "«include»" .-> UC8
    UC9 -. "«include»" .-> UC8

    classDef acteur fill:#e8eef7,stroke:#2d3e50,stroke-width:2px
    classDef derive fill:#f2f2f2,stroke:#888,stroke-dasharray:4 3
    class F,E acteur
    class R derive
```

## Lecture

| Relation | Sens |
|---|---|
| `UC2 «include» UC4` | On ne peut pas marquer sa présence sans avoir choisi son nom dans la liste de la promotion — conséquence directe de `Q1` |
| `UC5 «include» UC2` | Le dépôt exige une présence enregistrée (`RG8`, arbitrage du trou n°3) |
| `UC7 «include» UC8` | L'attribution crée les relectures : avant la clôture, aucun relecteur n'a rien à consulter (`RG13`) |
| `Étudiant ⇢ Relecteur` | Dérivation, pas héritage : le rôle naît d'une ligne dans `relecture`, il n'est porté par aucun état de l'étudiant |

**Exigences couvertes :** `EF1` (UC1), `EF2` (UC2, UC4), `EF4` (UC3), `EF5` (UC5), `EF6` (UC6), `EF7` (UC7), `EF8` (UC8), `EF9` (UC9), `EF10` (UC10), `EF11` (UC11), `EF12` (UC12, UC13).
