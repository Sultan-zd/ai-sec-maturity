# 02 — Modèle de maturité AIMA (Livrable 2)

## 1. Structure générale

Le modèle est organisé en **3 niveaux hiérarchiques** :

```
Référentiel AIMA v1
 └── 8 Domaines                   (ex. D5 — Sécurité des applications LLM)
      └── 29 Sous-domaines        (ex. LLM.1 — Injection de prompt)
           └── Questions / critères (étape 3 : questionnaire)
```

La définition « machine » du modèle se trouve dans
[`referential/referentiel-v1.json`](../referential/referentiel-v1.json) — c'est ce
fichier que la plateforme chargera en base de données.

## 2. Les 5 niveaux de maturité

Inspirés de CMMI et de l'ISO/IEC 33020 (évaluation des processus), adaptés à l'IA.

| Niveau | Nom | Description générique | Indices typiques |
|---|---|---|---|
| **1** | **Initial** | Pratiques absentes ou improvisées ; dépendance aux individus. | Aucune politique ; usage de l'IA non connu de la direction. |
| **2** | **Reproductible** | Pratiques existantes mais informelles, appliquées de manière inégale. | Quelques règles partagées par e-mail ; actions ponctuelles. |
| **3** | **Défini** | Pratiques documentées, approuvées, communiquées et appliquées à l'ensemble du périmètre. | Politique IA validée ; procédures ; rôles nommés. |
| **4** | **Maîtrisé** | Pratiques mesurées par des indicateurs, contrôlées, auditées. | KPI suivis ; audits internes ; tests automatisés. |
| **5** | **Optimisé** | Amélioration continue fondée sur les mesures, veille et innovation. | Retours d'expérience intégrés ; red teaming continu ; benchmark sectoriel. |

## 3. Les 8 domaines et 29 sous-domaines

| Domaine | Sous-domaines | Poids |
|---|---|---|
| **D1 — Gouvernance & stratégie IA** (GOV) | GOV.1 Stratégie et politique IA · GOV.2 Rôles, responsabilités et redevabilité · GOV.3 Inventaire et classification des systèmes IA · GOV.4 Conformité légale et réglementaire | 15 % |
| **D2 — Gestion des risques IA** (RISK) | RISK.1 Méthodologie d'appréciation des risques IA · RISK.2 Analyse d'impact des systèmes IA · RISK.3 Traitement et suivi des risques | 15 % |
| **D3 — Données & vie privée** (DATA) | DATA.1 Qualité, provenance et traçabilité des données · DATA.2 Protection des données personnelles · DATA.3 Sécurité des données d'entraînement | 15 % |
| **D4 — Sécurité du cycle de vie des modèles** (DEV) | DEV.1 Développement sécurisé des systèmes IA · DEV.2 Chaîne d'approvisionnement IA · DEV.3 Tests, validation et red teaming · DEV.4 Déploiement et gestion des changements | 15 % |
| **D5 — Sécurité des applications LLM / GenAI** (LLM) | LLM.1 Injection de prompt et manipulation des entrées · LLM.2 Divulgation d'informations et gestion des sorties · LLM.3 Agentivité et permissions des agents · LLM.4 Sécurité RAG, vecteurs et embeddings | 15 % |
| **D6 — Opérations & résilience** (OPS) | OPS.1 Gestion des identités et des accès · OPS.2 Surveillance et journalisation · OPS.3 Disponibilité et consommation des ressources · OPS.4 Gestion des incidents IA | 10 % |
| **D7 — IA digne de confiance** (TRUST) | TRUST.1 Transparence et explicabilité · TRUST.2 Équité et gestion des biais · TRUST.3 Supervision humaine · TRUST.4 Fiabilité et désinformation | 10 % |
| **D8 — Culture & amélioration continue** (PEOPLE) | PEOPLE.1 Sensibilisation et formation · PEOPLE.2 Usage encadré de l'IA par les collaborateurs · PEOPLE.3 Audit interne et amélioration continue | 5 % |

Les poids sont **paramétrables** par profil sectoriel (ex. une banque augmentera
TRUST pour l'équité du scoring crédit ; une startup GenAI augmentera LLM).

## 4. Méthodologie de scoring

### 4.1 Réponse à une question
Chaque question propose **5 réponses** décrivant concrètement les niveaux 1 à 5.
Le répondant choisit le niveau qui correspond à la réalité, ou **« Non applicable »**
(N/A, avec justification obligatoire). Il peut joindre une **preuve** (document,
lien, capture) et un **commentaire**.

Chaque question a une **criticité** : `1 = normale`, `2 = importante`, `3 = critique`.

### 4.2 Score d'un sous-domaine
Moyenne pondérée par la criticité, en excluant les questions N/A :

$$ S_{sd} = \frac{\sum_q c_q \cdot n_q}{\sum_q c_q} \qquad n_q \in [1;5],\ c_q \in \{1,2,3\} $$

### 4.3 Score d'un domaine
Moyenne pondérée des sous-domaines (poids égaux par défaut) :

$$ S_{d} = \frac{\sum_{sd} w_{sd} \cdot S_{sd}}{\sum_{sd} w_{sd}} $$

### 4.4 Règle du maillon faible (plafonnement)
Une moyenne peut masquer une faille grave. Règle appliquée :

> Si une question **critique** (c = 3) est au **niveau 1**, le score du domaine est
> **plafonné à 2,0**. Si elle est au niveau 2, le domaine est plafonné à 3,0.

Exemple : une entreprise excellente partout mais sans aucune protection contre
l'injection de prompt ne peut pas prétendre à un niveau 4 en sécurité LLM.

### 4.5 Score global
$$ S_{global} = \sum_{d} W_d \cdot S_d \qquad \sum_d W_d = 1 $$

### 4.6 Conversion score → niveau et pourcentage

| Score | Niveau affiché |
|---|---|
| 1,00 – 1,79 | 1 — Initial |
| 1,80 – 2,59 | 2 — Reproductible |
| 2,60 – 3,39 | 3 — Défini |
| 3,40 – 4,19 | 4 — Maîtrisé |
| 4,20 – 5,00 | 5 — Optimisé |

Pourcentage de maturité : $ P = \frac{S - 1}{4} \times 100 $ (1 → 0 %, 5 → 100 %).

### 4.7 Indice de confiance
Chaque réponse peut être marquée **« prouvée »** (preuve jointe) ou **« déclarative »**.
Indice de confiance d'une évaluation = % de réponses prouvées. Un auditeur peut
**valider** ou **corriger** une réponse (mode audit vs auto-évaluation).

## 5. Analyse des écarts (gap analysis)

L'organisation fixe un **niveau cible** par domaine (par défaut 3 — Défini, cible
réaliste à 12-18 mois ; 4 pour les secteurs régulés).

$$ \text{Écart}_d = \max(0,\ \text{Cible}_d - S_d) $$

Visualisation : **graphique radar** (actuel vs cible) + barres d'écarts triées.

## 6. Recommandations et feuille de route

### 6.1 Génération des recommandations
Chaque question possède, pour chaque niveau, une **recommandation** pour passer au
niveau supérieur. Une recommandation est générée pour toute question dont le niveau
est inférieur à la cible de son domaine.

### 6.2 Priorisation
$$ \text{Priorité} = (\text{Cible} - n_q) \times c_q \times W_d \times 100 $$

Chaque recommandation a aussi un **effort estimé** (Faible / Moyen / Élevé).

### 6.3 Horizons de la feuille de route

| Horizon | Règle | Esprit |
|---|---|---|
| **Quick wins (0-3 mois)** | Priorité élevée **et** effort faible | Gains rapides et visibles |
| **Court terme (3-6 mois)** | Questions critiques au niveau 1, ou priorité élevée | Fermer les failles graves |
| **Moyen terme (6-12 mois)** | Priorité moyenne | Structurer et documenter |
| **Long terme (12-18 mois)** | Reste | Mesurer et optimiser |

## 7. Cycle d'une évaluation dans la plateforme

```
Créer l'organisation → Choisir le périmètre et les cibles → Répondre au questionnaire
→ (Validation par un auditeur) → Calcul des scores → Dashboard radar et écarts
→ Recommandations priorisées → Feuille de route → Export PDF → Réévaluation (suivi dans le temps)
```
