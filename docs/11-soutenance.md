# 11 — Plan de soutenance et scénario de démonstration

Durée de référence : **20 min de présentation + 10 min de questions**. Adapter les durées au format imposé.

## 1. Plan de la présentation

| # | Partie | Durée | Contenu clé | Support |
|---|---|---|---|---|
| 1 | Contexte et problème | 2 min | L'IA crée des risques nouveaux : injection de prompt, shadow AI, biais, agents trop autonomes. Question d'une direction : « Où en sommes-nous, et que faire en priorité ? » | 1 exemple concret (chatbot qui bloque une carte) |
| 2 | Analyse des normes | 3 min | 7 référentiels, qui se complètent ; **aucun ne donne de score de maturité couvrant gouvernance et technique** : c'est ce que nous apportons | Tableau comparatif (doc 01) |
| 3 | Modèle de maturité | 4 min | 8 domaines, 5 niveaux (CMMI), moyenne pondérée par la criticité, **règle du maillon faible**, formule de priorité | Exemple de calcul (doc 02, doc 03 §exemple) |
| 4 | Grille d'audit | 1 min | 59 questions ; 5 situations décrites par question ; preuves attendues ; recommandations par niveau | Une question projetée en entier |
| 5 | **Démonstration** | 6 min | Voir §2 | Application en direct |
| 6 | Architecture et qualité | 2 min | Stack, sécurité (OWASP), 35 tests, CI, Docker, bugs trouvés grâce aux tests | Schéma (doc 10 §3) |
| 7 | Étude de cas et bilan | 2 min | Banque Atlas : 2,38/5 ; 3 constats ; apport de l'audit ; limites du modèle ; perspectives | Radar + tableau (doc 09) |

**Message à faire retenir :** *« AIMA transforme des normes abstraites en un score mesurable, vérifiable par un
auditeur, et en un plan d'action priorisé. »*

## 2. Scénario de démonstration (6 min)

**Préparation (la veille) :**

```bash
docker compose up -d --build
node scripts/case-study/run.mjs      # crée la Banque Atlas déjà évaluée et auditée
```

Prévoir aussi un compte `ORG_USER` sur une **deuxième organisation vide** pour la démonstration en direct,
et ouvrir les onglets du navigateur à l'avance.

| Temps | Action à l'écran | Ce qu'on dit |
|---|---|---|
| 0:00 | Connexion en administrateur → menu Organisations, Utilisateurs | « Trois rôles : administrateur, organisation, auditeur. » |
| 0:45 | Connexion en organisation → créer une évaluation | « Le référentiel actif est figé dans l'évaluation, ce qui garantit la comparabilité. » |
| 1:15 | Répondre à LLM.3.2 au **niveau 1**, et à une question avec une preuve | « On choisit la situation réelle décrite, pas une note au hasard. » |
| 2:00 | Onglet Résultats → montrer l'alerte **maillon faible** | « Une faille critique plafonne le domaine : elle ne peut pas être masquée. » |
| 2:45 | Ouvrir l'évaluation **Banque Atlas** (déjà validée) → radar, écarts | « Voici un cas complet : 2,38 sur 5. » |
| 3:30 | Feuille de route → LLM.3.2 en tête (priorité 135) | Expliquer la formule : (4 − 1) × 3 × 0,15 × 100 |
| 4:15 | Questionnaire → GOV.1.2 « déclaré 3, retenu 2 » avec le commentaire de l'auditeur | « L'audit corrige dans les deux sens : voir OPS.2.1. » |
| 5:00 | Télécharger le **rapport PDF** et le feuilleter | « Livrable prêt à présenter à une direction générale. » |
| 5:30 | Swagger (`/swagger-ui.html`) et l'onglet **Actions** de GitHub (CI verte) | « API documentée, tests automatiques à chaque modification. » |

**Plan B :** si le réseau ou Docker pose problème, montrer les captures (`docs/captures/`,
`docs/etude-de-cas/`) et le PDF déjà généré.

## 3. Questions probables et réponses

| Question du jury | Éléments de réponse |
|---|---|
| Pourquoi ces poids (15 %, 10 %, 5 %) ? | Choix d'experts : les domaines transverses et les plus exposés pèsent plus. Ils sont paramétrables dans le JSON, et une version 2 prévoit des profils sectoriels. Limite assumée (doc 09 §6). |
| Une auto-évaluation n'est-elle pas subjective ? | D'où les 5 situations concrètes par question, les preuves attendues, l'indice de confiance et surtout le **mode audit** (déclaré / retenu, commentaire obligatoire). |
| Pourquoi une règle du maillon faible ? | Une moyenne compense : 4 bonnes pratiques masqueraient une faille critique. En sécurité, c'est le maillon le plus faible qui compte. Exemple : TRUST 2,38 → 2,00. |
| En quoi votre outil diffère-t-il d'ISO 42001 ? | ISO 42001 est un système de management certifiable (exigences oui / non). AIMA mesure un **niveau de progression**, couvre aussi la technique (OWASP LLM) et produit un plan d'action. Il prépare à ISO 42001. |
| Comment garantissez-vous la sécurité de la plateforme elle-même ? | JWT + BCrypt, rôles et cloisonnement par organisation (BOLA testé), anti-force brute, en-têtes de sécurité, conteneurs non-root, Dependabot (doc 10 §5). |
| Comment avez-vous testé ? | 35 tests automatiques dont une partie sur un vrai PostgreSQL ; calculs vérifiés à la main ; parcours navigateur automatisé ; CI. Citer les 4 bugs trouvés. |
| Que feriez-vous avec plus de temps ? | Doc 10 §8 : profils sectoriels, comparaison dans le temps, dépôt de preuves, correspondance avec l'AI Act. |
| Pourquoi Java et non Python ? | Écosystème d'entreprise (Spring), typage fort, sécurité mature (Spring Security), standard dans la banque et l'assurance, qui sont les clients cibles. |
