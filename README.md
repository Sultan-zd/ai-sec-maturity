# AIMA — AI Security Maturity Assessment

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

Référentiel et plateforme web d'évaluation de la maturité des entreprises en
**cybersécurité et gouvernance de l'intelligence artificielle**.

Basé sur ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI 600-1 et
OWASP Top 10 for LLM Applications.

## Structure du dépôt

```
PFS/
├── docs/                 Livrables méthodologiques (analyse, modèle, grille, étude de cas)
├── referential/          Référentiel AIMA au format JSON (source unique de vérité)
│   └── questions/        Questionnaire : un fichier par domaine
├── scripts/              Outil de validation du référentiel et scénario d'étude de cas
├── backend/              API Java 21 / Spring Boot 4 (port 8090)
├── frontend/             Interface React 19 / TypeScript (port 5173)
├── .github/              Intégration continue (GitHub Actions) et Dependabot
└── docker-compose.yml    Lancement complet : PostgreSQL + backend + frontend (Nginx)
```

## Documentation

1. [Cadrage du projet](docs/00-cadrage.md)
2. [Analyse comparative des normes](docs/01-analyse-comparative.md)
3. [Modèle de maturité et méthode de scoring](docs/02-modele-maturite.md)
4. [Grille d'audit : méthodologie](docs/03-grille-audit.md)
5. [Grille d'audit détaillée — 59 questions](docs/03b-grille-audit-detaillee.md)
6. [Architecture du backend](docs/04-architecture-backend.md)
7. [Évaluations, scoring et feuille de route](docs/05-evaluations-scoring.md)
8. [Sécurité, mode audit et rapport PDF](docs/06-securite-audit-rapport.md)
9. [Frontend React](docs/07-frontend.md)
10. [Conteneurisation et intégration continue](docs/08-docker-ci.md)
11. [Étude de cas : Banque Atlas Digitale](docs/09-etude-de-cas.md)
12. [Synthèse, bilan et perspectives](docs/10-synthese.md)
13. [Plan de soutenance et démonstration](docs/11-soutenance.md)
14. [Contrats entre les modules (référentiel, API red-team)](docs/12-contrats-modules.md)

## Modules de gouvernance et de preuve

Ces modules complètent la plateforme et sont documentés dans
[`docs/mohamed/CONTRIB.md`](docs/mohamed/CONTRIB.md).

| Module | Commande | Livrable |
|---|---|---|
| Classification EU AI Act | `python3 mohamed/eu-ai-act/eu_ai_act.py --demo` | `mohamed/eu-ai-act/eu-ai-act-report.md` |
| Assistant de preuves (LLM) | `python3 mohamed/evidence-assistant/assistant.py --document <doc>` | Suggestions JSON + revue auditeur |
| Revue auditeur | `python3 mohamed/evidence-assistant/review.py --input suggestions.json` | `suggestions.reviewed.json` |
| Validation AHP et sensibilité | `python3 mohamed/ahp/ahp_weights.py` | [`docs/mohamed/ahp-sensitivity.md`](docs/mohamed/ahp-sensitivity.md) |
| Matrice ISO/IEC 42001 (38 contrôles) | `python3 mohamed/iso42001/iso42001_matrix.py` | [`docs/mohamed/iso42001-matrix.md`](docs/mohamed/iso42001-matrix.md) |
| Fiabilité inter-évaluateurs | `node scripts/case-study/multi-respondent.mjs` | [`docs/mohamed/inter-rater-reliability.md`](docs/mohamed/inter-rater-reliability.md) |

Les tests de ces modules s'exécutent sans réseau ni clé API :

```bash
python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py'
python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py'
python3 mohamed/iso42001/iso42001_matrix.py --check
```

## Démarrage rapide (Docker)

Prérequis : Docker Desktop uniquement.

```bash
cp .env.example .env          # puis remplacer les secrets
docker compose up -d --build  # premier lancement : quelques minutes
```

Application : http://localhost:8088 · Swagger : http://localhost:8088/swagger-ui.html
Arrêt : `docker compose down` (ajouter `-v` pour effacer aussi la base).

## Mettre à jour le référentiel

```bash
(cd scripts && npm ci)            # une seule fois : installe le validateur de schéma
node scripts/referential-tool.mjs   # vérifie le contrat (referential/schema/) et la cohérence, régénère docs/03b
```

## Stack technique

Java 21 · Spring Boot · Spring Security (JWT) · PostgreSQL · Flyway ·
React · TypeScript · Vite · Tailwind CSS · Recharts · Docker

## Développement (sans Docker pour l'application)

### Backend

Prérequis : Java 21 et Docker Desktop démarré.

```bash
cd backend
./mvnw spring-boot:run    # API sur http://localhost:8090 — Swagger : /swagger-ui.html
./mvnw test               # 39 tests unitaires + intégration (PostgreSQL via Testcontainers)
```

## Comptes

Au premier démarrage, un administrateur est créé : `admin@aima.local` / `Admin@2026!` en
développement, ou les valeurs `AIMA_ADMIN_EMAIL` / `AIMA_ADMIN_PASSWORD` du fichier `.env` avec Docker. Il crée ensuite les
organisations, les utilisateurs `ORG_USER` et les auditeurs (`AUDITOR`).

### Frontend

```bash
cd frontend
npm install
npm run dev               # http://localhost:5173 (backend requis sur le port 8090)
```
