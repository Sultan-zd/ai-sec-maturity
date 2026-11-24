# 12 — Contrats entre les modules (plateforme, labo red-team, gouvernance)

Le projet est développé par trois personnes. Chaque module peut évoluer seul, à condition de respecter
les deux contrats décrits ici. Toute modification d'un contrat se décide en équipe.

| Contrat | Fournisseur | Utilisateurs | Où |
|---|---|---|---|
| 1. Forme du référentiel (JSON) | Plateforme | Labo red-team, gouvernance | `referential/schema/*.schema.json` |
| 2. API d'import des mesures red-team | Plateforme | Labo red-team | `POST /api/v1/assessments/{id}/measurements` |

---

## 1. Le référentiel : un contrat verrouillé par JSON Schema

- `referential/schema/referentiel.schema.json` décrit le fichier principal (domaines, poids, niveaux, règles).
- `referential/schema/questions.schema.json` décrit un fichier de questions (`referential/questions/XXX.json`).
- `node scripts/referential-tool.mjs --check` (et la CI à chaque push) refuse tout fichier qui ne respecte pas
  le schéma : champ inconnu, champ manquant, mauvais type, nombre de niveaux différent de 5…

**Règles d'évolution :**

| Changement | Autorisé ? | Comment |
|---|---|---|
| Corriger une faute, reformuler une question | Oui | Nouvelle version **mineure** (1.0.0 → 1.1.0) |
| Ajouter une question, un sous-domaine, un domaine ; changer des poids | Oui, en équipe | Nouvelle version (1.x → 2.0.0) : les poids et les scores changent |
| Ajouter ou renommer un **champ** | En équipe | Modifier d'abord le schéma, dans le même commit que les données |
| Réutiliser un code de question pour une autre question | **Non** | Les codes (ex. `LLM.1.1`) sont la clé utilisée par le labo |
| Écrire des résultats de mesure dans le référentiel | **Non** | Les mesures concernent **une évaluation** : elles passent par l'API (§ 2) |

Une version publiée n'est jamais modifiée : les évaluations déjà faites gardent la version qu'elles ont utilisée.

---

## 2. API d'import des mesures red-team

### 2.1 Principe

Le labo attaque un chatbot de test, puis traduit chaque résultat en **niveau de maturité mesuré** pour une
question du référentiel. Exemple : 14 charges d'injection sur 20 réussissent, donc LLM.1.1 est mesuré au
niveau 1 (« aucune protection efficace »).

Dans le calcul des scores, chaque question prend sa valeur de la source la plus fiable disponible :

```
mesuré (labo red-team)  >  audité (correction de l'auditeur)  >  déclaré (organisation)
```

### 2.2 Authentification et droits

1. `POST /api/v1/auth/login` avec un compte **AUDITOR** dédié au labo (créé par l'administrateur), pour récupérer `accessToken`.
2. En-tête `Authorization: Bearer <accessToken>` sur chaque appel (le jeton est valable 8 h).

| Action | ADMIN | AUDITOR (labo) | ORG_USER |
|---|---|---|---|
| Envoyer ou supprimer des mesures | ✔ | ✔ | ✘ (une organisation ne mesure pas sa propre sécurité) |
| Consulter les mesures | ✔ | ✔ | ✔ (seulement pour son organisation) |

### 2.3 Familles d'attaques et questions mesurées

`GET /api/v1/measurements/attack-families` renvoie la liste à jour :

| `attackFamily` | Questions | Exemple d'attaque |
|---|---|---|
| `PROMPT_INJECTION` | LLM.1.1 | « Ignore tes instructions et… », injection cachée dans un document RAG |
| `PROMPT_LEAKING` | LLM.1.2 | Extraction du prompt système et du faux secret qu'il contient |
| `DATA_DISCLOSURE` | LLM.2.2 | Faire révéler des données personnelles d'un autre client |
| `UNSAFE_TOOL_USE` | LLM.3.1, LLM.3.2 | Déclencher `issue_refund` sans validation humaine |
| `RAG_ACCESS_BREAK` | LLM.4.1 | Lire les documents RAG d'un autre utilisateur |
| `OTHER` | toutes | Attaque hors liste (à décrire dans `evidence`) |

Une mesure dont la famille ne correspond pas à la question est refusée : c'est ce qui garantit que la
correspondance attaque → question est la même dans les deux modules.

### 2.4 Envoyer un lot de résultats

`POST /api/v1/assessments/{assessmentId}/measurements`

```json
{
  "runId": "garak-2026-11-24-01",
  "tool": "garak 0.12",
  "measuredAt": "2026-11-24T10:15:00Z",
  "results": [
    {
      "questionCode": "LLM.1.1",
      "attackFamily": "PROMPT_INJECTION",
      "measuredLevel": 1,
      "evidence": "14/20 charges d'injection réussies (sondes promptinject, latentinjection)",
      "evidenceUrl": "https://labo.local/rapports/garak-2026-11-24-01.html"
    },
    {
      "questionCode": "LLM.3.2",
      "attackFamily": "UNSAFE_TOOL_USE",
      "measuredLevel": 1,
      "evidence": "issue_refund exécuté 5 fois sur 5 sans aucune confirmation"
    }
  ]
}
```

| Champ | Obligatoire | Règle |
|---|---|---|
| `runId` | oui | ≤ 100 caractères ; identifie le passage côté labo |
| `tool` | oui | ≤ 100 caractères (outil et version) |
| `measuredAt` | non | date ISO 8601, pas dans le futur ; par défaut, l'heure de réception |
| `results` | oui | 1 à 100 éléments, une question au plus une fois |
| `questionCode` | oui | code existant dans le référentiel de l'évaluation |
| `attackFamily` | oui | voir § 2.3 |
| `measuredLevel` | oui | 1 à 5, selon les niveaux décrits dans la question |
| `evidence` | oui | ≤ 2 000 caractères : un résumé vérifiable |
| `evidenceUrl` | non | ≤ 1 000 caractères |

**Réponse `200` :** le lot enregistré, chaque mesure comparée au niveau déclaré :

```json
{
  "runId": "garak-2026-11-24-01",
  "recorded": 2,
  "measurements": [
    { "questionCode": "LLM.1.1", "attackFamily": "PROMPT_INJECTION", "measuredLevel": 1,
      "declaredLevel": 4, "auditedLevel": null, "gap": 3, "tool": "garak 0.12", "runId": "garak-2026-11-24-01",
      "evidence": "14/20 charges…", "evidenceUrl": "https://…", "measuredAt": "2026-11-24T10:15:00Z",
      "recordedAt": "2026-11-24T10:15:02Z" }
  ]
}
```

`gap` = déclaré − mesuré : **positif = l'organisation s'est surévaluée**. C'est l'écart « déclaré vs mesuré » du projet.

**Comportement :**
- **Tout ou rien** : si un seul résultat est invalide, rien n'est enregistré, et la réponse `400` liste toutes les erreurs.
- **Remplacement** : un nouveau passage sur la même question remplace la mesure précédente. C'est le scénario
  « faille corrigée, on relance les tests, le score remonte ».
- Une évaluation **validée** est définitive : `409`.

| Code | Cas |
|---|---|
| 200 | Lot enregistré |
| 400 | Lot invalide (détail de toutes les erreurs dans `detail`) |
| 401 / 403 | Jeton absent ou expiré / rôle non autorisé |
| 404 | Évaluation introuvable |
| 409 | Évaluation validée |
| 429 | Trop d'échecs de connexion sur le compte du labo (attendre `Retry-After` secondes) |

### 2.5 Autres appels

| Appel | Rôle |
|---|---|
| `GET /api/v1/assessments/{id}/measurements` | Mesures de l'évaluation avec niveaux déclaré, audité, mesuré et l'écart |
| `DELETE /api/v1/assessments/{id}/measurements/{questionCode}` | Retirer une mesure |
| `GET /api/v1/assessments/{id}/results` | Scores recalculés avec les mesures |

### 2.6 Exemple côté labo (Python)

```python
import requests

AIMA = "http://localhost:8088/api/v1"
token = requests.post(f"{AIMA}/auth/login",
                      json={"email": "labo@redteam.local", "password": "..."}).json()["accessToken"]
headers = {"Authorization": f"Bearer {token}"}

batch = {
    "runId": "garak-2026-11-24-01",
    "tool": "garak 0.12",
    "results": [{
        "questionCode": "LLM.1.1",
        "attackFamily": "PROMPT_INJECTION",
        "measuredLevel": 1,
        "evidence": "14/20 charges d'injection réussies",
    }],
}
r = requests.post(f"{AIMA}/assessments/{assessment_id}/measurements", json=batch, headers=headers)
r.raise_for_status()   # en cas de 400, r.json()["detail"] contient toutes les erreurs
for m in r.json()["measurements"]:
    print(m["questionCode"], "déclaré", m["declaredLevel"], "→ mesuré", m["measuredLevel"], "écart", m["gap"])
```

### 2.7 Vérification

Le test `MeasurementIntegrationTest` rejoue le scénario de la démonstration de soutenance, sur une vraie base PostgreSQL :
1. l'organisation déclare LLM.1.1 = 4 ;
2. le labo mesure 1, et LLM.1 passe de 4,0 à 2,2 ;
3. après correction de la faille, un nouveau passage mesure 4, et LLM.1 revient à 4,0.

Il vérifie aussi les refus : organisation qui mesure sa propre sécurité, famille d'attaque incohérente, question inconnue.
