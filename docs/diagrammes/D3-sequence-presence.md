# D3 — Séquence : marquer sa présence

Cas nominal et quatre cas d'erreur. **Les codes HTTP et les identifiants d'erreur sont
ceux du contrat `api/contrat.yaml`, à la lettre** — `201`, `400 CODE_INCONNU`,
`409 DEJA_PRESENT`, `410 CODE_EXPIRE` — augmentés du `429 TROP_DE_TENTATIVES` ajouté
pour porter `RG6`, décision justifiée en section 7 du cahier des charges.

Toute réponse d'erreur porte le corps imposé `{ code, message }` (`RG25`).

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front React
    participant C as PresenceController
    participant S as PresenceService
    participant DB as PostgreSQL

    E->>F: choisit son nom, saisit le code
    F->>C: POST /api/presences { code, etudiantId }

    alt champ manquant (RG25)
        C-->>F: 400 { code: "CHAMP_MANQUANT" }
        F-->>E: message de saisie
    else validation passée
        C->>S: enregistrer(code, etudiantId)
        S->>DB: cinq dernières tentatives de l'étudiant

        alt étudiant bloqué — 5 échecs en moins de 2 min (RG6)
            DB-->>S: 5 échecs consécutifs récents
            S-->>C: TropDeTentativesException
            C-->>F: 429 { code: "TROP_DE_TENTATIVES" }
            F-->>E: réessayez dans deux minutes
        else tentatives autorisées
            S->>DB: session par code
            alt code inconnu (RG2)
                DB-->>S: aucune session
                S->>DB: trace la tentative en échec
                S-->>C: CodeInconnuException
                C-->>F: 400 { code: "CODE_INCONNU" }
                F-->>E: code incorrect
            else session trouvée
                alt code expiré — maintenant > expiration_at (RG1)
                    S-->>C: CodeExpireException
                    C-->>F: 410 { code: "CODE_EXPIRE" }
                    F-->>E: le code a expiré
                else code encore valide
                    S->>DB: présence existante pour (session, étudiant) ?
                    alt déjà présent (RG3)
                        DB-->>S: présence trouvée
                        S-->>C: DejaPresentException
                        C-->>F: 409 { code: "DEJA_PRESENT" }
                        F-->>E: présence déjà enregistrée
                    else cas nominal
                        S->>DB: insère presence (source = ETUDIANT) — RG4
                        S->>DB: trace la tentative réussie — remet RG6 à zéro
                        DB-->>S: presence créée
                        S-->>C: Presence
                        C-->>F: 201 { id, sessionId, etudiantId, source }
                        F-->>E: présence confirmée
                    end
                end
            end
        end
    end
```

## Correspondance avec le contrat

| Branche | Statut | Code stable | Règle | Au contrat |
|---|---|---|---|---|
| Cas nominal | `201` | — | `RG3`, `RG4` | imposé |
| Champ absent du corps | `400` | `CHAMP_MANQUANT` | `RG25` | imposé |
| Code ne correspondant à aucune session | `400` | `CODE_INCONNU` | `RG2` | imposé |
| Présence déjà enregistrée | `409` | `DEJA_PRESENT` | `RG3` | imposé |
| Code dont la durée de vie est écoulée | `410` | `CODE_EXPIRE` | `RG1` | imposé |
| Étudiant bloqué après cinq échecs | `429` | `TROP_DE_TENTATIVES` | `RG6` | **ajouté** — voir §7 |

## Points de vigilance pour l'implémentation

- **L'ordre des contrôles est signifiant.** Le blocage (`RG6`) précède la recherche de la session : un étudiant bloqué ne doit pas pouvoir apprendre, par la différence entre `400` et `410`, qu'un code existe. C'est la raison d'être de `Q4`.
- **L'expiration se teste avant l'unicité.** Un code expiré renvoie `410`, même si l'étudiant était déjà présent : l'information la plus utile est que le code ne vaut plus rien.
- **Seuls les échecs sont tracés comme tels.** Une tentative réussie remet le compteur à zéro (`RG6`), ce qui se traduit par une ligne `reussie = true` dans `tentatives_presence`.
- **La présence ajoutée par le formateur ne passe pas par ce chemin.** Elle emprunte l'opération de présence manuelle, sans code et sans contrôle d'expiration (`RG5`), et écrit `source = FORMATEUR`.

**Règles de gestion illustrées :** `RG1`, `RG2`, `RG3`, `RG4`, `RG6`, `RG25`.
