# 03b — Grille d'audit détaillée AIMA v1.0.0

> Fichier **généré automatiquement** par `scripts/referential-tool.mjs` à partir de `referential/`. Ne pas modifier à la main.

**59 questions** · 29 sous-domaines · 8 domaines

## Sommaire

- [D1 — Gouvernance & stratégie IA](#d1) (poids 15 %)
- [D2 — Gestion des risques IA](#d2) (poids 15 %)
- [D3 — Données & vie privée](#d3) (poids 15 %)
- [D4 — Sécurité du cycle de vie des modèles](#d4) (poids 15 %)
- [D5 — Sécurité des applications LLM / GenAI](#d5) (poids 15 %)
- [D6 — Opérations & résilience](#d6) (poids 10 %)
- [D7 — IA digne de confiance](#d7) (poids 10 %)
- [D8 — Culture & amélioration continue](#d8) (poids 5 %)

---

<a id="d1"></a>
## D1 — Gouvernance & stratégie IA (GOV)

Engagement de la direction, politique IA, organisation, inventaire et conformité.

### GOV.1 — Stratégie et politique IA

Existence d'une stratégie IA alignée sur les objectifs métier et d'une politique IA approuvée par la direction.

*Références : ISO42001 5.2, A.2.2, A.2.3 · ISO38507 4, 6.2 · NIST_AI_RMF GOVERN 1.1, GOVERN 1.2 · ISO27001 5.2, A.5.1*

#### GOV.1.1 — La direction a-t-elle défini une stratégie IA alignée sur les objectifs de l'entreprise ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Document de stratégie IA, compte rendu de comité de direction, feuille de route IA.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune stratégie IA ; les projets naissent au gré des initiatives individuelles. | Organiser un atelier de direction pour identifier les usages IA actuels et souhaités et formuler des orientations. |
| 2 — Reproductible | Des orientations IA existent oralement ou dans des présentations ponctuelles, sans validation formelle. | Formaliser une stratégie IA écrite (objectifs, cas d'usage, appétit au risque) et la faire valider par la direction. |
| 3 — Défini | Une stratégie IA écrite, validée par la direction, fixe les objectifs, les cas d'usage prioritaires et l'appétit au risque. | Définir des indicateurs (valeur créée, incidents, conformité) et les suivre en comité de direction. |
| 4 — Maîtrisé | La stratégie est déclinée en objectifs mesurables suivis par des indicateurs revus en comité de direction. | Instaurer une revue annuelle de la stratégie intégrant la veille réglementaire et les retours d'expérience. |
| 5 — Optimisé | La stratégie est revue au moins annuellement à partir des résultats, de la veille technologique et réglementaire. | — |

#### GOV.1.2 — Existe-t-il une politique IA (usage acceptable, principes éthiques, exigences de sécurité) approuvée et diffusée ?

Criticité : **Critique** · Effort de mise en œuvre : Faible  
Preuves attendues : Politique IA signée, preuve de diffusion (intranet, e-mail), attestations de lecture.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune politique IA. | Rédiger une première politique IA en s'appuyant sur l'Annexe A.2 de l'ISO/IEC 42001. |
| 2 — Reproductible | Des règles éparses (e-mails, notes) sans document de référence. | Consolider les règles existantes dans une politique unique, la faire approuver et la diffuser. |
| 3 — Défini | Une politique IA approuvée par la direction est publiée et communiquée à tous les collaborateurs. | Mettre en place des attestations de lecture et un registre des dérogations. |
| 4 — Maîtrisé | L'application de la politique est contrôlée (attestations de lecture, contrôles de conformité, dérogations tracées). | Instaurer un cycle de révision annuel de la politique alimenté par les audits et les incidents. |
| 5 — Optimisé | La politique est révisée périodiquement en fonction des incidents, des audits et des évolutions réglementaires. | — |

### GOV.2 — Rôles, responsabilités et redevabilité

Rôles IA définis (responsable IA, comité), redevabilité de la direction, séparation des tâches.

*Références : ISO42001 5.3, A.3.2 · ISO38507 5 · NIST_AI_RMF GOVERN 2.1, GOVERN 2.3 · ISO27001 5.3, A.5.2*

#### GOV.2.1 — Les rôles et responsabilités liés à l'IA sont-ils formellement attribués ?

Criticité : **Critique** · Effort de mise en œuvre : Faible  
Preuves attendues : Note de nomination du responsable IA, matrice RACI, fiches de poste.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Personne n'est identifié comme responsable des systèmes IA. | Désigner un responsable IA et un propriétaire pour chaque système IA. |
| 2 — Reproductible | Des référents informels existent selon les projets. | Formaliser une matrice RACI couvrant conception, déploiement, risques et conformité. |
| 3 — Défini | Un responsable IA et un propriétaire par système IA sont nommés ; une matrice RACI est documentée. | Intégrer ces responsabilités aux fiches de poste et vérifier leur exercice lors des audits. |
| 4 — Maîtrisé | Les responsabilités sont intégrées aux fiches de poste et aux objectifs ; leur exercice est vérifié en audit. | Réévaluer annuellement l'organisation à partir des indicateurs et des retours d'expérience. |
| 5 — Optimisé | L'organisation des rôles est ajustée en continu selon la charge, les incidents et les nouveaux usages. | — |

#### GOV.2.2 — Un comité de gouvernance IA supervise-t-il les décisions et les risques liés à l'IA ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Charte du comité IA, ordres du jour, comptes rendus de réunion.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune instance ; les décisions IA sont prises projet par projet. | Identifier les parties prenantes clés et organiser une première revue des projets IA. |
| 2 — Reproductible | Des réunions ponctuelles ont lieu sur certains projets IA importants. | Créer un comité IA pluridisciplinaire doté d'une charte et d'un calendrier de réunions. |
| 3 — Défini | Un comité IA pluridisciplinaire (métier, sécurité, juridique, données) se réunit régulièrement avec un mandat écrit. | Soumettre tout nouveau système IA à risque à la validation du comité et établir un reporting à la direction. |
| 4 — Maîtrisé | Le comité valide chaque nouveau système IA à risque, suit des indicateurs et rend compte à la direction générale. | Évaluer annuellement l'efficacité du comité et ajuster son mandat. |
| 5 — Optimisé | Le comité fait évoluer le dispositif à partir des tendances, des audits et des comparaisons sectorielles. | — |

#### GOV.2.3 — La direction générale (ou le conseil d'administration) reçoit-elle un reporting régulier sur les risques et la performance de l'IA ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Tableaux de bord présentés à la direction, procès-verbaux du conseil.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun reporting. | Présenter à la direction un premier état des lieux des usages et des risques IA. |
| 2 — Reproductible | Information ponctuelle en cas d'incident ou de projet majeur. | Instaurer un reporting semestriel formalisé vers la direction. |
| 3 — Défini | Un reporting périodique formalisé (au moins semestriel) couvre les usages, risques et incidents IA. | Construire un tableau de bord avec indicateurs et seuils d'alerte. |
| 4 — Maîtrisé | Un tableau de bord avec indicateurs clés et seuils d'alerte est discuté et donne lieu à des décisions tracées. | Relier le reporting aux décisions d'appétit au risque et d'investissement. |
| 5 — Optimisé | La direction ajuste l'appétit au risque et les investissements IA sur la base de ce reporting. | — |

### GOV.3 — Inventaire et classification des systèmes IA

Registre à jour des systèmes IA (internes, achetés, SaaS) classés par niveau de risque.

*Références : ISO42001 A.4.2, A.6.2.2 · NIST_AI_RMF GOVERN 1.6, MAP 1.1 · ISO27001 A.5.9*

#### GOV.3.1 — L'entreprise dispose-t-elle d'un inventaire de ses systèmes IA (développés en interne, achetés, SaaS) ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Registre des systèmes IA avec propriétaire, finalité, données traitées et fournisseur.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun inventaire ; la direction ne connaît pas les usages IA. | Lancer un recensement des usages IA auprès des directions métiers et de la DSI. |
| 2 — Reproductible | Une liste partielle existe, limitée aux projets IA internes. | Étendre l'inventaire aux outils achetés et SaaS et attribuer un propriétaire à chaque système. |
| 3 — Défini | Un registre couvre tous les systèmes IA (internes, fournisseurs, SaaS) avec propriétaire, finalité et données traitées. | Rendre la mise à jour obligatoire à chaque changement et rapprocher le registre des achats et flux réseau. |
| 4 — Maîtrisé | Le registre est mis à jour à chaque changement et rapproché périodiquement des outils réellement utilisés. | Intégrer le registre à l'outil GRC / CMDB et l'automatiser avec l'analyse des risques. |
| 5 — Optimisé | Le registre est intégré à l'outil GRC / CMDB et alimente automatiquement l'analyse des risques. | — |

#### GOV.3.2 — Les systèmes IA sont-ils classés selon leur niveau de risque (par exemple selon les catégories de l'AI Act) ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Grille de classification, colonne « niveau de risque » dans le registre.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune classification. | Définir des critères simples de sensibilité (données personnelles, décisions sur des personnes, exposition externe). |
| 2 — Reproductible | Une distinction intuitive « sensible / non sensible » est faite au cas par cas. | Adopter une grille documentée inspirée de l'AI Act et l'appliquer à l'inventaire. |
| 3 — Défini | Une grille documentée (inacceptable, élevé, limité, minimal) est appliquée à chaque système. | Associer à chaque classe de risque un socle d'exigences obligatoires. |
| 4 — Maîtrisé | La classification détermine automatiquement les exigences applicables (analyse d'impact, tests, validation). | Mettre en place une veille réglementaire et réviser la grille chaque année. |
| 5 — Optimisé | La grille est revue selon l'évolution réglementaire et les retours d'expérience. | — |

### GOV.4 — Conformité légale et réglementaire

Identification et respect des obligations légales applicables (AI Act, RGPD, loi 09-08, réglementations sectorielles).

*Références : ISO42001 4.1, A.2.4 · NIST_AI_RMF GOVERN 1.1 · ISO27001 A.5.31, A.5.34*

#### GOV.4.1 — Les obligations légales et réglementaires applicables à l'IA sont-elles identifiées et suivies ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Registre des obligations, avis juridiques, rapports de contrôle de conformité.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les obligations ne sont pas identifiées. | Faire réaliser par le service juridique un état des lieux des textes applicables aux usages IA. |
| 2 — Reproductible | Le service juridique est consulté ponctuellement sur certains projets. | Constituer un registre des obligations rattaché aux systèmes IA. |
| 3 — Défini | Un registre des obligations (AI Act, RGPD, loi 09-08, réglementations sectorielles) est tenu et rattaché aux systèmes. | Planifier des contrôles de conformité périodiques et suivre les écarts. |
| 4 — Maîtrisé | La conformité est contrôlée périodiquement et les écarts sont suivis dans un plan d'action. | Mettre en place une veille réglementaire formelle avec analyse d'impact des nouveaux textes. |
| 5 — Optimisé | Une veille réglementaire structurée anticipe les nouvelles obligations et adapte le dispositif. | — |

#### GOV.4.2 — Les questions de propriété intellectuelle et de responsabilité liées à l'IA (contenus générés, données d'entraînement, contrats) sont-elles traitées ?

Criticité : Normale · Effort de mise en œuvre : Moyen  
Preuves attendues : Clauses contractuelles types IA, analyses juridiques sur les droits des données.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Ces questions ne sont pas traitées. | Identifier les risques de propriété intellectuelle et de responsabilité liés aux contenus générés. |
| 2 — Reproductible | Elles sont traitées au cas par cas lors de litiges ou de demandes. | Rédiger des clauses types IA avec le service juridique. |
| 3 — Défini | Des clauses types IA (responsabilité, propriété intellectuelle, usage des données) sont intégrées aux contrats. | Réviser les contrats en cours et contrôler l'application des clauses. |
| 4 — Maîtrisé | Les contrats existants sont revus et le respect des clauses est contrôlé. | Suivre la jurisprudence et mettre à jour les clauses types. |
| 5 — Optimisé | Les clauses évoluent en fonction de la jurisprudence et des retours d'expérience. | — |

---

<a id="d2"></a>
## D2 — Gestion des risques IA (RISK)

Appréciation, analyse d'impact, traitement et suivi des risques propres à l'IA.

### RISK.1 — Méthodologie d'appréciation des risques IA

Méthode formalisée d'identification, d'analyse et d'évaluation des risques IA intégrée à la gestion des risques de l'entreprise.

*Références : ISO23894 6.4 · ISO42001 6.1.2, 8.2 · NIST_AI_RMF MAP 1, MAP 4, MEASURE 1 · NIST_600_1 GAI risks 2.1-2.12*

#### RISK.1.1 — Une méthode d'appréciation des risques spécifiques à l'IA est-elle définie et appliquée ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Méthodologie documentée, analyses de risques par système IA, cartographie des risques.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les risques IA ne sont pas évalués. | Réaliser une première analyse des risques des systèmes IA les plus critiques. |
| 2 — Reproductible | Des analyses ponctuelles sont réalisées sans méthode commune. | Formaliser une méthode d'appréciation des risques IA basée sur ISO/IEC 23894. |
| 3 — Défini | Une méthode documentée (inspirée d'ISO/IEC 23894) couvre biais, robustesse, sécurité et vie privée, et est appliquée à chaque système. | Intégrer les risques IA à la cartographie globale des risques et planifier des revues. |
| 4 — Maîtrisé | Les résultats sont consolidés dans la cartographie des risques de l'entreprise et revus périodiquement. | Exploiter les bases d'incidents (AI Incident Database, MITRE ATLAS) pour enrichir la méthode. |
| 5 — Optimisé | La méthode s'appuie sur les incidents internes et externes et est améliorée en continu. | — |

#### RISK.1.2 — Les menaces adversariales propres à l'IA (empoisonnement, évasion, extraction de modèle, injection) sont-elles prises en compte ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Modélisation des menaces (MITRE ATLAS, STRIDE), scénarios d'attaque documentés.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Ces menaces sont inconnues de l'organisation. | Sensibiliser les équipes aux menaces adversariales (MITRE ATLAS, OWASP Top 10 LLM). |
| 2 — Reproductible | Elles sont connues de quelques experts mais ne sont pas analysées. | Réaliser une modélisation des menaces pour chaque système IA exposé. |
| 3 — Défini | Une modélisation des menaces est réalisée pour chaque système IA exposé. | Transformer les scénarios de menace en cas de test et vérifier les protections. |
| 4 — Maîtrisé | Les scénarios de menace sont testés et les résultats alimentent les mesures de protection. | S'abonner à des sources de renseignement sur les menaces IA et mettre à jour les modèles de menace. |
| 5 — Optimisé | La modélisation est mise à jour en continu grâce à la veille et au renseignement sur les menaces. | — |

### RISK.2 — Analyse d'impact des systèmes IA

Évaluation des impacts sur les personnes, les groupes et la société avant déploiement.

*Références : ISO42001 6.1.4, A.5.2, A.5.4, A.5.5 · NIST_AI_RMF MAP 5.1, MAP 5.2*

#### RISK.2.1 — Une analyse d'impact sur les personnes, les groupes et la société est-elle réalisée avant le déploiement d'un système IA ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Analyses d'impact signées, procédure de mise en production faisant référence à l'analyse.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune analyse d'impact. | Réaliser une analyse d'impact pilote sur le système IA le plus sensible. |
| 2 — Reproductible | Des analyses sont réalisées uniquement à la demande d'un client ou d'un régulateur. | Rendre l'analyse d'impact obligatoire pour les systèmes à risque élevé (modèle ISO/IEC 42005). |
| 3 — Défini | Une analyse d'impact documentée est obligatoire avant la mise en production de tout système IA à risque élevé. | Faire revoir les analyses par une fonction indépendante et les actualiser à chaque changement. |
| 4 — Maîtrisé | Les analyses sont revues par une fonction indépendante et réactualisées à chaque changement significatif. | Mesurer les impacts réels après déploiement et ajuster la méthode. |
| 5 — Optimisé | Les impacts réels sont mesurés après déploiement et comparés aux prévisions pour améliorer la méthode. | — |

#### RISK.2.2 — L'analyse d'impact relative à la protection des données (AIPD) est-elle articulée avec l'analyse d'impact IA ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : AIPD intégrant les spécificités IA, avis du DPO.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune AIPD pour les traitements IA. | Identifier les traitements IA utilisant des données personnelles et impliquer le DPO. |
| 2 — Reproductible | Des AIPD sont faites séparément sans prendre en compte les spécificités de l'IA. | Intégrer les spécificités IA (inférence, réidentification, biais) au modèle d'AIPD. |
| 3 — Défini | Une démarche commune intègre l'AIPD et les impacts IA pour les traitements de données personnelles. | Rendre obligatoire la validation du DPO et suivre les mesures décidées. |
| 4 — Maîtrisé | Le DPO valide systématiquement les traitements IA et suit les mesures décidées. | Outiller la démarche et capitaliser sur les retours des autorités. |
| 5 — Optimisé | La démarche est outillée, réutilisable et enrichie par les retours des autorités (CNDP, CNIL). | — |

### RISK.3 — Traitement et suivi des risques

Plans de traitement, propriétaires de risques, risques résiduels acceptés et revus périodiquement.

*Références : ISO23894 6.5, 6.6 · ISO42001 6.1.3, 8.3 · NIST_AI_RMF MANAGE 1, MANAGE 2*

#### RISK.3.1 — Chaque risque IA identifié dispose-t-il d'un plan de traitement, d'un propriétaire et d'une échéance ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Plans de traitement des risques, registre d'acceptation des risques résiduels.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les risques identifiés ne sont pas traités. | Attribuer un propriétaire à chaque risque IA majeur. |
| 2 — Reproductible | Certaines mesures sont prises sans suivi formel. | Formaliser des plans de traitement avec échéances et acceptation du risque résiduel. |
| 3 — Défini | Un plan documente pour chaque risque la mesure, le propriétaire, l'échéance et l'acceptation formelle du risque résiduel. | Suivre l'avancement des plans et vérifier l'efficacité des mesures. |
| 4 — Maîtrisé | L'avancement des plans est suivi par indicateurs et l'efficacité des mesures est vérifiée. | Prioriser les mesures selon une analyse coût/bénéfice. |
| 5 — Optimisé | Le traitement des risques est optimisé par analyse coût/bénéfice et retours d'expérience. | — |

#### RISK.3.2 — Les risques IA sont-ils réévalués périodiquement et à chaque changement (nouveau modèle, nouvelles données, nouvel usage) ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Calendrier de revue, historique des réévaluations.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les risques ne sont jamais réévalués. | Planifier une revue annuelle des risques IA. |
| 2 — Reproductible | Ils sont réévalués uniquement après un incident. | Définir les changements significatifs qui imposent une réévaluation. |
| 3 — Défini | Une revue est planifiée au moins une fois par an et à chaque changement significatif. | Mettre en place des déclencheurs automatiques liés à la surveillance. |
| 4 — Maîtrisé | Des déclencheurs automatiques (dérive, incident, nouvelle version) lancent une réévaluation. | Évoluer vers une évaluation continue des risques. |
| 5 — Optimisé | La réévaluation est continue et s'appuie sur la surveillance en temps réel. | — |

---

<a id="d3"></a>
## D3 — Données & vie privée (DATA)

Qualité, provenance, protection et sécurité des données utilisées par les systèmes IA.

### DATA.1 — Qualité, provenance et traçabilité des données

Documentation de l'origine, de la qualité et du lignage des données d'entraînement et d'inférence.

*Références : ISO42001 A.7.2, A.7.3, A.7.4, A.7.5 · NIST_AI_RMF MAP 2.3, MEASURE 2.1*

#### DATA.1.1 — L'origine, les droits d'utilisation et le lignage des données utilisées par les systèmes IA sont-ils documentés ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Fiches descriptives des jeux de données (datasheets), outil de lignage, catalogue de données.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune documentation des données. | Recenser les jeux de données utilisés par les systèmes IA. |
| 2 — Reproductible | Documentation partielle selon les projets. | Rendre obligatoire une fiche descriptive (datasheet) pour chaque jeu de données. |
| 3 — Défini | Chaque jeu de données dispose d'une fiche (source, licence, date, finalité, transformations). | Outiller le traçage automatique du lignage (DVC, MLflow, OpenLineage). |
| 4 — Maîtrisé | Le lignage est tracé automatiquement de la source jusqu'au modèle déployé. | Intégrer ces fiches au catalogue de données de l'entreprise. |
| 5 — Optimisé | La gouvernance des données IA est intégrée au catalogue de données de l'entreprise et améliorée en continu. | — |

#### DATA.1.2 — La qualité des données (exactitude, complétude, représentativité) est-elle évaluée avant l'entraînement ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Critères de qualité documentés, rapports de contrôle qualité des données.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune évaluation de la qualité. | Définir les critères de qualité essentiels pour les données d'entraînement. |
| 2 — Reproductible | Contrôles manuels ponctuels. | Documenter et appliquer ces critères à chaque jeu de données. |
| 3 — Défini | Des critères de qualité documentés (ISO/IEC 5259) sont vérifiés pour chaque jeu de données. | Automatiser les contrôles qualité dans le pipeline (ex. Great Expectations). |
| 4 — Maîtrisé | Des contrôles automatisés avec seuils bloquent les données non conformes dans le pipeline. | Suivre les tendances de qualité et corriger les problèmes à la source. |
| 5 — Optimisé | Les indicateurs de qualité sont suivis dans le temps et utilisés pour améliorer la collecte. | — |

### DATA.2 — Protection des données personnelles

Minimisation, base légale, anonymisation et droits des personnes appliqués aux traitements IA.

*Références : ISO27001 A.5.34, A.8.11 · ISO42001 A.7.6 · NIST_AI_RMF MEASURE 2.10 · NIST_600_1 Data Privacy · OWASP_LLM LLM02*

#### DATA.2.1 — Les principes de protection des données personnelles (base légale, minimisation, durée de conservation) sont-ils appliqués aux traitements IA ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Registre des traitements, déclarations / autorisations CNDP, mentions d'information.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Ces principes ne sont pas appliqués. | Recenser avec le DPO les données personnelles utilisées par les systèmes IA. |
| 2 — Reproductible | Ils sont appliqués partiellement, sans vérification. | Inscrire les traitements au registre et effectuer les formalités CNDP / RGPD. |
| 3 — Défini | Les traitements IA figurent au registre avec base légale, minimisation et durées définies ; les formalités CNDP / RGPD sont effectuées. | Contrôler régulièrement la conformité et l'exercice des droits des personnes. |
| 4 — Maîtrisé | Des contrôles réguliers vérifient la conformité et l'effectivité des droits des personnes. | Étudier les techniques de protection avancées (données synthétiques, confidentialité différentielle). |
| 5 — Optimisé | Des techniques avancées (données synthétiques, confidentialité différentielle, apprentissage fédéré) réduisent l'usage de données personnelles. | — |

#### DATA.2.2 — Des mesures empêchent-elles l'envoi de données sensibles vers des services IA externes non autorisés ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Règle écrite, liste des outils IA autorisés, configuration DLP / CASB / proxy.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune mesure. | Informer les collaborateurs des données à ne jamais soumettre à une IA externe. |
| 2 — Reproductible | Des consignes orales ou des e-mails de sensibilisation. | Formaliser la règle et mettre à disposition des outils IA approuvés. |
| 3 — Défini | Une règle écrite définit les données interdites et des outils IA approuvés sont proposés. | Déployer des contrôles DLP / CASB sur les services IA. |
| 4 — Maîtrisé | Des contrôles techniques (DLP, proxy, CASB) détectent ou bloquent les envois de données sensibles. | Analyser les alertes et affiner les règles en continu. |
| 5 — Optimisé | Les contrôles sont ajustés en continu selon les détections et les nouveaux usages. | — |

### DATA.3 — Sécurité des données d'entraînement

Protection contre l'empoisonnement des données et du modèle, contrôle d'intégrité des jeux de données.

*Références : ISO27001 A.8.24 · NIST_AI_RMF MEASURE 2.7 · NIST_600_1 Information Security · OWASP_LLM LLM04*

#### DATA.3.1 — L'intégrité des données d'entraînement est-elle protégée contre l'empoisonnement ?

Criticité : **Critique** · Effort de mise en œuvre : Élevé  
Preuves attendues : Contrôles d'accès aux jeux de données, empreintes (hash), rapports de détection d'anomalies.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune protection. | Restreindre et tracer l'accès aux données d'entraînement. |
| 2 — Reproductible | Accès aux données restreint de manière informelle. | Versionner les jeux de données et contrôler leur intégrité par empreintes. |
| 3 — Défini | Les jeux de données sont stockés avec contrôle d'accès, versionnés et leur intégrité est vérifiée par empreintes. | Ajouter une détection d'anomalies dans le pipeline d'entraînement. |
| 4 — Maîtrisé | Une détection d'anomalies et de données aberrantes est appliquée avant chaque entraînement. | Conduire des tests d'empoisonnement et adopter des techniques d'entraînement robuste. |
| 5 — Optimisé | Des techniques d'entraînement robuste et des tests d'empoisonnement réguliers sont mis en œuvre. | — |

#### DATA.3.2 — Les données d'entraînement et les modèles sont-ils chiffrés et sauvegardés ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Configuration du chiffrement, politique de sauvegarde, procès-verbaux de tests de restauration.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Ni chiffrés ni sauvegardés. | Mettre en place des sauvegardes régulières des données et des modèles. |
| 2 — Reproductible | Sauvegardes ponctuelles, sans chiffrement systématique. | Activer le chiffrement au repos et en transit. |
| 3 — Défini | Chiffrement au repos et en transit ; sauvegardes régulières documentées. | Tester les restaurations et centraliser la gestion des clés. |
| 4 — Maîtrisé | Les restaurations sont testées périodiquement ; la gestion des clés est centralisée. | Adopter des sauvegardes immuables et la rotation automatique des clés. |
| 5 — Optimisé | Stratégie optimisée : sauvegardes immuables et hors ligne, rotation automatique des clés. | — |

---

<a id="d4"></a>
## D4 — Sécurité du cycle de vie des modèles (DEV)

Conception, développement, approvisionnement, tests et déploiement sécurisés des systèmes IA (MLSecOps).

### DEV.1 — Développement sécurisé des systèmes IA

Exigences de sécurité dès la conception, revue de code, gestion des secrets, pipelines MLOps sécurisés.

*Références : ISO27001 A.8.25, A.8.26, A.8.27, A.8.28 · ISO42001 A.6.1.2, A.6.2.3 · NIST_AI_RMF MAP 3*

#### DEV.1.1 — Des exigences de sécurité sont-elles intégrées dès la conception des systèmes IA (security by design) ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Catalogue d'exigences de sécurité IA, comptes rendus de revue de conception, configuration CI/CD.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune exigence de sécurité. | Impliquer l'équipe sécurité dès le lancement de chaque projet IA. |
| 2 — Reproductible | La sécurité est traitée en fin de projet. | Rédiger un catalogue d'exigences de sécurité IA appliqué dès la conception. |
| 3 — Défini | Un catalogue d'exigences de sécurité IA est appliqué dès la conception et validé par l'équipe sécurité. | Intégrer des contrôles de sécurité automatiques dans la CI/CD. |
| 4 — Maîtrisé | Les exigences sont vérifiées automatiquement dans la CI/CD (SAST, scan des dépendances, scan des modèles). | Créer un réseau de champions sécurité et mettre à jour les exigences en continu. |
| 5 — Optimisé | Les exigences évoluent avec les incidents et les menaces ; des champions sécurité sont présents dans chaque équipe. | — |

#### DEV.1.2 — Les secrets (clés API des fournisseurs IA, identifiants) sont-ils gérés de manière sécurisée ?

Criticité : **Critique** · Effort de mise en œuvre : Faible  
Preuves attendues : Coffre-fort de secrets, rapport de scan de secrets (gitleaks), procédure de rotation.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Des secrets sont en clair dans le code ou les notebooks. | Retirer les secrets du code et révoquer immédiatement les clés exposées. |
| 2 — Reproductible | Les secrets sont dans des fichiers de configuration non versionnés. | Adopter un coffre-fort de secrets et un scanner de secrets dans les dépôts. |
| 3 — Défini | Un coffre-fort de secrets est utilisé et le code est scanné pour détecter les secrets. | Mettre en place la rotation des clés et le moindre privilège par clé. |
| 4 — Maîtrisé | Rotation régulière, droits minimaux par clé et alertes sur usage anormal. | Passer à des identifiants éphémères avec rotation automatique. |
| 5 — Optimisé | Secrets éphémères générés à la demande avec rotation automatique. | — |

### DEV.2 — Chaîne d'approvisionnement IA

Évaluation des fournisseurs IA, des modèles pré-entraînés, des bibliothèques et des API tierces (SBOM / AI-BOM).

*Références : ISO27001 A.5.19, A.5.20, A.5.21, A.5.22 · ISO42001 A.10.2, A.10.3 · NIST_AI_RMF GOVERN 6.1, GOVERN 6.2 · NIST_600_1 Value Chain and Component Integration · OWASP_LLM LLM03*

#### DEV.2.1 — Les fournisseurs et services IA tiers (API de LLM, SaaS IA) sont-ils évalués avant utilisation ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Questionnaires fournisseurs, clauses contractuelles (non-réutilisation des données, localisation), certifications.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune évaluation. | Recenser les fournisseurs IA utilisés. |
| 2 — Reproductible | Évaluation informelle selon le coût et les fonctionnalités. | Mettre en place un questionnaire d'évaluation sécurité/IA obligatoire avant contrat. |
| 3 — Défini | Un questionnaire sécurité/IA est obligatoire avant contrat (localisation des données, réutilisation pour l'entraînement, certifications). | Réévaluer périodiquement les fournisseurs critiques et contrôler leurs engagements. |
| 4 — Maîtrisé | Les fournisseurs critiques sont réévalués périodiquement et leurs engagements contractuels sont contrôlés. | Mettre en place une surveillance continue des tiers IA. |
| 5 — Optimisé | Évaluation continue (notation automatisée, veille sur les incidents) intégrée à la gestion des tiers. | — |

#### DEV.2.2 — Les modèles pré-entraînés et bibliothèques open source sont-ils vérifiés (origine, intégrité, vulnérabilités) ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Liste des sources autorisées, AI-BOM / SBOM, rapports de scan des modèles.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les modèles sont téléchargés et utilisés sans vérification. | Lister les modèles et bibliothèques IA utilisés. |
| 2 — Reproductible | Vérification manuelle occasionnelle de la réputation. | Définir les sources autorisées, imposer des formats sûrs et produire un AI-BOM. |
| 3 — Défini | Sources autorisées définies, formats sûrs imposés (ex. safetensors), inventaire des composants (AI-BOM). | Automatiser le scan des modèles et la vérification des signatures. |
| 4 — Maîtrisé | Scans automatiques des modèles et dépendances et vérification des signatures dans la CI/CD. | Mettre en place un registre interne de modèles approuvés. |
| 5 — Optimisé | Registre interne de modèles approuvés et surveillance continue des vulnérabilités. | — |

### DEV.3 — Tests, validation et red teaming

Tests de performance, robustesse, sécurité adversariale et red teaming avant mise en production.

*Références : ISO42001 A.6.2.4 · NIST_AI_RMF MEASURE 2.3, MEASURE 2.7 · NIST_600_1 MS-2.7 · ISO27001 A.8.29*

#### DEV.3.1 — Les modèles sont-ils testés (performance, robustesse, biais, sécurité) avant leur mise en production ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Plan de tests, critères d'acceptation, rapports de tests archivés.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun test formalisé. | Définir des critères d'acceptation minimaux pour chaque modèle. |
| 2 — Reproductible | Tests de performance uniquement. | Formaliser un plan de tests (performance, robustesse, biais, sécurité). |
| 3 — Défini | Un plan de tests documenté couvre performance, robustesse, biais et sécurité, avec des critères d'acceptation. | Automatiser les tests dans le pipeline et archiver les résultats. |
| 4 — Maîtrisé | Les tests sont automatisés à chaque version ; les résultats sont archivés et revus. | Enrichir les tests à partir des incidents et de la veille sur les menaces. |
| 5 — Optimisé | La couverture des tests est étendue en continu à partir des incidents et des nouvelles menaces. | — |

#### DEV.3.2 — Des exercices de red teaming ou des tests d'intrusion spécifiques à l'IA sont-ils réalisés ?

Criticité : Importante · Effort de mise en œuvre : Élevé  
Preuves attendues : Rapports de red teaming, méthodologie utilisée, plan de remédiation.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Jamais. | Réaliser un premier test adversarial sur le système IA le plus exposé. |
| 2 — Reproductible | Tests ponctuels réalisés par l'équipe projet. | Rendre le red teaming obligatoire avant production selon une méthodologie reconnue. |
| 3 — Défini | Un red teaming est réalisé avant la mise en production de tout système exposé, selon une méthodologie reconnue (OWASP, MITRE ATLAS). | Faire intervenir une équipe indépendante et outiller les tests. |
| 4 — Maîtrisé | Red teaming périodique par une équipe indépendante, avec outils automatisés (garak, PyRIT). | Étendre au red teaming continu et à un programme de bug bounty. |
| 5 — Optimisé | Red teaming continu et programme de bug bounty incluant les systèmes IA. | — |

### DEV.4 — Déploiement et gestion des changements

Validation formelle avant mise en production, versionnage des modèles, retour arrière possible.

*Références : ISO42001 A.6.2.5, A.6.2.6 · ISO27001 A.8.32 · NIST_AI_RMF MANAGE 2.4*

#### DEV.4.1 — Le déploiement des modèles suit-il un processus de validation et de gestion des changements ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Procédure de mise en production, tickets de changement, historique des déploiements.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Déploiement manuel sans validation. | Formaliser une checklist de mise en production des modèles. |
| 2 — Reproductible | Validation informelle par l'équipe projet. | Mettre en place une validation formelle métier et sécurité. |
| 3 — Défini | Processus de mise en production documenté avec validation formelle métier et sécurité. | Automatiser le déploiement avec des contrôles bloquants. |
| 4 — Maîtrisé | Déploiement automatisé (MLOps) avec contrôles bloquants et traçabilité complète. | Adopter les déploiements progressifs avec retour arrière automatique. |
| 5 — Optimisé | Déploiements progressifs (canary, shadow) avec retour arrière automatique. | — |

#### DEV.4.2 — Les modèles sont-ils versionnés avec leur documentation (model card), avec possibilité de revenir à une version antérieure ?

Criticité : Normale · Effort de mise en œuvre : Faible  
Preuves attendues : Registre de modèles, model cards, test de retour arrière.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun versionnage. | Versionner systématiquement les modèles. |
| 2 — Reproductible | Versionnage manuel des fichiers. | Mettre en place un registre de modèles (ex. MLflow) avec model cards. |
| 3 — Défini | Un registre de modèles conserve versions, métadonnées et model cards. | Relier modèles, données, code et tests ; tester le retour arrière. |
| 4 — Maîtrisé | Lien complet modèle ↔ données ↔ code ↔ résultats de tests ; retour arrière testé. | Formaliser la procédure de retrait des modèles obsolètes. |
| 5 — Optimisé | Gouvernance complète du cycle de vie, y compris le retrait des modèles obsolètes. | — |

---

<a id="d5"></a>
## D5 — Sécurité des applications LLM / GenAI (LLM)

Mesures techniques spécifiques aux applications fondées sur des grands modèles de langage.

### LLM.1 — Injection de prompt et manipulation des entrées

Défenses contre les injections directes et indirectes, filtrage des entrées, protection du prompt système.

*Références : OWASP_LLM LLM01, LLM07 · NIST_600_1 Information Security · NIST_AI_RMF MEASURE 2.7 · ISO27001 A.8.28*

#### LLM.1.1 — Des protections contre l'injection de prompt (directe et indirecte) sont-elles en place ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Architecture des garde-fous, configuration des filtres, rapports de tests d'injection.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune protection. | Recenser les points d'entrée des applications LLM (utilisateurs, documents, pages web, e-mails). |
| 2 — Reproductible | Uniquement des instructions défensives dans le prompt système. | Mettre en place séparation instructions / données, filtrage des entrées et guardrails. |
| 3 — Défini | Séparation instructions / données, filtrage des entrées, garde-fous (guardrails) et marquage des contenus externes. | Tester régulièrement les défenses et journaliser les tentatives d'injection. |
| 4 — Maîtrisé | Défenses en profondeur testées régulièrement ; tentatives détectées et journalisées. | Mettre à jour les défenses selon la veille et les résultats de red teaming. |
| 5 — Optimisé | Défenses adaptatives mises à jour selon les nouvelles techniques d'attaque et le red teaming. | — |

#### LLM.1.2 — Le prompt système est-il protégé et dépourvu d'informations sensibles ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Revue des prompts système, tests d'extraction du prompt.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Le prompt système contient des secrets ou des règles de sécurité sensibles. | Retirer les secrets et informations sensibles des prompts système. |
| 2 — Reproductible | Le risque est connu mais aucune règle n'existe. | Déplacer les contrôles d'autorisation hors du LLM, dans le code applicatif. |
| 3 — Défini | Règle : aucune donnée sensible dans le prompt système ; les contrôles d'autorisation sont appliqués hors du LLM. | Tester régulièrement l'extraction du prompt système. |
| 4 — Maîtrisé | Des tests réguliers tentent d'extraire le prompt système. | Surveiller les fuites et revoir les prompts en continu. |
| 5 — Optimisé | Les fuites sont surveillées et les prompts revus en continu. | — |

### LLM.2 — Divulgation d'informations et gestion des sorties

Prévention des fuites de données sensibles et validation des sorties avant usage par d'autres composants.

*Références : OWASP_LLM LLM02, LLM05 · NIST_600_1 Data Privacy, Information Integrity · ISO27001 A.8.12*

#### LLM.2.1 — Les sorties des LLM sont-elles validées avant d'être affichées ou transmises à d'autres systèmes ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Code de validation / encodage des sorties, tests XSS / injection SQL sur les sorties.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les sorties sont utilisées directement. | Identifier les flux où les sorties LLM sont exécutées, stockées ou affichées. |
| 2 — Reproductible | Validation ponctuelle sur certaines applications. | Traiter les sorties LLM comme des entrées utilisateur non fiables (encodage, validation). |
| 3 — Défini | Les sorties sont traitées comme non fiables : encodage, validation de format, requêtes paramétrées. | Automatiser et tester la validation des sorties. |
| 4 — Maîtrisé | La validation est automatisée et testée (XSS, injection SQL, exécution de code). | Améliorer les contrôles à partir des incidents et du red teaming. |
| 5 — Optimisé | Les contrôles de sortie sont améliorés en continu à partir des incidents. | — |

#### LLM.2.2 — Des mécanismes empêchent-ils le LLM de divulguer des données sensibles ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Configuration du masquage des données personnelles, tests d'extraction de données.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun mécanisme. | Identifier les données sensibles accessibles au LLM. |
| 2 — Reproductible | Uniquement des consignes dans le prompt. | Mettre en place le masquage des données personnelles et le cloisonnement par utilisateur. |
| 3 — Défini | Filtrage des données sensibles en entrée et en sortie (masquage des données personnelles) et cloisonnement par utilisateur. | Tester l'extraction de données et mettre en place des alertes. |
| 4 — Maîtrisé | Tests réguliers d'extraction de données et alertes en cas de fuite. | Adopter des protections avancées (classification automatique, confidentialité différentielle). |
| 5 — Optimisé | Protections avancées : classification automatique, confidentialité différentielle à l'entraînement. | — |

### LLM.3 — Agentivité et permissions des agents

Limitation des outils, des permissions et de l'autonomie des agents IA ; validation humaine des actions sensibles.

*Références : OWASP_LLM LLM06 · NIST_600_1 Human-AI Configuration · ISO27001 A.8.2, A.5.15*

#### LLM.3.1 — Les outils et permissions des agents IA sont-ils limités au strict nécessaire ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Liste des outils accessibles aux agents, comptes de service dédiés, revue des permissions.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les agents ont des accès larges (lecture, écriture, suppression) sans restriction. | Inventorier les outils et permissions de chaque agent IA. |
| 2 — Reproductible | Restrictions partielles décidées au cas par cas. | Appliquer le moindre privilège : liste blanche d'outils et comptes dédiés. |
| 3 — Défini | Moindre privilège : liste blanche d'outils, comptes dédiés, droits minimaux. | Revoir les permissions périodiquement et journaliser les actions des agents. |
| 4 — Maîtrisé | Permissions revues périodiquement ; actions des agents journalisées et surveillées. | Mettre en place des permissions dynamiques et une détection d'anomalies. |
| 5 — Optimisé | Permissions dynamiques et contextuelles avec détection des comportements anormaux. | — |

#### LLM.3.2 — Les actions sensibles déclenchées par une IA (paiement, suppression, envoi externe) nécessitent-elles une validation humaine ?

Criticité : **Critique** · Effort de mise en œuvre : Faible  
Preuves attendues : Liste des actions sensibles, captures du flux de validation, journaux d'approbation.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune validation humaine. | Identifier toutes les actions sensibles qu'une IA peut déclencher. |
| 2 — Reproductible | Validation pour certaines actions seulement, de manière informelle. | Imposer une validation humaine documentée pour ces actions. |
| 3 — Défini | Une liste documentée d'actions sensibles impose une validation humaine. | Implémenter la validation techniquement et tester son contournement. |
| 4 — Maîtrisé | La validation est imposée techniquement, tracée, et sa résistance au contournement est testée. | Ajuster les seuils de validation selon le niveau de risque. |
| 5 — Optimisé | Les seuils de validation sont ajustés selon le risque et les retours d'expérience. | — |

### LLM.4 — Sécurité RAG, vecteurs et embeddings

Contrôle d'accès aux bases vectorielles, intégrité des documents indexés, cloisonnement des données.

*Références : OWASP_LLM LLM08, LLM04 · NIST_600_1 Information Security · ISO27001 A.8.3*

#### LLM.4.1 — L'accès aux documents d'une base RAG respecte-t-il les droits de l'utilisateur qui interroge ?

Criticité : **Critique** · Effort de mise en œuvre : Élevé  
Preuves attendues : Conception du filtrage par permissions, tests de cloisonnement entre utilisateurs.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Tous les documents sont accessibles à tous les utilisateurs. | Cartographier les sources indexées et leur niveau de sensibilité. |
| 2 — Reproductible | Séparation grossière par application. | Appliquer un filtrage par permissions au niveau document lors de la recherche. |
| 3 — Défini | Un contrôle d'accès au niveau document est appliqué lors de la recherche (filtrage par permissions). | Tester le cloisonnement entre utilisateurs et journaliser les accès. |
| 4 — Maîtrisé | Le cloisonnement est testé et les accès sont journalisés. | Synchroniser automatiquement les droits avec les systèmes sources. |
| 5 — Optimisé | Les droits sont synchronisés automatiquement avec les sources et audités en continu. | — |

#### LLM.4.2 — L'intégrité des documents indexés et de la base vectorielle est-elle contrôlée ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Procédure d'ingestion, liste des sources autorisées, droits d'écriture sur la base vectorielle.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun contrôle sur ce qui est indexé. | Définir les sources autorisées pour l'indexation. |
| 2 — Reproductible | Contrôle manuel ponctuel. | Contrôler le processus d'ingestion et restreindre l'accès en écriture. |
| 3 — Défini | Sources validées, processus d'ingestion contrôlé, accès en écriture restreint. | Détecter les contenus malveillants à l'ingestion. |
| 4 — Maîtrisé | Détection des contenus malveillants (instructions cachées) à l'ingestion ; traçabilité des sources. | Surveiller et nettoyer la base vectorielle en continu. |
| 5 — Optimisé | Surveillance continue et nettoyage automatique de la base. | — |

---

<a id="d6"></a>
## D6 — Opérations & résilience (OPS)

Exploitation sécurisée des systèmes IA : accès, surveillance, disponibilité, incidents.

### OPS.1 — Gestion des identités et des accès

Authentification forte, moindre privilège et revue des accès aux modèles, données et API IA.

*Références : ISO27001 A.5.15, A.5.16, A.5.17, A.5.18, A.8.5*

#### OPS.1.1 — L'accès aux systèmes IA (modèles, données, plateformes MLOps, consoles des fournisseurs) est-il contrôlé selon le moindre privilège ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Procédure de gestion des accès, configuration MFA, comptes rendus de revue des accès.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Comptes partagés et accès non contrôlés. | Supprimer les comptes partagés et créer des comptes nominatifs. |
| 2 — Reproductible | Comptes nominatifs mais droits excessifs. | Formaliser la gestion des accès et activer la MFA. |
| 3 — Défini | Gestion des accès formalisée, moindre privilège et MFA pour les accès d'administration. | Revoir périodiquement les accès et automatiser la désactivation au départ. |
| 4 — Maîtrisé | Revues périodiques des accès et désactivation automatique au départ des collaborateurs. | Adopter l'accès juste-à-temps et une approche Zero Trust. |
| 5 — Optimisé | Accès juste-à-temps et approche Zero Trust. | — |

#### OPS.1.2 — Les API exposant des systèmes IA sont-elles protégées (authentification, autorisation, chiffrement) ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Configuration de la passerelle API, documentation OAuth2 / JWT, rapports de tests de sécurité.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | API ouvertes sans authentification. | Protéger toutes les API IA par une authentification. |
| 2 — Reproductible | Authentification simple par clé partagée. | Mettre en place OAuth2 / JWT, TLS et des autorisations par rôle. |
| 3 — Défini | Authentification forte (OAuth2 / JWT), TLS et autorisations par rôle. | Centraliser la protection via une passerelle API et la tester régulièrement. |
| 4 — Maîtrisé | Passerelle API avec politiques centralisées et tests de sécurité réguliers. | Ajouter une détection d'abus fondée sur le comportement. |
| 5 — Optimisé | Protection adaptative (détection d'abus, analyse comportementale). | — |

### OPS.2 — Surveillance et journalisation

Journalisation des requêtes et décisions IA, détection des dérives et des comportements anormaux.

*Références : ISO27001 A.8.15, A.8.16 · ISO42001 A.6.2.6, A.6.2.8 · NIST_AI_RMF MANAGE 4.1, MEASURE 2.4*

#### OPS.2.1 — Les interactions avec les systèmes IA (requêtes, réponses, décisions) sont-elles journalisées ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Politique de journalisation, extraits de journaux, règles de détection dans le SIEM.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune journalisation. | Activer la journalisation sur tous les systèmes IA. |
| 2 — Reproductible | Journaux techniques partiels. | Définir le contenu, la durée de conservation et la protection des journaux. |
| 3 — Défini | Requêtes, réponses et décisions sont journalisées avec une durée de conservation définie et des données protégées. | Centraliser les journaux dans le SIEM et créer des règles de détection IA. |
| 4 — Maîtrisé | Les journaux sont centralisés dans un SIEM avec des règles de détection spécifiques à l'IA. | Ajouter une analyse comportementale automatisée. |
| 5 — Optimisé | Analyse comportementale et corrélation automatisée des événements. | — |

#### OPS.2.2 — La performance et la dérive des modèles en production sont-elles surveillées ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Tableaux de bord de surveillance des modèles, seuils d'alerte, historique des alertes.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune surveillance. | Définir les indicateurs de performance clés de chaque modèle. |
| 2 — Reproductible | Les dégradations sont constatées par les utilisateurs. | Suivre régulièrement la performance et la dérive des données. |
| 3 — Défini | Des indicateurs de performance et de dérive sont définis et suivis régulièrement. | Automatiser les alertes et les seuils d'intervention. |
| 4 — Maîtrisé | Surveillance automatisée avec alertes et seuils déclenchant une action. | Automatiser les actions correctives selon des règles validées. |
| 5 — Optimisé | Réentraînement ou correction automatisés selon des règles validées. | — |

### OPS.3 — Disponibilité et consommation des ressources

Quotas, limitation de débit, maîtrise des coûts et plans de continuité des services IA.

*Références : OWASP_LLM LLM10 · ISO27001 A.8.6, A.5.30 · NIST_600_1 Environmental Impacts*

#### OPS.3.1 — Des limites (quotas, limitation de débit, taille des requêtes) protègent-elles contre la consommation excessive de ressources ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Configuration des quotas et limites, budgets et alertes de coûts.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune limite. | Mesurer la consommation actuelle des services IA. |
| 2 — Reproductible | Uniquement les limites par défaut du fournisseur. | Définir des quotas, limites de débit et budgets. |
| 3 — Défini | Quotas et limites de débit définis par utilisateur et par application, budgets de coûts fixés. | Surveiller les coûts avec alertes et réaliser des tests de charge. |
| 4 — Maîtrisé | Coûts surveillés avec alertes ; tests de charge réalisés. | Ajuster dynamiquement les limites et détecter les abus. |
| 5 — Optimisé | Limites ajustées dynamiquement et détection automatique des abus. | — |

#### OPS.3.2 — La continuité des services IA critiques est-elle assurée (plan de secours, fournisseur alternatif, mode dégradé) ?

Criticité : Importante · Effort de mise en œuvre : Élevé  
Preuves attendues : Plan de continuité, comptes rendus de tests de bascule.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun plan de continuité. | Identifier les services IA critiques pour l'activité. |
| 2 — Reproductible | Solutions de contournement informelles. | Documenter un plan de continuité incluant un mode dégradé. |
| 3 — Défini | Un plan de continuité documenté prévoit un mode dégradé sans IA. | Tester le plan et identifier un fournisseur alternatif. |
| 4 — Maîtrisé | Le plan est testé périodiquement et un fournisseur alternatif est identifié. | Mettre en place une bascule automatique multi-fournisseurs. |
| 5 — Optimisé | Bascule automatique et résilience multi-fournisseurs. | — |

### OPS.4 — Gestion des incidents IA

Procédure de détection, de réponse et de retour d'expérience pour les incidents propres à l'IA.

*Références : ISO27001 A.5.24, A.5.25, A.5.26, A.5.27 · NIST_AI_RMF MANAGE 4.3 · ISO42001 A.8.4*

#### OPS.4.1 — La procédure de gestion des incidents couvre-t-elle les incidents propres à l'IA ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Procédure d'incident incluant les scénarios IA, comptes rendus d'exercices, indicateurs de traitement.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune procédure de gestion des incidents. | Définir ce qu'est un incident IA et ses niveaux de gravité. |
| 2 — Reproductible | Une procédure générale existe, sans spécificités IA. | Intégrer les scénarios IA à la procédure de gestion des incidents. |
| 3 — Défini | La procédure couvre les incidents IA (fuite via un LLM, comportement dangereux, biais) avec rôles et escalade. | Réaliser des exercices et suivre des indicateurs de traitement. |
| 4 — Maîtrisé | Des exercices réguliers sont réalisés et des indicateurs (délais de détection et de résolution) suivis. | Systématiser les retours d'expérience et les partager. |
| 5 — Optimisé | Les retours d'expérience sont systématiques et partagés avec l'écosystème. | — |

#### OPS.4.2 — Les utilisateurs peuvent-ils facilement signaler un comportement anormal d'un système IA ?

Criticité : Normale · Effort de mise en œuvre : Faible  
Preuves attendues : Canal de signalement (bouton, formulaire), statistiques des signalements.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun canal de signalement. | Communiquer aux utilisateurs un moyen de signalement. |
| 2 — Reproductible | Signalement possible via le support général uniquement. | Créer un canal dédié avec une procédure de traitement. |
| 3 — Défini | Un canal dédié est connu des utilisateurs et les signalements sont traités selon une procédure. | Analyser les signalements et les suivre par des indicateurs. |
| 4 — Maîtrisé | Les signalements sont analysés et suivis par des indicateurs. | Exploiter les signalements pour améliorer les modèles. |
| 5 — Optimisé | Les signalements sont exploités pour améliorer les modèles en continu. | — |

---

<a id="d7"></a>
## D7 — IA digne de confiance (TRUST)

Transparence, équité, supervision humaine et fiabilité des systèmes IA.

### TRUST.1 — Transparence et explicabilité

Information des utilisateurs sur l'usage de l'IA, documentation des modèles, explicabilité des décisions.

*Références : ISO42001 A.8.2, A.6.2.7 · NIST_AI_RMF MEASURE 2.8, MEASURE 2.9*

#### TRUST.1.1 — Les utilisateurs sont-ils informés lorsqu'ils interagissent avec une IA ou reçoivent un contenu généré par IA ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Mentions d'information dans les interfaces, marquage des contenus générés.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les utilisateurs ne sont pas informés. | Ajouter une mention claire sur les interfaces utilisant l'IA. |
| 2 — Reproductible | Information parfois présente, selon les applications. | Rendre l'information et le marquage des contenus générés systématiques. |
| 3 — Défini | Information systématique et claire ; les contenus générés sont marqués. | Contrôler la conformité et mesurer la compréhension des utilisateurs. |
| 4 — Maîtrisé | La conformité est contrôlée et la compréhension des utilisateurs est mesurée. | Améliorer la transparence à partir des retours des utilisateurs. |
| 5 — Optimisé | Les pratiques de transparence sont améliorées à partir des retours des utilisateurs. | — |

#### TRUST.1.2 — Les décisions des systèmes IA ayant un impact sur les personnes sont-elles explicables ?

Criticité : Importante · Effort de mise en œuvre : Élevé  
Preuves attendues : Model cards, méthodes d'explicabilité utilisées (SHAP, LIME), exemples d'explications fournies.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les modèles fonctionnent comme des boîtes noires. | Identifier les décisions IA qui ont un impact sur des personnes. |
| 2 — Reproductible | Des explications sont fournies au cas par cas par des experts. | Documenter les modèles (model cards) et appliquer des méthodes d'explicabilité. |
| 3 — Défini | Documentation (model cards) et méthodes d'explicabilité pour tous les systèmes à impact. | Fournir des explications compréhensibles aux personnes concernées et en évaluer la qualité. |
| 4 — Maîtrisé | Des explications compréhensibles sont fournies aux personnes concernées et leur qualité est évaluée. | Intégrer l'explicabilité comme exigence de conception. |
| 5 — Optimisé | L'explicabilité est intégrée dès la conception de chaque système. | — |

### TRUST.2 — Équité et gestion des biais

Détection, mesure et correction des biais discriminatoires.

*Références : ISO42001 A.5.4 · NIST_AI_RMF MEASURE 2.11 · NIST_600_1 Harmful Bias and Homogenization*

#### TRUST.2.1 — Les biais des modèles sont-ils mesurés et corrigés ?

Criticité : **Critique** · Effort de mise en œuvre : Élevé  
Preuves attendues : Métriques d'équité retenues, rapports d'analyse des biais, plans de correction.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Les biais ne sont pas mesurés. | Identifier les groupes potentiellement affectés par chaque système IA. |
| 2 — Reproductible | Vérifications ponctuelles et informelles. | Définir des métriques d'équité et les mesurer avant chaque déploiement. |
| 3 — Défini | Des métriques d'équité sont définies et mesurées avant déploiement sur les groupes concernés. | Surveiller les biais en production avec des seuils et plans de correction. |
| 4 — Maîtrisé | Les biais sont mesurés en continu en production avec seuils et plans de correction. | Faire auditer les biais par un tiers indépendant. |
| 5 — Optimisé | Amélioration continue et audit externe des biais. | — |

#### TRUST.2.2 — Les parties prenantes concernées sont-elles consultées et la diversité des équipes est-elle prise en compte dans les projets IA à impact ?

Criticité : Normale · Effort de mise en œuvre : Moyen  
Preuves attendues : Comptes rendus de consultation, composition des équipes projet.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune consultation. | Identifier les parties prenantes concernées par chaque projet à impact. |
| 2 — Reproductible | Consultations ponctuelles. | Organiser leur consultation et veiller à la diversité des équipes. |
| 3 — Défini | Consultation des parties prenantes et diversité des équipes prises en compte dans les projets à impact. | Formaliser le processus de consultation et tracer la prise en compte des retours. |
| 4 — Maîtrisé | Processus formel de consultation ; les retours sont intégrés et tracés. | Associer les parties prenantes à tout le cycle de vie. |
| 5 — Optimisé | Participation continue des parties prenantes tout au long du cycle de vie. | — |

### TRUST.3 — Supervision humaine

Capacité humaine à surveiller, interpréter, contester et interrompre les décisions IA.

*Références : ISO42001 A.9.2, A.9.3, A.9.4 · NIST_AI_RMF GOVERN 3.2, MAP 3.5 · NIST_600_1 Human-AI Configuration*

#### TRUST.3.1 — Un humain peut-il surveiller, contester et corriger les décisions d'un système IA à risque élevé ?

Criticité : **Critique** · Effort de mise en œuvre : Moyen  
Preuves attendues : Description du dispositif de supervision humaine, procédure de contestation, statistiques de correction.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Décisions entièrement automatisées, sans recours. | Identifier les décisions automatisées à risque élevé. |
| 2 — Reproductible | Un recours est possible mais n'est pas organisé. | Définir le mode de supervision humaine et une procédure de contestation. |
| 3 — Défini | La supervision humaine est définie (humain dans la boucle / sur la boucle) avec une procédure de contestation. | Mesurer l'efficacité de la supervision et le biais d'automatisation. |
| 4 — Maîtrisé | L'efficacité de la supervision est contrôlée (taux de correction, biais d'automatisation). | Ajuster le niveau de supervision selon le risque. |
| 5 — Optimisé | Le niveau de supervision est optimisé en fonction du risque. | — |

#### TRUST.3.2 — Est-il possible d'arrêter ou de désactiver rapidement un système IA en cas de problème ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Procédure d'arrêt d'urgence (kill switch), responsables désignés, test d'arrêt.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Impossible ou non prévu. | Prévoir un moyen technique d'arrêter chaque système IA critique. |
| 2 — Reproductible | Possible, mais la procédure n'est pas connue. | Documenter la procédure d'arrêt et désigner les responsables. |
| 3 — Défini | Un mécanisme d'arrêt documenté existe avec des responsables désignés. | Tester périodiquement le mécanisme d'arrêt. |
| 4 — Maîtrisé | Le mécanisme d'arrêt est testé périodiquement. | Mettre en place un arrêt automatique sur seuils de risque. |
| 5 — Optimisé | Arrêt automatique déclenché par des seuils de risque. | — |

### TRUST.4 — Fiabilité et désinformation

Maîtrise des hallucinations (confabulation), validation des réponses, sources vérifiables.

*Références : OWASP_LLM LLM09 · NIST_600_1 Confabulation, Information Integrity · NIST_AI_RMF MEASURE 2.5*

#### TRUST.4.1 — Des mesures limitent-elles les hallucinations et la désinformation produites par les systèmes IA ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Architecture RAG, citations des sources, avertissements, résultats des jeux d'évaluation.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune mesure. | Afficher des avertissements et identifier les usages critiques. |
| 2 — Reproductible | Un avertissement générique est affiché. | Ancrer les réponses sur des sources fiables, citer les sources et imposer une vérification humaine. |
| 3 — Défini | Ancrage sur des sources fiables (RAG), citation des sources et vérification humaine pour les usages critiques. | Mesurer le taux d'hallucination avec des jeux d'évaluation. |
| 4 — Maîtrisé | Le taux d'hallucination est mesuré par des jeux d'évaluation. | Améliorer en continu à partir des mesures. |
| 5 — Optimisé | Amélioration continue fondée sur les mesures. | — |

#### TRUST.4.2 — L'exactitude des systèmes IA est-elle évaluée par rapport à des critères métier définis ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Critères d'exactitude, jeux d'évaluation de référence, rapports d'évaluation.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune évaluation. | Définir avec le métier ce qu'est une réponse correcte. |
| 2 — Reproductible | Évaluation subjective par les utilisateurs. | Construire des jeux d'évaluation de référence. |
| 3 — Défini | Des critères d'exactitude et des jeux d'évaluation de référence sont définis. | Évaluer régulièrement en production et comparer aux critères. |
| 4 — Maîtrisé | L'exactitude est évaluée régulièrement en production et comparée aux critères. | Comparer à l'état de l'art et améliorer en continu. |
| 5 — Optimisé | Comparaison à l'état de l'art et amélioration continue. | — |

---

<a id="d8"></a>
## D8 — Culture & amélioration continue (PEOPLE)

Compétences, usage responsable de l'IA par les collaborateurs, audit et amélioration.

### PEOPLE.1 — Sensibilisation et formation

Formation des équipes aux risques IA, adaptée aux rôles (direction, développeurs, utilisateurs).

*Références : ISO27001 7.2, 7.3, A.6.3 · ISO42001 7.2, 7.3, A.4.6 · NIST_AI_RMF GOVERN 2.2*

#### PEOPLE.1.1 — Les collaborateurs sont-ils sensibilisés aux risques liés à l'IA ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Supports de sensibilisation, taux de participation, résultats de quiz.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune sensibilisation. | Organiser une première session de sensibilisation aux risques de l'IA. |
| 2 — Reproductible | Communications ponctuelles. | Mettre en place un programme de sensibilisation pour tous les collaborateurs. |
| 3 — Défini | Un programme de sensibilisation couvre tous les collaborateurs (shadow AI, fuite de données, hallucinations). | Rendre le programme obligatoire et mesurer son efficacité. |
| 4 — Maîtrisé | L'efficacité est mesurée (quiz, simulations) et le programme est obligatoire. | Actualiser le programme à partir des incidents et des nouveaux usages. |
| 5 — Optimisé | Le programme est adapté en continu aux incidents et aux nouveaux usages. | — |

#### PEOPLE.1.2 — Les équipes techniques (data scientists, développeurs, exploitants) sont-elles formées à la sécurité de l'IA ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Plan de formation, attestations, certifications obtenues.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune formation. | Identifier les besoins de formation des équipes techniques. |
| 2 — Reproductible | Autoformation individuelle. | Mettre en place un parcours de formation à la sécurité de l'IA. |
| 3 — Défini | Un parcours de formation à la sécurité de l'IA (OWASP LLM, MLSecOps) est suivi par les équipes techniques. | Évaluer les compétences et actualiser régulièrement la formation. |
| 4 — Maîtrisé | Les compétences sont évaluées et la formation est mise à jour régulièrement. | Encourager la contribution à la communauté sécurité IA. |
| 5 — Optimisé | Les équipes contribuent à la communauté (publications, conférences, outils). | — |

### PEOPLE.2 — Usage encadré de l'IA par les collaborateurs

Charte d'usage de l'IA générative, outils autorisés, lutte contre le shadow AI.

*Références : ISO42001 A.9.2 · ISO27001 A.5.10 · NIST_600_1 Data Privacy, Intellectual Property*

#### PEOPLE.2.1 — Une charte d'usage de l'IA générative définit-elle les outils autorisés et les usages interdits ?

Criticité : **Critique** · Effort de mise en œuvre : Faible  
Preuves attendues : Charte d'usage de l'IA, liste des outils autorisés, preuve d'acceptation par les collaborateurs.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune charte ; chacun utilise les outils de son choix. | Communiquer rapidement des règles de base sur l'usage de l'IA générative. |
| 2 — Reproductible | Recommandations informelles. | Rédiger et faire approuver une charte d'usage de l'IA générative. |
| 3 — Défini | Une charte approuvée définit les outils autorisés, les usages interdits et les données à ne pas partager. | Tracer l'acceptation de la charte et contrôler son respect. |
| 4 — Maîtrisé | L'acceptation de la charte est tracée et son respect contrôlé. | Réviser régulièrement la charte avec les utilisateurs. |
| 5 — Optimisé | La charte est révisée régulièrement avec les retours des utilisateurs. | — |

#### PEOPLE.2.2 — L'usage d'outils IA non autorisés (shadow AI) est-il détecté ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Rapports du proxy / CASB sur les services IA, actions de suivi.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune détection. | Interroger les équipes sur les outils IA qu'elles utilisent. |
| 2 — Reproductible | Découverte fortuite. | Analyser périodiquement les journaux réseau pour identifier les services IA. |
| 3 — Défini | Des analyses périodiques des journaux réseau identifient les services IA utilisés. | Automatiser la détection et proposer des alternatives autorisées. |
| 4 — Maîtrisé | Détection automatique avec alertes et proposition d'alternatives autorisées. | Faire évoluer le catalogue d'outils selon les besoins détectés. |
| 5 — Optimisé | Le catalogue d'outils autorisés évolue selon les besoins détectés. | — |

### PEOPLE.3 — Audit interne et amélioration continue

Audits internes du dispositif IA, revue de direction, actions correctives.

*Références : ISO42001 9.2, 9.3, 10.1, 10.2 · ISO27001 9.2, 9.3, 10 · NIST_AI_RMF GOVERN 1.5*

#### PEOPLE.3.1 — Des audits internes du dispositif de gouvernance et de sécurité de l'IA sont-ils réalisés ?

Criticité : Importante · Effort de mise en œuvre : Moyen  
Preuves attendues : Programme d'audit, rapports d'audit, revue de direction.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucun audit. | Réaliser un premier audit interne du dispositif IA (cette évaluation peut servir de base). |
| 2 — Reproductible | Audits ponctuels à la suite d'un incident. | Planifier un programme d'audit annuel. |
| 3 — Défini | Un programme d'audit interne couvre le dispositif IA au moins une fois par an. | Présenter les résultats en revue de direction et suivre des indicateurs. |
| 4 — Maîtrisé | Les résultats d'audit alimentent la revue de direction et des indicateurs. | Préparer un audit externe ou une certification ISO/IEC 42001. |
| 5 — Optimisé | Audits externes ou certification ISO/IEC 42001 visée ou obtenue. | — |

#### PEOPLE.3.2 — Les non-conformités et incidents donnent-ils lieu à des actions correctives suivies jusqu'à leur clôture ?

Criticité : Importante · Effort de mise en œuvre : Faible  
Preuves attendues : Registre des actions correctives, taux de clôture, analyses des causes.

| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |
|---|---|---|
| 1 — Initial | Aucune action corrective. | Recenser les non-conformités et incidents récents. |
| 2 — Reproductible | Corrections ponctuelles sans suivi. | Tenir un registre des actions correctives avec responsables et échéances. |
| 3 — Défini | Un registre des actions correctives avec responsables et échéances est tenu. | Analyser les causes et vérifier l'efficacité des actions. |
| 4 — Maîtrisé | Les causes sont analysées et l'efficacité des actions est vérifiée. | Analyser les tendances pour prévenir les récurrences. |
| 5 — Optimisé | Les tendances sont analysées pour prévenir les récurrences. | — |
