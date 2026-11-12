# 10 — Synthèse du projet, bilan et perspectives

## 1. Le projet en une page

**Problème.** Les entreprises adoptent l'IA (chatbots, modèles prédictifs, IA générative) plus vite qu'elles ne
l'encadrent. Les normes existantes sont complémentaires (ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI
600-1, OWASP Top 10 LLM), mais **aucune ne donne de score de maturité chiffré** qui couvre à la fois la
gouvernance et la sécurité technique de l'IA.

**Réponse.** AIMA (*AI Security Maturity Assessment*) se compose de deux parties :

1. **Un référentiel** : 8 domaines, 29 sous-domaines, 59 questions, 5 niveaux de maturité, et une méthode de
   scoring avec la règle du maillon faible et une priorisation des actions.
2. **Une plateforme web** : auto-évaluation, audit contradictoire, scoring, radar actuel / cible, feuille de
   route en 4 horizons et rapport PDF.

## 2. Livrables

| # | Livrable demandé | Où le trouver |
|---|---|---|
| 1 | Analyse comparative des normes | [01-analyse-comparative.md](01-analyse-comparative.md) |
| 2 | Modèle de maturité et méthode de scoring | [02-modele-maturite.md](02-modele-maturite.md), [referentiel-v1.json](../referential/referentiel-v1.json) |
| 3 | Grille d'audit / questionnaire | [03-grille-audit.md](03-grille-audit.md), [03b — 59 questions](03b-grille-audit-detaillee.md) |
| 4 | Plateforme web | `backend/`, `frontend/`, [04](04-architecture-backend.md) à [08](08-docker-ci.md) |
| 5 | Étude de cas | [09-etude-de-cas.md](09-etude-de-cas.md) et son [rapport PDF](etude-de-cas/rapport-banque-atlas.pdf) |

## 3. Architecture globale

```
 ┌────────────── Référentiel (JSON versionné, source unique) ──────────────┐
 │ referentiel-v1.json + questions/*.json  ──▶ scripts/referential-tool.mjs │──▶ docs/03b (grille générée)
 └───────────────────────────────┬──────────────────────────────────────────┘
                                 │ importé et validé au démarrage
 Navigateur ──▶ Nginx ──/api──▶ Backend Spring Boot ──▶ PostgreSQL
   (React)      (frontend)       ├─ referential  : import, lecture du référentiel
                                 ├─ assessment   : évaluations, réponses, audit
                                 ├─ scoring      : moteur de calcul (Java pur, testé)
                                 ├─ report       : rapport PDF (OpenPDF, radar vectoriel)
                                 ├─ security/user: JWT, rôles, anti-force brute, cloisonnement
                                 └─ organization : organisations clientes
```

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA / Hibernate 7, Flyway, OpenPDF, springdoc (Swagger) |
| Base de données | PostgreSQL 17 (3 migrations, 14 tables) |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query, React Router, Recharts |
| Tests | JUnit 5, AssertJ, MockMvc, Testcontainers (PostgreSQL réel) |
| Exploitation | Docker (images multi-étapes non-root), Docker Compose, Nginx, GitHub Actions, Dependabot |

**Volume.** 67 classes Java (≈ 4 600 lignes), 11 classes de test (≈ 1 100 lignes, 35 tests), 22 fichiers
TypeScript (≈ 1 900 lignes), 27 endpoints REST, 11 documents de méthode.

## 4. Choix structurants et justification

| Choix | Pourquoi |
|---|---|
| Référentiel en **JSON séparé du code** | Faire évoluer les questions et les poids sans recompiler ; une seule source pour l'application et la documentation |
| Référentiel **versionné et immuable** | Une évaluation reste liée à sa version : on peut comparer deux évaluations dans le temps |
| Moteur de scoring **sans dépendance** (Java pur) | Déterministe, testable à la main, facile à expliquer |
| Calculs en **BigDecimal** | Éviter les erreurs d'arrondi des nombres à virgule (bug réel trouvé : 44,999… au lieu de 45) |
| **Règle du maillon faible** | Une faille critique ne doit pas être masquée par des moyennes (étude de cas : TRUST plafonné) |
| **Mode audit** (déclaré / retenu) | Une auto-évaluation est déclarative ; l'audit la rend crédible et traçable |
| Schéma géré par **Flyway**, Hibernate en `validate` | Schéma versionné comme le code, jamais modifié en cachette |
| Identifiants **UUID** et contrôle d'accès par organisation | Protection contre l'accès aux données d'une autre organisation (OWASP API1 « BOLA »), testée |
| **Problem Details** (RFC 9457) | Format d'erreur standard, lisible par le frontend |

## 5. Sécurité de la plateforme (synthèse)

| Menace (OWASP) | Mesure |
|---|---|
| A01 / API1 — Contrôle d'accès | Rôles `@PreAuthorize` + vérification de l'organisation à chaque accès (`AccessGuard`) ; tests dédiés |
| A02 — Cryptographie | Mots de passe BCrypt ; JWT HMAC-SHA256 avec un secret d'au moins 256 bits, fourni par variable d'environnement |
| A03 — Injection | Requêtes JPA paramétrées ; validation des entrées (Bean Validation) ; CSP stricte côté navigateur |
| A05 — Configuration | En-têtes de sécurité Nginx, actuator non exposé, base non publiée, conteneurs non-root |
| A06 — Composants vulnérables | Dependabot ; `npm audit` : 0 vulnérabilité |
| A07 — Authentification | Message identique et temps constant (pas d'énumération des comptes) ; **blocage après 5 échecs en 15 min (429)** |
| A09 — Journalisation | Traçabilité des revues (qui, quand, pourquoi) ; dates de soumission et de validation |

## 6. Démarche qualité

- **Tests à trois niveaux.** Tests unitaires du moteur, avec des valeurs calculées à la main ; tests
  d'intégration de l'API sur un vrai PostgreSQL ; parcours complets (auto-évaluation, audit, validation).
- **Bugs réels détectés par les tests** et documentés : booléen absent refusé par Jackson 3, arrondi en
  virgule flottante, CORS derrière le proxy Nginx (403), tri de l'annexe du PDF.
- **Vérification visuelle** dans un vrai navigateur (Chrome automatisé) : captures, mobile, mode sombre,
  accessibilité des couleurs pour les personnes daltoniennes.
- **Intégration continue** à chaque push. **Documentation as code** : la grille d'audit est régénérée depuis le
  JSON, et la CI vérifie qu'elle est à jour.

## 7. Bilan

**Objectifs atteints.** Les 5 livrables sont produits. La plateforme fonctionne de bout en bout et se lance
avec une seule commande. L'étude de cas démontre la méthode sur un cas réaliste.

**Limites connues** (voir aussi [09 §6](09-etude-de-cas.md)) :
- la priorité dépend du poids du domaine, ce qui peut repousser une action critique et facile ;
- le score ne tient pas compte du niveau de preuve (l'indice de confiance est affiché à côté, sans effet) ;
- les poids sont les mêmes pour tous les secteurs ;
- le blocage anti-force brute est stocké en mémoire (une seule instance du backend) ;
- le jeton JWT est conservé dans le navigateur, sans « refresh token » ni révocation avant expiration (8 h).

## 8. Perspectives

| Court terme | Moyen terme |
|---|---|
| Règle « critique + effort faible ⇒ quick win » | Profils de pondération sectoriels (banque, santé, secteur public) |
| Score « prouvé » à côté du score « déclaré » | Comparaison de deux évaluations (progression dans le temps) |
| Dépôt de fichiers de preuve (au lieu d'une référence texte) | Benchmark anonymisé entre organisations d'un même secteur |
| Refresh token et révocation des sessions | Correspondance automatique avec l'AI Act (obligations par classe de risque) |
| Export Excel des réponses | Authentification unique (SSO, OpenID Connect) pour les grandes entreprises |
