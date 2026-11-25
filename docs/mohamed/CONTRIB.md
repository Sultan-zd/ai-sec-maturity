# Mohamed's Contributions — AIMA Project

> **Author:** Mohamed  
> **Date:** November 2026  
> **Role:** Governance, Evidence & Case Study  
> **Referential version affected:** 1.0.0 → 1.1.0  

This file documents **all changes made by Mohamed** before pushing to the shared repository.  
It satisfies the team rule: *"document everything before pushing your part"*.

---

## Summary of Changes

| Area | Type | File(s) | Description |
|------|------|---------|-------------|
| Referential | MODIFY | `referential/referentiel-v1.json` | Version bump 1.0.0→1.1.0, add EU AI Act source |
| Referential | MODIFY | `referential/questions/*.json` | Rebalance criticality: 41% → 15.3% critical |
| Documentation | NEW | `docs/mohamed/iso42001-matrix.md` | Full ISO/IEC 42001 Annex A ↔ AIMA mapping |
| Documentation | NEW | `docs/mohamed/ahp-sensitivity.md` | AHP weight validation results |
| Documentation | NEW | `docs/mohamed/inter-rater-reliability.md` | Multi-respondent reliability report |
| Case study | NEW | `scripts/case-study/scenario-multi-respondents.json` | 3-respondent scenario (RSSI, CDO, Risk Mgr) |
| Case study | NEW | `scripts/case-study/multi-respondent.mjs` | Inter-rater reliability analysis script |
| EU AI Act | NEW | `mohamed/eu-ai-act/eu_ai_act.py` | AI system classifier per EU AI Act 2024/1689 |
| EU AI Act | NEW | `mohamed/eu-ai-act/README.md` | Usage guide for the classifier |
| AHP | NEW | `mohamed/ahp/ahp_weights.py` | AHP pairwise comparison + sensitivity analysis |
| Evidence | NEW | `mohamed/evidence-assistant/assistant.py` | LLM-powered evidence assistant (OpenAI API) |
| Evidence | NEW | `mohamed/evidence-assistant/README.md` | Setup and usage guide |

---

## Task 1 — EU AI Act Mode

### What it does
Classifies any organization's AI systems into EU AI Act 2024/1689 risk categories
(Unacceptable / High / Limited / Minimal risk) and lists the applicable legal obligations
for each class.

### Files
- [`mohamed/eu-ai-act/eu_ai_act.py`](../mohamed/eu-ai-act/eu_ai_act.py) — Main classifier
- [`mohamed/eu-ai-act/README.md`](../mohamed/eu-ai-act/README.md) — Usage guide

### How to run
```bash
# Demo with Banque Atlas Digitale systems
python3 mohamed/eu-ai-act/eu_ai_act.py --demo

# Custom systems
python3 mohamed/eu-ai-act/eu_ai_act.py --input my_systems.json --output report.json
```

### Banque Atlas Digitale results
| System | AI Act Category | Key Obligation |
|--------|----------------|----------------|
| Atlas Assistant (LLM) | 🟡 Limited Risk (Art. 50) | Inform users they interact with AI |
| Credit Scoring Model | 🟠 High Risk (Annex III.5b) | Conformity assessment + EU DB registration |
| Fraud Detection Model | 🟠 High Risk (Annex III.5b) | Full Article 9–15 obligations |

### Design decisions
- **Classification logic:** Article 5 → Annex III → Article 50 → Minimal Risk (cascade)
- **Explicit override:** Systems can declare `annex_iii_category` to avoid false keyword matches
- **AIMA mapping:** Each category lists the AIMA questions most relevant for compliance verification
- **No external dependencies:** Pure stdlib Python — no install needed beyond the script itself

---

## Task 2 — AI Evidence Assistant

### What it does
Accepts a company document (PDF, text policy, audit report) and uses an **OpenAI GPT** model
to suggest a maturity level (1–5) for each AIMA question, citing the exact paragraph from
the document as evidence. The auditor then confirms or overrides each suggestion.

### Files
- [`mohamed/evidence-assistant/assistant.py`](../mohamed/evidence-assistant/assistant.py) — Main script
- [`mohamed/evidence-assistant/README.md`](../mohamed/evidence-assistant/README.md) — Setup guide

### How to run
```bash
export OPENAI_API_KEY="sk-..."
pip install openai PyPDF2

# Analyze a document against all 59 questions
python3 mohamed/evidence-assistant/assistant.py --document company_policy.pdf

# Only governance and LLM domains
python3 mohamed/evidence-assistant/assistant.py --document audit.txt --domains GOV LLM

# Save to JSON
python3 mohamed/evidence-assistant/assistant.py --document policy.pdf --output suggestions.json
```

### Design decisions
- **Model:** `gpt-4o-mini` by default (fast, cheap). Use `--model gpt-4o` for best quality.
- **Batching:** 5 questions per API call to stay within token limits
- **Low temperature (0.1):** Forces the LLM to be factual, not creative
- **Confidence levels:** HIGH / MEDIUM / LOW based on evidence quality found
- **No hallucination by design:** If no evidence found → `suggestedLevel: null`, not a guess

---

## Task 3 — Multi-Respondent Case Study

### What it does
Extends the original Banque Atlas Digitale case study from 1 respondent (RSSI) to 3 independent
respondents (RSSI, CDO, IT Risk Manager), then computes **inter-rater reliability** to validate
how consistent the AIMA questionnaire is across different knowledgeable people.

### Files
- [`scripts/case-study/scenario-multi-respondents.json`](../scripts/case-study/scenario-multi-respondents.json) — 3×59 answers
- [`scripts/case-study/multi-respondent.mjs`](../scripts/case-study/multi-respondent.mjs) — Analysis script
- [`docs/mohamed/inter-rater-reliability.md`](inter-rater-reliability.md) — Full report (auto-generated)

### How to run
```bash
node scripts/case-study/multi-respondent.mjs
# Report written to docs/mohamed/inter-rater-reliability.md
```

### Results (November 2026)
| Metric | Value |
|--------|-------|
| Exact agreement | 40.7% (24/59 questions) |
| Within-1 agreement | 94.9% (56/59 questions) |
| Kappa-like coefficient κ | **0.258 (Fair)** |
| Contested questions (spread ≥ 2) | 3 questions |

**Interpretation:** κ = 0.258 is in the "Fair" range (0.20–0.40), which is acceptable for a
complex governance questionnaire. The 94.9% within-1 rate confirms the grid is well-calibrated —
disagreements are small (±1 level), not wild divergences. The main source of variability is
role-based perspective (RSSI vs CDO vs Risk Manager), not questionnaire ambiguity.

---

## Task 4 — AHP Weight Validation & Sensitivity Analysis

### What it does
Validates the AIMA domain weights (GOV=15%, RISK=15%, ..., PEOPLE=5%) using the
**Analytic Hierarchy Process** (Saaty, 1980). Also runs a sensitivity analysis:
if any domain weight moves ±5%, does the global score or roadmap ranking change?

### Files
- [`mohamed/ahp/ahp_weights.py`](../mohamed/ahp/ahp_weights.py) — AHP + sensitivity script
- [`docs/mohamed/ahp-sensitivity.md`](ahp-sensitivity.md) — Results documentation

### How to run
```bash
python3 mohamed/ahp/ahp_weights.py
```

### Results
| Metric | Value | Verdict |
|--------|-------|---------|
| λ_max | 8.0052 | Expected ≈ 8 for n=8 ✓ |
| Consistency Ratio (CR) | 0.0005 | ✅ Acceptable (< 0.10) |
| Max weight deviation (AHP vs current) | +1.9% (PEOPLE) | No significant difference |
| Sensitivity: score shift per ±5% | < 0.05 | ✅ Stable ranking |

**Conclusion:** The existing weight distribution is well-justified by AHP pairwise comparison.
No weight needs to be changed for AIMA v1.x. A future v2 could introduce sector-specific profiles.

---

## Task 5 — ISO/IEC 42001 Correspondence Matrix

### What it does
Maps every control in **ISO/IEC 42001:2023 Annex A** to AIMA question codes, and explicitly
identifies which controls are **not covered** (gaps). This makes the framework's regulatory
grounding verifiable and transparent.

### Files
- [`docs/mohamed/iso42001-matrix.md`](iso42001-matrix.md) — Full matrix

### Results
| Status | Controls | % |
|--------|----------|---|
| ✅ Fully covered | 26 | 65% |
| 🟡 Partially covered | 9 | 22.5% |
| ⚠️ Not covered (gaps) | 5 | 12.5% |
| **Total** | **40** | **100%** |

**Identified gaps (candidates for AIMA v2.0):**
- A.3.3 — Conflict of interest
- A.3.4 — Whistleblowing / reporting of concerns
- A.5.5 — Vulnerable groups
- A.6.2.8 — Decommissioning of AI systems
- A.9.4 — Contestability of AI decisions

---

## Task 6 — Rebalance Criticality Levels

### What it does
Reduces the share of `criticality: 3` questions from **24/59 (40.7%)** to **9/59 (15.3%)**
so that the **weakest-link rule** (a critical question at level 1 caps the domain at 2.00)
keeps its discriminating power. Too many critical questions dilutes the rule.

### Rubric for criticality 3
A question is `criticality: 3` **only if** its failure alone can cause:
- A direct violation of a binding regulation (AI Act, GDPR/09-08), **or**
- A critical safety or security incident with no compensating control available

### Questions kept at criticality 3 (9 total)
| Question | Domain | Why critical |
|----------|--------|-------------|
| GOV.1.2 | GOV | AI Policy: regulatory obligation under ISO 42001 + AI Act |
| GOV.4.1 | GOV | Legal tracking: direct compliance obligation |
| LLM.1.1 | LLM | Prompt injection: direct active attack vector |
| LLM.2.2 | LLM | Data disclosure: GDPR/09-08 violation risk |
| LLM.3.2 | LLM | Human validation of sensitive actions: safety-critical |
| LLM.4.1 | LLM | RAG access control: direct data breach risk |
| RISK.1.1 | RISK | Risk methodology: foundation of the entire framework |
| TRUST.2.1 | TRUST | Bias control: required for AI Act high-risk systems |
| DATA.2.1 | DATA | Personal data protection: GDPR direct requirement |

### Validation
```bash
node scripts/referential-tool.mjs --check
# ✅ Référentiel valide : 8 domaines, 29 sous-domaines, 59 questions (criticité 1/2/3 : 4/46/9)
```

---

## Referential Changes (v1.0.0 → v1.1.0)

### `referential/referentiel-v1.json`
1. **Version:** `"1.0.0"` → `"1.1.0"`
2. **New source entry:**
   ```json
   { "code": "EU_AI_ACT", "name": "EU AI Act 2024/1689 (Regulation (EU) 2024/1689)" }
   ```

### Questions changed (15 questions downgraded from criticality 3 → 2)
`DATA.2.2`, `DATA.3.1`, `DEV.1.2`, `DEV.2.1`, `DEV.2.2`, `DEV.3.1`,
`GOV.2.1`, `GOV.3.1`, `LLM.2.1`, `LLM.3.1`, `OPS.1.1`, `OPS.4.1`,
`PEOPLE.2.1`, `RISK.2.1`, `TRUST.3.1`

> **Backward compatibility:** Score calculations for existing assessments are **not affected**
> by criticality changes unless those assessments used the weakest-link rule for one of the
> 15 downgraded questions. The JSON shape is unchanged — this is a minor version bump (1.1.0).

---

## How to Reproduce Everything

```bash
# 1. Validate the referential
cd scripts && npm ci && cd ..
node scripts/referential-tool.mjs --check

# 2. Run multi-respondent reliability analysis
node scripts/case-study/multi-respondent.mjs

# 3. Run AHP weight validation
python3 mohamed/ahp/ahp_weights.py

# 4. Run EU AI Act classification (demo)
python3 mohamed/eu-ai-act/eu_ai_act.py --demo

# 5. Run evidence assistant (requires OPENAI_API_KEY)
export OPENAI_API_KEY="sk-..."
python3 mohamed/evidence-assistant/assistant.py --document <your_doc.pdf>
```

---

## Git Commit Message

```
feat(governance): Mohamed's tasks — EU AI Act mode, evidence assistant,
multi-respondent case study, AHP weight validation, ISO 42001 matrix,
criticality rebalancing (41%->15%, referential v1.1.0)
```
