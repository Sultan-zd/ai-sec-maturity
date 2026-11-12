# 06 — Sécurité, mode audit et rapport PDF

## 1. Authentification par jeton JWT

```
Navigateur / client                         API AIMA
   │  POST /api/v1/auth/login {email, mot de passe}
   │ ─────────────────────────────────────────▶  vérifie le hash BCrypt
   │ ◀───────────────────────────────────────── { accessToken: "eyJhbGciOi…", expiresAt }
   │
   │  GET /api/v1/assessments
   │  Authorization: Bearer eyJhbGciOi…
   │ ─────────────────────────────────────────▶  vérifie la signature HMAC-SHA256 + l'expiration
   │ ◀───────────────────────────────────────── 200 (ou 401 / 403)
```

| Choix | Pourquoi |
|---|---|
| **JWT signé HS256** (Spring Security OAuth2 Resource Server + Nimbus) | Standard du marché, API sans état : pas de session côté serveur, et la montée en charge reste simple |
| Le jeton contient : identifiant, e-mail, **rôle**, **organisation** | Chaque requête sait qui est l'utilisateur, sans requête SQL |
| Durée de validité **8 h** | Une journée de travail ; au-delà, il faut se reconnecter |
| Mots de passe **BCrypt** (`{bcrypt}$2a$10$…`) | Hash lent et salé, résistant au cassage hors ligne |
| Secret et mot de passe admin en **variables d'environnement** | Aucun secret de production dans le code (`AIMA_JWT_SECRET`, `AIMA_ADMIN_PASSWORD`) |
| CSRF désactivé, sessions `STATELESS` | Pas de cookie de session, donc pas de vulnérabilité CSRF |
| CORS limité à `http://localhost:5173` | Seul le frontend AIMA peut appeler l'API depuis un navigateur |

### Protections contre les attaques classiques
- **Énumération de comptes** : « mauvais mot de passe » et « e-mail inconnu » renvoient
  exactement la même réponse, dans le même temps (un hash factice est vérifié quand
  l'e-mail n'existe pas).
- **Jeton falsifié** : une signature modifiée donne 401 (testé).
- **Politique de mot de passe** : 10 caractères minimum, au moins une lettre et un chiffre.
- **E-mails uniques sans tenir compte de la casse** : index unique sur `LOWER(email)`.
- **Clickjacking** : en-tête `Content-Security-Policy: frame-ancestors 'none'`.

## 2. Rôles et matrice des droits

| Action | ADMIN | ORG_USER (sa propre organisation) | AUDITOR |
|---|:-:|:-:|:-:|
| Gérer les utilisateurs | ✔ | ✘ | ✘ |
| Créer une organisation | ✔ | ✘ | ✘ |
| Consulter organisations et évaluations | toutes | **la sienne uniquement** | toutes |
| Créer une évaluation, répondre, soumettre, rouvrir | ✔ | ✔ | ✘ |
| Revoir et corriger les réponses, valider l'audit | ✔ | ✘ | ✔ |
| Résultats, feuille de route, rapport PDF | ✔ | ✔ | ✔ |

Deux niveaux de contrôle :
1. **Rôle** : `@PreAuthorize("hasAnyRole('ADMIN','ORG_USER')")` sur les contrôleurs.
2. **Données** : `AccessGuard.checkOrganization()` dans les services. Un ORG_USER qui
   tente d'accéder à l'évaluation d'une autre organisation reçoit **403**, même s'il
   connaît son identifiant. Les listes sont filtrées automatiquement.

Ce deuxième niveau protège contre la vulnérabilité n° 1 du classement
**OWASP API Security Top 10 : BOLA** (*Broken Object Level Authorization*).

Au tout premier démarrage, un administrateur est créé (`admin@aima.local`). Il crée
ensuite les organisations, les comptes ORG_USER et les auditeurs.

## 3. Mode audit

```
 DRAFT ──soumettre──▶ SUBMITTED ──(l'auditeur revoit chaque réponse)──valider──▶ VALIDATED
   ▲                     │                                                       (définitif)
   └──────rouvrir────────┘  (annule les revues déjà commencées)
```

Pour chaque réponse, l'auditeur décide :
- **CONFIRMED** : la réponse est confirmée au vu des preuves ;
- **ADJUSTED** : il retient un autre niveau (`auditedLevel`), avec un **commentaire
  obligatoire** pour justifier la correction.

Le calcul utilise le **niveau effectif** : le niveau audité s'il existe, sinon le niveau
déclaré. Le niveau déclaré reste conservé, ce qui garantit la traçabilité de l'écart
entre déclaration et constat.

La validation exige que **toutes** les réponses aient été revues. Elle fige le score
audité, l'auditeur (`validated_by`) et la date (`validated_at`). Une évaluation validée
n'est plus modifiable.

**Exemple testé** : l'organisation déclare LLM.3.2 au niveau 1, ce qui plafonne LLM à
2,0 (score global 2,85). L'auditeur constate un workflow d'approbation et corrige à 3.
Le plafond disparaît et le score global audité devient **3,00**.

## 4. Rapport PDF

`GET /api/v1/assessments/{id}/report.pdf` produit un rapport A4 avec OpenPDF :

| Page | Contenu |
|---|---|
| 1 | Identification (organisation, périmètre, statut, référentiel, complétion, indice de confiance), score global coloré par niveau, **graphique radar vectoriel** (actuel / cible), alerte sur les domaines plafonnés |
| 2 | Tableau par domaine (poids, score, niveau coloré, cible, écart, plafonnement), **feuille de route** par horizon |
| 3+ | Annexe : chaque réponse avec la question, le niveau déclaré, le niveau retenu par l'auditeur, la preuve et les commentaires |

- Pied de page sur chaque page : organisation, mention « Document confidentiel », numéro.
- Le rapport passe par les mêmes contrôles d'accès que l'API : un ORG_USER ne peut pas
  télécharger le rapport d'une autre organisation (testé).
- Exemple généré : [exemple-rapport.pdf](exemple-rapport.pdf).

Point technique : les polices PDF standard (Helvetica) utilisent l'encodage Windows-1252,
qui couvre le français mais pas certains symboles (`↔`, `≥`, `−`). La classe
`PdfStyle.clean()` les remplace par un équivalent (`<->`, `>=`, `-`) au lieu d'afficher « ? ».

## 5. Essayer dans Swagger

1. Ouvrir http://localhost:8090/swagger-ui.html
2. `POST /api/v1/auth/login` avec `{"email":"admin@aima.local","password":"Admin@2026!"}`
3. Copier `accessToken`, cliquer sur **Authorize** et le coller
4. Tous les endpoints sont alors utilisables

## 6. Tests (31 au total, +10 à cette étape)

| Classe | Ce qui est vérifié |
|---|---|
| `SecurityIntegrationTest` (7) | JWT valide, `/me`, même réponse pour un mauvais mot de passe et un e-mail inconnu, jeton falsifié rejeté, endpoints publics, droits par rôle, règles de création de compte, **cloisonnement complet entre deux organisations** |
| `AssessmentFlowIntegrationTest` (3) | Auto-évaluation, puis audit complet (corrections, validation, score audité), réouverture qui annule les revues, état définitif après validation |
| `ReportIntegrationTest` (1) | PDF valide (`%PDF-`), au moins 3 pages, contenu relu avec un extracteur de texte, symboles remplacés, nom de fichier |
