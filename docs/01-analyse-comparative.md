# 01 — Analyse comparative des normes et référentiels (Livrable 1)

## 1. Objectif

Avant de construire notre propre modèle, il faut comprendre ce que chaque norme
apporte, ce qu'elle ne couvre pas, et comment elles se complètent. Notre
référentiel AIMA ne remplace aucune de ces normes : il les **agrège** dans une
grille d'évaluation unique et opérationnelle.

## 2. Fiches d'identité

### 2.1 ISO/IEC 27001:2022 — Système de management de la sécurité de l'information (SMSI)
- **Nature** : norme d'exigences, **certifiable**.
- **Structure** : clauses 4 à 10 (contexte, leadership, planification, support,
  fonctionnement, évaluation, amélioration) + **Annexe A : 93 mesures** en 4 thèmes
  (organisationnelles 37, humaines 8, physiques 14, technologiques 34).
- **Apport pour AIMA** : socle de sécurité « classique » sur lequel repose tout
  système IA (contrôle d'accès, journalisation, gestion des incidents, fournisseurs,
  développement sécurisé).
- **Limite** : ne traite pas des risques spécifiques à l'IA (biais, hallucination,
  injection de prompt, empoisonnement de modèle).

### 2.2 ISO/IEC 42001:2023 — Système de management de l'IA (SMIA / AIMS)
- **Nature** : première norme internationale **certifiable** dédiée au management de l'IA.
- **Structure** : même architecture de clauses que 27001 (structure harmonisée ISO)
  + **Annexe A : 38 mesures** réparties en 9 objectifs (A.2 politiques IA,
  A.3 organisation interne, A.4 ressources, A.5 évaluation des impacts, A.6 cycle de
  vie des systèmes IA, A.7 données, A.8 information des parties intéressées,
  A.9 utilisation des systèmes IA, A.10 relations avec les tiers).
- **Apport pour AIMA** : colonne vertébrale de la **gouvernance IA** et du cycle de vie.
- **Limite** : décrit le « quoi » au niveau management, peu de détails techniques
  sur les attaques contre les modèles.

### 2.3 ISO/IEC 23894:2023 — Lignes directrices pour le management du risque IA
- **Nature** : guide (**non certifiable**), déclinaison IA de l'ISO 31000.
- **Structure** : principes, cadre et processus de management du risque
  (identification, analyse, évaluation, traitement, suivi), annexes sur les
  objectifs et sources de risque IA et sur le cycle de vie.
- **Apport pour AIMA** : méthode d'**appréciation et de traitement des risques IA**.
- **Limite** : pas de liste de contrôles ni de niveaux de maturité.

### 2.4 ISO/IEC 38507:2022 — Gouvernance des SI : implications de l'usage de l'IA
- **Nature** : guide pour l'**organe de gouvernance** (conseil d'administration, direction).
- **Structure** : extension de l'ISO/IEC 38500 ; responsabilité, stratégie,
  acquisition, performance, conformité, comportement humain face à l'IA.
- **Apport pour AIMA** : niveau **stratégique** — la direction est-elle redevable
  de l'IA ? A-t-elle fixé un appétit au risque ?
- **Limite** : très haut niveau, pas opérationnel.

### 2.5 NIST AI RMF 1.0 (NIST AI 100-1, 2023)
- **Nature** : cadre **volontaire** américain, non certifiable, très utilisé.
- **Structure** : 4 fonctions — **GOVERN** (transverse), **MAP** (contexte),
  **MEASURE** (mesure), **MANAGE** (traitement) — déclinées en catégories et
  sous-catégories, plus un *Playbook*. Définit 7 caractéristiques d'une IA digne de
  confiance : valide et fiable ; sûre ; sécurisée et résiliente ; responsable et
  transparente ; explicable et interprétable ; respectueuse de la vie privée ;
  équitable (biais maîtrisés).
- **Apport pour AIMA** : vocabulaire de l'**IA de confiance** et logique de cycle
  Govern → Map → Measure → Manage.
- **Limite** : pas de système de notation de maturité intégré.

### 2.6 NIST AI 600-1 (2024) — Profil IA générative
- **Nature** : profil complémentaire de l'AI RMF pour la **GenAI**.
- **Structure** : **12 risques** propres ou amplifiés par la GenAI (informations
  CBRN, confabulation, contenus dangereux/violents/haineux, vie privée, impacts
  environnementaux, biais et homogénéisation, configuration humain-IA, intégrité de
  l'information, sécurité de l'information, propriété intellectuelle, contenus
  obscènes/abusifs, chaîne de valeur et intégration de composants) et plus de 200
  actions suggérées rattachées aux fonctions de l'AI RMF.
- **Apport pour AIMA** : couverture des risques **LLM / GenAI**.
- **Limite** : volumineux, orienté actions, pas de scoring.

### 2.7 OWASP Top 10 for LLM Applications (édition 2025)
- **Nature** : liste communautaire des **10 vulnérabilités** les plus critiques des
  applications LLM.
- **Contenu** : LLM01 Injection de prompt · LLM02 Divulgation d'informations
  sensibles · LLM03 Chaîne d'approvisionnement · LLM04 Empoisonnement des données et
  du modèle · LLM05 Gestion inappropriée des sorties · LLM06 Agentivité excessive ·
  LLM07 Fuite du prompt système · LLM08 Faiblesses des vecteurs et embeddings ·
  LLM09 Désinformation · LLM10 Consommation non bornée.
- **Apport pour AIMA** : niveau **technique** et concret, directement testable.
- **Limite** : pas de gouvernance, pas de management.

## 3. Tableau comparatif

| Critère | ISO 27001 | ISO 42001 | ISO 23894 | ISO 38507 | NIST AI RMF | NIST AI 600-1 | OWASP LLM |
|---|---|---|---|---|---|---|---|
| Éditeur | ISO/IEC | ISO/IEC | ISO/IEC | ISO/IEC | NIST (US) | NIST (US) | OWASP |
| Année | 2022 | 2023 | 2023 | 2022 | 2023 | 2024 | 2025 |
| Type | Exigences | Exigences | Guide | Guide | Cadre | Profil | Liste de risques |
| Certifiable | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Spécifique IA | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ (GenAI) | ✅ (LLM) |
| Niveau | Management + technique | Management | Risque | Stratégique | Management + risque | Risque GenAI | Technique |
| Public cible | RSSI | Resp. IA / conformité | Risk manager | Direction / CA | Toutes équipes | Équipes GenAI | Développeurs / pentesters |
| Contrôles listés | 93 | 38 | — | — | ~72 sous-catégories | 200+ actions | 10 risques |
| Niveaux de maturité | ❌ | ❌ | ❌ | ❌ | ❌ (tiers informels) | ❌ | ❌ |

**Constat clé** : aucune de ces normes ne fournit de **modèle de maturité chiffré**
couvrant à la fois gouvernance, risque, sécurité classique et sécurité LLM. C'est
précisément le vide que comble AIMA.

## 4. Complémentarité : la pyramide des référentiels

```
          ┌───────────────┐
          │  ISO 38507    │  Stratégique : la direction gouverne l'IA
          ├───────────────┤
          │  ISO 42001    │  Management : un système de management de l'IA
          │  NIST AI RMF  │
          ├───────────────┤
          │  ISO 23894    │  Risque : comment apprécier et traiter les risques IA
          │  NIST AI 600-1│
          ├───────────────┤
          │  ISO 27001    │  Sécurité : socle de mesures de sécurité de l'information
          ├───────────────┤
          │  OWASP LLM    │  Technique : vulnérabilités concrètes des applications LLM
          └───────────────┘
```

## 5. Matrice de correspondance (extrait)

Chaque sous-domaine AIMA est rattaché aux exigences sources. La matrice complète
est stockée dans `referential/referentiel-v1.json` (champ `references`) et sera
affichée dans l'application.

| Thème | ISO 27001 | ISO 42001 | NIST AI RMF | NIST 600-1 | OWASP LLM |
|---|---|---|---|---|---|
| Politique IA | 5.2, A.5.1 | 5.2, A.2.2 | GOVERN 1 | — | — |
| Rôles et responsabilités | 5.3, A.5.2 | 5.3, A.3.2 | GOVERN 2 | — | — |
| Appréciation des risques | 6.1.2 | 6.1.2 | MAP 1-5 | Tous risques | — |
| Analyse d'impact | — | 6.1.4, A.5 | MAP 5 | — | — |
| Données d'entraînement | A.5.12 | A.7 | MAP 2.3 | Data privacy | LLM04 |
| Fournisseurs / modèles tiers | A.5.19-5.22 | A.10 | GOVERN 6 | Value chain | LLM03 |
| Injection de prompt | A.8.28 | A.6.2.4 | MEASURE 2.7 | Information security | LLM01, LLM07 |
| Journalisation | A.8.15-8.16 | A.6.2.8 | MANAGE 4.1 | — | LLM10 |
| Incidents | A.5.24-5.28 | — | MANAGE 4.3 | — | — |
| Supervision humaine | — | A.9 | GOVERN 3.2 | Human-AI config. | LLM06 |
| Biais et équité | — | A.5.4 | MEASURE 2.11 | Harmful bias | — |
| Désinformation | — | A.6.2.4 | MEASURE 2.5 | Confabulation | LLM09 |
| Formation | A.6.3 | 7.2-7.3 | GOVERN 2.2 | — | — |

## 6. Références complémentaires (contexte réglementaire)

- **Règlement européen sur l'IA (AI Act, UE 2024/1689)** : classification des
  systèmes IA par niveau de risque ; utile pour les entreprises marocaines
  travaillant avec l'UE.
- **RGPD** et **Loi marocaine 09-08** (protection des données personnelles, CNDP).
- **MITRE ATLAS** : base de connaissances des tactiques d'attaque contre les systèmes IA.
- **ISO/IEC 42005:2025** : évaluation d'impact des systèmes IA.

## 7. Conclusion

Les normes ISO apportent la **structure de management** et la certifiabilité, le
NIST apporte le **vocabulaire de l'IA de confiance** et la logique de cycle, OWASP
apporte la **précision technique**. AIMA combine ces trois angles dans un modèle de
maturité unique, mesurable sur 5 niveaux.
