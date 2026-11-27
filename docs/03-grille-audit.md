# 03 — Grille d'audit et questionnaire d'évaluation (Livrable 3)

## 1. Vue d'ensemble

| Élément | Valeur |
|---|---|
| Questions | **59** |
| Sous-domaines couverts | 29 (minimum 2 questions chacun) |
| Questions critiques (c = 3) | 9 (15,3 %) |
| Questions importantes (c = 2) | 46 (78,0 %) |
| Questions normales (c = 1) | 4 (6,8 %) |
| Durée estimée d'une auto-évaluation | 2 à 3 heures (en équipe) |

- **Grille complète** : [03b-grille-audit-detaillee.md](03b-grille-audit-detaillee.md), générée automatiquement.
- **Source de vérité** : `referential/questions/*.json`, un fichier par domaine.

## 2. Anatomie d'une question

Chaque question de la grille contient 7 éléments :

| Élément | Rôle | Exemple (LLM.3.2) |
|---|---|---|
| **Code** | Identifiant unique `DOMAINE.SD.N` | `LLM.3.2` |
| **Question** | Formulation fermée, compréhensible par un non-expert | « Les actions sensibles déclenchées par une IA nécessitent-elles une validation humaine ? » |
| **5 descriptions de niveaux** | Le répondant choisit la situation **observée**, pas celle souhaitée | Niveau 3 : « Une liste documentée d'actions sensibles impose une validation humaine. » |
| **Criticité** (1 à 3) | Poids dans le calcul ; déclenche la règle du maillon faible si = 3 | 3 — Critique |
| **Effort** (Faible / Moyen / Élevé) | Sert à placer la recommandation dans la feuille de route | Faible → candidat « quick win » |
| **Preuves attendues** | Ce que l'auditeur demande pour valider la réponse | Journaux d'approbation, captures du flux |
| **4 recommandations** | Action pour passer du niveau *n* au niveau *n + 1* | 1→2 : « Identifier toutes les actions sensibles… » |

> **Principe des niveaux cumulatifs** : un niveau n'est atteint que si les
> conditions des niveaux inférieurs le sont aussi. Une organisation qui mesure
> des indicateurs (niveau 4) sans avoir de procédure écrite (niveau 3) reste au
> niveau 2.

## 3. Règles de réponse

1. **Situation réelle, sur le périmètre évalué**. Si une pratique n'existe que
   sur une partie du périmètre, on retient le niveau le plus représentatif et on
   le précise en commentaire.
2. **« Non applicable » (N/A)** est autorisé uniquement avec une justification
   (ex. « Aucune application LLM déployée » pour le domaine LLM). Une question N/A
   est exclue du calcul. Si tout un sous-domaine est N/A, il est exclu du domaine.
3. **Preuve** : chaque réponse peut être accompagnée d'un document ou d'un lien.
   Une réponse sans preuve est dite **déclarative**.
4. **Commentaire** libre pour donner le contexte.

## 4. Deux modes d'évaluation

| | Auto-évaluation | Audit |
|---|---|---|
| Qui répond | L'organisation elle-même | L'organisation, puis un auditeur |
| Validation | Aucune | L'auditeur **valide** ou **corrige** chaque réponse au vu des preuves |
| Statut des réponses | Déclarative / prouvée | Validée / corrigée (ancien et nouveau niveau conservés) |
| Usage | Diagnostic interne rapide | Évaluation fiable et opposable |

L'application gère ces deux modes grâce aux rôles `ORGANISATION` et `AUDITEUR`.

## 5. Déroulement d'une évaluation

```
1. Cadrage         → Périmètre (entités, systèmes IA), profil sectoriel, niveaux cibles
2. Collecte        → Réponses aux 59 questions + preuves (ateliers avec DSI, RSSI, DPO, métiers, data)
3. Revue           → (mode audit) validation / correction par l'auditeur
4. Calcul          → Scores sous-domaines → domaines → global (règles du document 02)
5. Restitution     → Dashboard radar, écarts, recommandations, feuille de route, rapport PDF
6. Suivi           → Réévaluation à 6 ou 12 mois et comparaison des résultats
```

**Interlocuteurs recommandés par domaine**

| Domaine | Interlocuteurs |
|---|---|
| GOV | Direction générale, responsable IA, juridique |
| RISK | Risk manager, RSSI, DPO |
| DATA | Chief Data Officer, DPO, data engineers |
| DEV | Data scientists, développeurs, équipe MLOps |
| LLM | Équipes produits GenAI, équipe sécurité applicative |
| OPS | DSI, SOC, exploitation |
| TRUST | Responsable IA, métiers, conformité |
| PEOPLE | RH, RSSI, audit interne |

## 6. Exemple de calcul complet (sous-domaine LLM.3)

Réponses : **LLM.3.1** (criticité 2) → niveau **2** ; **LLM.3.2** (criticité 3) → niveau **1**

$$ S_{LLM.3} = \frac{2 \times 2 + 3 \times 1}{2 + 3} = 1{,}4 $$

Supposons que les autres sous-domaines du domaine LLM donnent : LLM.1 = 3,2 ;
LLM.2 = 3,0 ; LLM.4 = 2,8.

$$ S_{LLM}^{brut} = \frac{3{,}2 + 3{,}0 + 1{,}4 + 2{,}8}{4} = 2{,}6 $$

**Maillon faible** : LLM.3.2 est critique et au niveau 1, donc le domaine est
plafonné à **2,0**.

$$ S_{LLM} = \min(2{,}6 ;\ 2{,}0) = 2{,}0 \Rightarrow \text{Niveau 2 — Reproductible (25 \%)} $$

Avec une cible de 3, l'écart vaut 1,0. Recommandation générée pour LLM.3.2 :
« Identifier toutes les actions sensibles qu'une IA peut déclencher ».
Priorité = (3 − 1) × 3 × 0,15 × 100 = **90**, effort faible : c'est un
**quick win (0 à 3 mois)**.

## 7. Maintenance du référentiel

Toute modification du référentiel suit ce cycle :

```
Modifier referential/questions/*.json
  → node scripts/referential-tool.mjs      (valide + régénère la grille 03b)
  → incrémenter "version" dans referentiel-v1.json
  → commit Git
```

Le script refuse un référentiel incohérent : sous-domaine inconnu, code en
double, 5 niveaux ou 4 recommandations manquants, somme des poids différente de 100 %.
Les évaluations déjà réalisées conservent la version du référentiel utilisée, ce
qui garantit qu'elles restent comparables dans le temps.
