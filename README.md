# AIMA — AI Security Maturity Assessment

Référentiel et plateforme web d'évaluation de la maturité des entreprises en
**cybersécurité et gouvernance de l'intelligence artificielle**.

Basé sur ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI 600-1 et
OWASP Top 10 for LLM Applications.

## Structure du dépôt

```
ai-maturity-assessment/
├── docs/                 Livrables méthodologiques (analyse, modèle, grille, étude de cas)
├── referential/          Référentiel AIMA au format JSON (source unique de vérité)
├── backend/              API Java 21 / Spring Boot          (à venir)
├── frontend/             Interface React / TypeScript       (à venir)
└── docker-compose.yml    Lancement complet                  (à venir)
```

## Documentation

1. [Cadrage du projet](docs/00-cadrage.md)
2. [Analyse comparative des normes](docs/01-analyse-comparative.md)
3. [Modèle de maturité et méthode de scoring](docs/02-modele-maturite.md)

## Stack technique

Java 21 · Spring Boot · Spring Security (JWT) · PostgreSQL · Flyway ·
React · TypeScript · Vite · Tailwind CSS · Recharts · Docker
