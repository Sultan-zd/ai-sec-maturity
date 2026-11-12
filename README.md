# AIMA — AI Security Maturity Assessment

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
├── scripts/              Outil de validation et de génération de la grille
├── backend/              API Java 21 / Spring Boot 4 (port 8090)
├── frontend/             Interface React / TypeScript       (à venir)
└── docker-compose.yml    Lancement complet                  (à venir)
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

## Mettre à jour le référentiel

```bash
node scripts/referential-tool.mjs   # valide le JSON et régénère docs/03b
```

## Stack technique

Java 21 · Spring Boot · Spring Security (JWT) · PostgreSQL · Flyway ·
React · TypeScript · Vite · Tailwind CSS · Recharts · Docker

## Lancer le backend

Prérequis : Java 21 et Docker Desktop démarré.

```bash
cd backend
./mvnw spring-boot:run    # API sur http://localhost:8090 — Swagger : /swagger-ui.html
./mvnw test               # 31 tests unitaires + intégration (PostgreSQL via Testcontainers)
```

## Comptes

Au premier démarrage, un administrateur est créé : `admin@aima.local` / `Admin@2026!`
(à changer en production via la variable `AIMA_ADMIN_PASSWORD`). Il crée ensuite les
organisations, les utilisateurs `ORG_USER` et les auditeurs (`AUDITOR`).
