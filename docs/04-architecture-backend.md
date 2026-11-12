# 04 — Architecture du backend (Java / Spring Boot)

## 1. Stack

| Élément | Version / choix |
|---|---|
| Langage | Java 21 (LTS) |
| Framework | Spring Boot 4.1 (Spring Framework 7, Hibernate 7, Jackson 3) |
| Build | Maven (via Maven Wrapper `mvnw` : aucune installation nécessaire) |
| Base de données | PostgreSQL 17 (Docker) |
| Migrations | Flyway |
| Documentation API | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, AssertJ, MockMvc, Testcontainers |

## 2. Organisation du code : par fonctionnalité

```
ma.uca.aima
├── common/                    Éléments transverses (exceptions HTTP…)
├── config/                    Configuration (OpenAPI…)
└── referential/               Fonctionnalité « Référentiel »
    ├── model/                 Entités JPA (tables ref_*)
    ├── repository/            Accès aux données (Spring Data JPA)
    ├── importer/              Lecture + validation + import du JSON
    ├── service/               Logique métier de lecture
    └── api/                   Contrôleur REST + DTO + mapper
```

Les prochaines fonctionnalités suivront le même modèle : `assessment/` (évaluations),
`scoring/` (moteur de calcul), `report/` (PDF), `security/` (authentification).

**Pourquoi par fonctionnalité ?** Tout ce qui concerne le référentiel est au même
endroit. Le code est plus facile à comprendre, à tester et à faire évoluer, et une
fonctionnalité pourrait même être extraite plus tard en microservice.

## 3. Flux au démarrage

```
Démarrage Spring Boot
  │
  ├─ (dev) Docker Compose démarre PostgreSQL         ← spring-boot-docker-compose
  ├─ Flyway applique les migrations V1, V2…          ← schéma versionné
  ├─ Hibernate VÉRIFIE que les entités = schéma      ← ddl-auto: validate
  └─ ReferentialStartupImporter
        ├─ ReferentialLoader     lit referential/*.json (embarqué dans le jar)
        ├─ ReferentialValidator  vérifie ~15 règles de cohérence
        │      └─ erreur ? → l'application REFUSE de démarrer (fail fast)
        └─ ReferentialImportService
               ├─ version déjà en base ? → rien à faire (idempotent)
               └─ sinon → insertion + activation de la nouvelle version
```

## 4. Modèle de données (référentiel)

```
ref_referential (code, version, active)
 ├── ref_source              normes sources (ISO42001, OWASP_LLM…)
 ├── ref_maturity_level      5 niveaux + score minimal
 ├── ref_scoring_cap         règles du maillon faible
 └── ref_domain              8 domaines (poids)
      └── ref_subdomain      29 sous-domaines
           ├── ref_subdomain_reference   correspondance avec les clauses des normes
           └── ref_question               59 questions (criticité, poids, effort)
                └── ref_question_level    5 niveaux + recommandation
```

Choix importants :
- **Versionnage immuable** : une version (`code` + `version`) n'est jamais modifiée.
  Les évaluations futures pointeront vers une version précise, ce qui permet de
  comparer des évaluations dans le temps.
- **Un seul référentiel actif**, garanti par un index unique partiel PostgreSQL
  (`WHERE active`), donc au niveau de la base et pas seulement dans le code.
- **Contraintes `CHECK`** : un niveau hors de 1 à 5 ou une criticité invalide sont
  refusés par la base elle-même.

## 5. API REST (v1)

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/v1/referentials` | Liste des versions (résumé + nombre de questions) |
| GET | `/api/v1/referentials/active` | Référentiel actif complet (arbre domaines → questions) |
| GET | `/api/v1/referentials/{id}` | Une version complète |
| GET | `/api/v1/referentials/{id}/questions/{code}` | Détail d'une question |

Documentation interactive : **http://localhost:8090/swagger-ui.html**

Les erreurs suivent le standard **RFC 9457 « Problem Details »** :

```json
{ "status": 404, "title": "Ressource introuvable",
  "detail": "Question NOPE.1.1 introuvable",
  "instance": "/api/v1/referentials/1/questions/NOPE.1.1" }
```

## 6. Bonnes pratiques appliquées

| Pratique | Où |
|---|---|
| DTO séparés des entités (le contrat d'API ne dépend pas de la base) | `ReferentialDtos`, `ReferentialMapper` |
| `open-in-view: false` : pas de requêtes SQL cachées dans la couche web | `application.yml` |
| Transactions en lecture seule pour les consultations | `ReferentialQueryService` |
| Chargement groupé des collections (`default_batch_fetch_size`) contre le problème N+1 | `application.yml` |
| Lecture JSON stricte (propriété inconnue = erreur) | `ReferentialLoader` |
| Injection par constructeur (pas de `@Autowired` sur les champs) | Toutes les classes |
| Tests d'intégration sur un **vrai** PostgreSQL (pas de H2) | `ReferentialIntegrationTest` |

## 7. Lancer le backend

Prérequis : Java 21 et Docker Desktop démarré.

```bash
cd backend
./mvnw spring-boot:run      # démarre PostgreSQL + l'API sur http://localhost:8090
./mvnw test                 # lance les 10 tests (unitaires + intégration)
```
