# 07 — Frontend React

## 1. Stack

| Outil | Rôle |
|---|---|
| **React 19 + TypeScript** (mode `strict`) | Interface typée : les types de `src/api/types.ts` reproduisent les DTO Java |
| **Vite 8** | Serveur de développement instantané et build de production |
| **React Router 8** | Navigation (`/connexion`, `/`, `/evaluations/:id`, `/organisations`, `/utilisateurs`) |
| **TanStack Query 5** | Appels API, cache et rafraîchissement automatique après chaque modification |
| **Tailwind CSS 4** | Mise en forme, avec des jetons de couleur partagés et un mode sombre automatique |
| **Recharts 3** | Radar de maturité et barres d'écarts |
| **oxlint** | Analyse statique du code (0 avertissement) |

## 2. Architecture

```
src/
├── api/            client.ts (fetch + JWT + erreurs RFC 9457), types.ts, hooks.ts (TanStack Query)
├── auth/           AuthProvider (session), context.ts (useAuth), RequireAuth (pages protégées)
├── components/     Layout (navigation selon le rôle), ui.tsx (boutons, badges…), charts/
├── lib/format.ts   Formats français (nombres, dates) et libellés
└── pages/
    ├── LoginPage, AssessmentsPage, AdminPages (organisations, utilisateurs)
    └── assessment/ AssessmentPage + onglets Questionnaire, Résultats, Feuille de route
```

En développement, Vite relaie `/api/*` vers le backend (`localhost:8090`). Le navigateur
ne voit qu'une seule origine, ce qui évite les problèmes de CORS.

## 3. Écrans

| Écran | Fonctions |
|---|---|
| **Connexion** | E-mail et mot de passe. Le jeton JWT est gardé en `sessionStorage` (effacé à la fermeture de l'onglet) |
| **Évaluations** | Cartes avec statut, avancement des réponses et de la revue, niveau figé. Création d'une évaluation |
| **Questionnaire** | Navigation par domaine (réponses/total), 5 niveaux descriptifs par question, **enregistrement immédiat**, « Non applicable » avec justification, preuve et commentaire. Pour l'auditeur : **Confirmer** ou **Corriger le niveau** (commentaire obligatoire) |
| **Résultats** | 4 chiffres clés (score, maturité, complétion, indice de confiance), alerte « maillon faible », **radar actuel / cible**, **écarts triés**, tableau détaillé par sous-domaine, **réglage des cibles** |
| **Feuille de route** | 4 colonnes (Quick wins, court, moyen et long terme), actions triées par priorité |
| **Organisations / Utilisateurs** | Réservés à l'administrateur : création et liste |
| **Actions** | Rapport PDF, Soumettre (si 100 % de réponses), Rouvrir, Valider l'audit (si tout est revu), avec confirmation |

Captures : [évaluations](captures/evaluations.png) · [questionnaire](captures/questionnaire.png) ·
[résultats (sombre)](captures/resultats.png) · [résultats (clair)](captures/resultats-clair.png) ·
[feuille de route](captures/feuille-de-route.png)

## 4. Choix de conception

- **Les droits affichés suivent le rôle**, par exemple pas de bouton « Soumettre » pour un
  auditeur. Ce n'est qu'un confort : la vraie protection reste l'API, qui renvoie 401/403.
- **Erreurs lisibles** : les « Problem Details » de l'API sont affichés tels quels, avec le
  détail champ par champ.
- **Session expirée** : toute réponse 401 déconnecte l'utilisateur et vide le cache, pour
  qu'aucune donnée de la session précédente ne reste en mémoire.
- **Code splitting** : la page d'évaluation, l'administration et la bibliothèque de graphiques
  (≈ 420 ko) sont chargées à la demande. Le fichier JavaScript initial est passé de 820 ko à 374 ko.
- **Graphiques accessibles**, selon une méthode de visualisation de données :
  - 2 séries seulement (actuel en bleu plein, cible en orange **pointillé**) : la forme du
    trait distingue les séries même sans percevoir les couleurs ;
  - couleurs **validées par script** (séparation pour les daltoniens ΔE ≥ 24, contraste ≥ 3:1),
    en mode clair et en mode sombre ;
  - légende et libellés directs (« GOV 2,0 », « RISK N/A »), info-bulles au survol,
    description textuelle (`aria-label`) et tableau détaillé équivalent sous les graphiques ;
  - les niveaux 1 à 5 utilisent les couleurs de statut (rouge → vert), **toujours accompagnées
    du libellé** (« Niveau 2 — Reproductible »).
- **Mode sombre** automatique (réglage du système), avec ses propres jetons de couleur.
- **Responsive** : vérifié à 390 px de large (téléphone), sans défilement horizontal.

## 5. Vérification

Parcours automatisé dans Chrome (Puppeteer) : connexion, liste, réponse à une question,
résultats, feuille de route, vue mobile, administration. **Aucune erreur dans la console,
aucun débordement horizontal.** `npm run build` et `npm run lint` passent sans avertissement.
