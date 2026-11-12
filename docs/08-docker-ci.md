# 08 — Conteneurisation et intégration continue

## 1. Objectif

- **Une seule commande** pour lancer toute la plateforme sur n'importe quel poste :
  `docker compose up -d --build`. Seul Docker est requis : pas de Java, Node ni PostgreSQL à installer.
- **Une vérification automatique** de chaque modification du code (GitHub Actions) :
  si un test échoue, le problème est visible avant la fusion dans `main`.

## 2. Architecture de déploiement

```
                Poste / serveur
 Navigateur ──▶ :8088 ┌──────────────────────── réseau « frontend » ────────┐
                      │  frontend (Nginx, non-root)                         │
                      │   /            → fichiers React (dist/)             │
                      │   /api/**, /swagger-ui, /v3/api-docs  ──▶ backend   │
                      └──────────────────────────────────────┬──────────────┘
                                                             │
                      ┌──────────── réseau « backend » ──────┴──────────────┐
                      │  backend (Spring Boot, JRE 21, non-root) :8090      │
                      │        │ JDBC                                       │
                      │  postgres 17 ── volume « pgdata » (données durables)│
                      └─────────────────────────────────────────────────────┘
```

| Service | Image | Port publié | Rôle |
|---|---|---|---|
| `postgres` | `postgres:17-alpine` | aucun | Base de données, volume persistant |
| `backend` | `aima-backend` (≈ 420 Mo) | aucun | API REST, migrations Flyway, import du référentiel |
| `frontend` | `aima-frontend` (≈ 80 Mo) | `8088` | Nginx : interface + reverse proxy de l'API |

Le service `frontend` est le **seul point d'entrée**. La base de données n'est pas joignable depuis l'extérieur, et
le frontend lui-même ne peut pas l'atteindre, car il n'est pas sur le réseau `backend`. C'est le principe de la
**segmentation réseau** (défense en profondeur).

Ordre de démarrage, garanti par les *healthchecks* :
`postgres` prêt (`pg_isready`), puis `backend` sain (`/actuator/health` = `UP`), puis `frontend` démarre.

## 3. Les images Docker

### 3.1 Backend : build multi-étapes (`backend/Dockerfile`)

1. **Étape `build`** (JDK 21) : le Maven Wrapper télécharge les dépendances (couche mise en cache tant que
   `pom.xml` ne change pas), compile, puis découpe le jar en **couches** : dépendances, chargeur Spring Boot,
   code de l'application.
2. **Étape d'exécution** (JRE 21 Alpine) : seules les couches sont copiées, sans JDK, sans Maven et sans code
   source. Quand on modifie seulement le code, Docker ne retransfère que la petite couche `application`.

Le contexte de build est la racine du dépôt, car le référentiel JSON (`referential/`) est embarqué dans le jar. Le
fichier `.dockerignore` n'envoie au build que `backend/` et `referential/`.

### 3.2 Frontend (`frontend/Dockerfile` + `nginx.conf`)

1. `npm ci` (installation exacte selon `package-lock.json`), puis `npm run build` (vérification TypeScript et Vite).
2. L'image finale est `nginx-unprivileged` : seuls les fichiers statiques sont copiés, Node.js n'est pas présent.

Nginx assure :
- **Le reverse proxy** : `/api` est relayé vers `backend:8090`. Le navigateur ne voit qu'une seule origine, il n'y a
  donc pas besoin de CORS.
- **Le routage de la SPA** : toute route inconnue (`/evaluations/…`) renvoie `index.html`, puis React Router prend le
  relais.
- **Le cache** : les fichiers `/assets/*` portent un hash dans leur nom et sont mis en cache un an ; `index.html`
  n'est jamais mis en cache.
- **Les en-têtes de sécurité** (OWASP Secure Headers) :

| En-tête | Protection |
|---|---|
| `Content-Security-Policy` (`default-src 'self'`) | Injection de scripts (XSS) : seuls les scripts du site s'exécutent |
| `X-Frame-Options: DENY`, `frame-ancestors 'none'` | Clickjacking (le site ne peut pas être intégré dans une iframe) |
| `X-Content-Type-Options: nosniff` | Interprétation d'un fichier sous un autre type |
| `Referrer-Policy`, `Permissions-Policy` | Fuite d'URL, accès caméra / micro / géolocalisation |
| `server_tokens off` | Masque la version de Nginx |

### 3.3 Bonnes pratiques appliquées

| Pratique | Mise en œuvre |
|---|---|
| Processus non-root | Utilisateur `aima` (backend), image `nginx-unprivileged` (frontend) |
| Images minimales | JRE Alpine, Nginx Alpine ; ni outils de build ni sources dans l'image finale |
| Secrets hors du code | Fichier `.env` (ignoré par Git) ; `.env.example` sert de modèle |
| Échec explicite | `${AIMA_JWT_SECRET:?…}` : Compose refuse de démarrer si un secret manque |
| Surveillance de l'état | `HEALTHCHECK` dans chaque image, `depends_on: condition: service_healthy` |
| Redémarrage automatique | `restart: unless-stopped` |
| Mémoire JVM adaptée au conteneur | `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` |
| Actuator non exposé | Nginx ne relaie pas `/actuator` ; le healthcheck l'interroge en interne |

## 4. Configuration (`.env`)

| Variable | Rôle |
|---|---|
| `POSTGRES_PASSWORD` | Mot de passe de la base |
| `AIMA_JWT_SECRET` | Clé de signature des jetons JWT (≥ 32 caractères aléatoires : `openssl rand -base64 48`) |
| `AIMA_ADMIN_EMAIL` / `AIMA_ADMIN_PASSWORD` | Premier compte administrateur, créé au premier démarrage |
| `AIMA_HTTP_PORT` | Port publié sur le poste (défaut 8088) |

## 5. Intégration continue (`.github/workflows/ci.yml`)

Le workflow se déclenche à chaque `push` sur `main` et à chaque *pull request*. Il comporte 4 jobs :

```
 referential ─┐
 backend ─────┼──▶ docker
 frontend ────┘
```

| Job | Vérifications |
|---|---|
| **Référentiel** | Cohérence du JSON (codes uniques, 5 niveaux, poids = 100 %…) et grille `docs/03b` à jour avec le JSON |
| **Backend** | `./mvnw verify` : 35 tests unitaires et d'intégration sur un vrai PostgreSQL (Testcontainers). Les rapports de tests sont archivés. |
| **Frontend** | `npm ci`, lint (oxlint), vérification TypeScript et build de production |
| **Docker** | Construction des deux images (cache GitHub Actions), seulement si les tests passent |

Les autres choix du workflow :
- **Moindre privilège** : `permissions: contents: read`. Le workflow ne peut ni écrire dans le dépôt ni publier.
- **`concurrency`** : un nouveau push annule l'exécution précédente de la même branche.
- **Dependabot** (`.github/dependabot.yml`) : chaque semaine, il propose des *pull requests* de mise à jour pour
  Maven, npm, les images Docker et les actions GitHub. Ces PR passent par la même CI. C'est la réponse au risque
  OWASP A06 (« composants vulnérables et obsolètes »).

## 6. Vérifications effectuées

| Vérification | Résultat |
|---|---|
| `docker compose up --wait` | 3 conteneurs *healthy* |
| Appel API sans jeton | `401` |
| Connexion puis `GET /api/v1/referentials/active` | 8 domaines, 59 questions |
| Route SPA `/evaluations/xyz` | `200` (index.html) |
| `/actuator/health` depuis l'extérieur | Non relayé (renvoie l'application, pas l'actuator) |
| Port PostgreSQL publié | Aucun |
| Utilisateur du processus backend | `uid=100(aima)`, non-root |
| Parcours navigateur (Chrome) : connexion, liste des évaluations, Swagger UI | OK, aucune erreur console, compatible avec la CSP |
| Étapes de la CI rejouées en local | Référentiel OK, 31/31 tests, lint 0 avertissement, build OK (dans un conteneur Linux) |

**Bug trouvé pendant les tests.** Dans le navigateur, la connexion renvoyait `403`, alors que la même requête
fonctionnait avec `curl`. Le navigateur envoie un en-tête `Origin: http://localhost:8088`, mais Nginx transmettait
`Host: localhost` sans le port (`$host`). Spring voyait donc deux origines différentes et appliquait la politique
CORS. La correction consiste à transmettre `$http_host`, qui conserve le port : la requête est alors reconnue comme
venant de la même origine.

## 7. Commandes utiles

```bash
docker compose up -d --build        # construire et démarrer
docker compose ps                   # état et santé des services
docker compose logs -f backend      # journaux du backend
docker compose down                 # arrêter (les données sont conservées)
docker compose down -v              # arrêter ET effacer la base
```
