# 05 — Évaluations, moteur de scoring et feuille de route

Ce document décrit l'implémentation (étape 5) de la méthodologie définie dans
[02-modele-maturite.md](02-modele-maturite.md).

## 1. Nouveaux concepts

```
Organisation ──< Évaluation >── Référentiel (version figée)
                     │
                     ├── Cibles par domaine (GOV → 3, LLM → 4…)
                     └──< Réponses (niveau 1-5 ou N/A + justification, commentaire, preuve)
```

| Table | Rôle |
|---|---|
| `organization` | Entreprise évaluée (secteur, taille, pays) |
| `assessment` | Une évaluation : titre, périmètre, statut, score global figé |
| `assessment_target` | Niveau cible par domaine |
| `answer` | Réponse à une question (unique par évaluation et par question) |

Contraintes en base : une réponse est **soit** un niveau, **soit** « Non applicable »
avec justification (`CHECK`). Le verrouillage optimiste (`version`) empêche deux
personnes d'écraser mutuellement leurs modifications.

## 2. Cycle de vie d'une évaluation

```
          créer                    soumettre (100 % répondu)
 (rien) ────────▶  DRAFT  ─────────────────────────────────▶  SUBMITTED
                    ▲  réponses et cibles modifiables            │ réponses verrouillées
                    │                                            │ score global figé
                    └──────────────── rouvrir ───────────────────┘
```

Les règles sont portées par l'entité `Assessment` elle-même (`assertEditable()`,
`submit()`, `reopen()`). Il est donc impossible de les contourner en passant par
un autre service.

## 3. Le moteur de scoring

`ScoringEngine` est une classe **sans état et sans base de données** :

```
ScoringModel (référentiel)  +  réponses  +  cibles   ──▶   ScoringEngine   ──▶   ScoringResult
```

- **`ScoringModel`** : une vue simplifiée et immuable du référentiel, construite une
  seule fois par version puis gardée en mémoire (`ScoringModelFactory`).
- **`ScoringResult`** contient :
  - le score global, le niveau et le pourcentage ;
  - le taux de complétion ;
  - pour chaque domaine : le score brut, le score final, la question critique
    responsable du plafonnement éventuel, la cible et l'écart ;
  - pour chaque sous-domaine : le score et le nombre de réponses et de N/A ;
  - les **recommandations** triées par horizon puis par priorité.

Avantage : le calcul est **déterministe** et **testé unitairement** (9 tests,
résultats vérifiés à la main). Les résultats ne sont pas stockés, ils sont
recalculés à la demande : le référentiel est immuable et les réponses sont
verrouillées après soumission, donc le résultat ne change pas. Seul le score
global est figé à la soumission, pour l'historique.

### Indice de confiance
`evidenceRatePercent` = part des réponses évaluées (hors N/A) qui citent une preuve.
Exemple : un score de 3,5 avec 10 % de preuves est moins fiable qu'un 3,0 avec 80 %.

## 4. API REST

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/v1/organizations` | Créer une organisation |
| GET | `/api/v1/organizations` · `/{id}` | Lister / consulter |
| PUT | `/api/v1/organizations/{id}` | Modifier |
| POST | `/api/v1/assessments` | Créer une évaluation (référentiel actif, cibles optionnelles) |
| GET | `/api/v1/assessments?organizationId=` | Lister |
| GET | `/api/v1/assessments/{id}` | Résumé : statut, avancement, score figé |
| DELETE | `/api/v1/assessments/{id}` | Supprimer (brouillon uniquement) |
| PUT | `/api/v1/assessments/{id}/targets` | Modifier les cibles |
| GET | `/api/v1/assessments/{id}/answers` | Réponses saisies |
| PUT | `/api/v1/assessments/{id}/answers/{code}` | Enregistrer une réponse (création ou modification) |
| DELETE | `/api/v1/assessments/{id}/answers/{code}` | Effacer une réponse |
| POST | `/api/v1/assessments/{id}/submit` | Soumettre |
| POST | `/api/v1/assessments/{id}/reopen` | Rouvrir |
| GET | `/api/v1/assessments/{id}/results` | Scores, écarts, recommandations, indice de confiance |
| GET | `/api/v1/assessments/{id}/roadmap` | Feuille de route en 4 horizons |

### Codes d'erreur

| Code | Cas | Exemple |
|---|---|---|
| 400 | Données invalides | Niveau 7, N/A sans justification, domaine cible inconnu |
| 404 | Ressource inexistante | Question `NOPE.1.1` |
| 409 | Opération impossible dans l'état actuel | Modifier une évaluation soumise, soumettre à 80 % |

Exemple de réponse 400 avec le détail par champ :
```json
{ "status": 400, "title": "Données invalides",
  "detail": "Un ou plusieurs champs sont invalides",
  "errors": { "name": "must not be blank", "sector": "must not be null" } }
```

## 5. Exemple réel (API en fonctionnement)

Évaluation partielle : GOV.1.1, GOV.1.2, LLM.1.1 et LLM.3.2 au niveau 2, LLM.3.1 au niveau 1, cible LLM = 4.

```
Global : 1,88 → niveau 2 « Reproductible » (22 %) — complétion 8,47 %
GOV  score 2,00  cible 3  écart 1,00
LLM  score 1,75  cible 4  écart 2,25

Feuille de route
  Quick wins (0-3 mois)    LLM.3.2 (prio 90) · GOV.1.2 (prio 45)
  Court terme (3-6 mois)   LLM.3.1 (prio 135) · LLM.1.1 (prio 90)
  Moyen terme (6-12 mois)  GOV.1.1 (prio 30)
```

## 6. Tests (21 au total)

| Classe | Type | Ce qui est vérifié |
|---|---|---|
| `ScoringEngineTest` | Unitaire (9) | Moyennes pondérées, maillon faible, N/A, renormalisation, seuils, priorités, horizons, arrondis |
| `AssessmentFlowIntegrationTest` | Intégration (2) | Parcours complet via l'API sur PostgreSQL : 59 réponses, résultats, feuille de route, soumission, verrouillage, réouverture, erreurs 400/404/409 |
| `ReferentialValidatorTest`, `ReferentialIntegrationTest` | Unitaire + intégration (9) | Étape 4 |

### Deux bugs trouvés grâce aux tests
1. **Champ booléen obligatoire** : avec Jackson 3, un JSON `{"level": 3}` sans
   `notApplicable` était rejeté (400). Le champ est devenu optionnel (absent = `false`).
2. **Arrondi flottant** : `1 × 3 × 0,15 × 100` vaut `44,999…` en `double`, ce qui
   classait une priorité affichée « 45 » dans le mauvais horizon. La priorité est
   désormais calculée en `BigDecimal`, et un test de non-régression a été ajouté.
