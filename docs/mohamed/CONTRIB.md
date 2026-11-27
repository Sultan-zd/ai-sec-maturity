# Mohamed's Contributions — AIMA Project

> **Author:** Mohamed  
> **Date:** November 2026  
> **Role:** Governance, Evidence & Case Study  
> **Referential version affected:** 1.0.0 → **2.0.0** (major)  
> **Detailed verification log:** [`audit-changelog.md`](audit-changelog.md)

This file documents **all changes made by Mohamed** before pushing to the shared
repository. It satisfies the team rule: *"document everything before pushing your
part"*. Every number below is reproducible with the commands in
[§ How to reproduce](#how-to-reproduce-everything).

---

## Summary of Changes

| Area | Type | File(s) | Description |
|------|------|---------|-------------|
| Referential | MODIFY | `referential/referentiel-v1.json` | Version bump 1.0.0 → **2.0.0**; EU AI Act source added |
| Referential | MODIFY | `referential/questions/*.json` | Rebalance criticality: 24/59 → 9/59 critical (40.7% → 15.3%) |
| Documentation | NEW | `docs/mohamed/iso42001-matrix.md` | ISO/IEC 42001 Annex A ↔ AIMA mapping (38 controls) |
| Documentation | NEW | `docs/mohamed/ahp-sensitivity.md` | AHP weight validation and ±5% sensitivity results |
| Documentation | NEW | `docs/mohamed/inter-rater-reliability.md` | Multi-respondent reliability report |
| Documentation | NEW | `docs/mohamed/audit-changelog.md` | Defect audit and verification log |
| ISO 42001 | NEW | `mohamed/iso42001/mapping.json` | Machine-readable 38-control mapping |
| ISO 42001 | NEW | `mohamed/iso42001/iso42001_matrix.py` | Validator + Markdown generator (`--check`) |
| Case study | NEW | `scripts/case-study/scenario-multi-respondents.json` | 3-respondent scenario (RSSI, CDO, Risk Manager) |
| Case study | NEW | `scripts/case-study/multi-respondent.mjs` | Fleiss' κ + weighted κ + bootstrap CI |
| EU AI Act | NEW | `mohamed/eu-ai-act/eu_ai_act.py` | AI system classifier per EU AI Act 2024/1689 |
| EU AI Act | NEW | `mohamed/eu-ai-act/test_eu_ai_act.py` | 13 unit tests |
| EU AI Act | MODIFY | `mohamed/eu-ai-act/eu-ai-act-report.md` | Regenerated with AIMA 2.0.0 and correct fraud classification |
| AHP | NEW | `mohamed/ahp/ahp_weights.py` | AHP pairwise comparison + sensitivity analysis |
| Evidence | NEW | `mohamed/evidence-assistant/assistant.py` | LLM evidence assistant (OpenAI API) |
| Evidence | NEW | `mohamed/evidence-assistant/review.py` | Auditor confirm/override/skip workflow |
| Evidence | NEW | `mohamed/evidence-assistant/requirements.txt` | `openai`, `PyPDF2` |
| Evidence | NEW | `mohamed/evidence-assistant/test_assistant.py` | 18 tests with a fake OpenAI client |
| Evidence | NEW | `mohamed/evidence-assistant/test_review.py` | 15 review-workflow tests |

---

## Task 1 — EU AI Act Mode

### What it does

Classifies any organization's AI systems into EU AI Act 2024/1689 risk
categories (Unacceptable / High / Limited / Minimal) and lists the applicable
legal obligations for each class.

### Files

- [`mohamed/eu-ai-act/eu_ai_act.py`](../../mohamed/eu-ai-act/eu_ai_act.py) — classifier
- [`mohamed/eu-ai-act/test_eu_ai_act.py`](../../mohamed/eu-ai-act/test_eu_ai_act.py) — tests
- [`mohamed/eu-ai-act/README.md`](../../mohamed/eu-ai-act/README.md) — usage guide
- [`mohamed/eu-ai-act/eu-ai-act-report.md`](../../mohamed/eu-ai-act/eu-ai-act-report.md) — generated demo report

### How to run

```bash
# Demo with the Banque Atlas Digitale systems
python3 mohamed/eu-ai-act/eu_ai_act.py --demo

# Custom systems
python3 mohamed/eu-ai-act/eu_ai_act.py --input my_systems.json --output report.json

# Tests (no network)
python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py' -v
```

### Banque Atlas Digitale results (AIMA 2.0.0)

| System | AI Act category | Key obligation |
|--------|-----------------|----------------|
| Atlas Assistant (LLM) | 🟡 Limited Risk (Art. 50) | Inform users they interact with AI |
| Credit Scoring Model | 🟠 High Risk (Annex III.5b) | Conformity assessment + EU database registration |
| Fraud Detection Model | 🟢 Minimal Risk | No AI Act obligation; GDPR still applies |

### Design decisions

- **Classification cascade:** Article 5 → Annex III → Article 50 → Minimal Risk.
- **Explicit override:** a system can declare `annex_iii_category` to avoid a
  false keyword match.
- **Fraud detection is minimal risk.** Annex III.5b only covers
  creditworthiness/credit scoring and Annex III.5c covers insurance pricing.
  Transaction fraud detection is not listed, so it is **not** high risk. This
  corrects the earlier version, which wrongly used an explicit Annex III
  override.
- **Stable output path:** without `--output`, the report is written next to the
  script, never in the caller's working directory.
- **Version:** the report reads the AIMA version from
  `referential/referentiel-v1.json` (currently **2.0.0**) instead of hardcoding it.
- **No external dependencies:** pure standard-library Python.

---

## Task 2 — AI Evidence Assistant

### What it does

Accepts a company document (PDF, text policy, audit report) and uses an
**OpenAI GPT** model to suggest a maturity level (1–5) for each AIMA question,
citing the exact paragraph from the document as evidence. An auditor then
confirms, overrides or skips each suggestion with `review.py`.

### Files

- [`mohamed/evidence-assistant/assistant.py`](../../mohamed/evidence-assistant/assistant.py) — analysis script
- [`mohamed/evidence-assistant/review.py`](../../mohamed/evidence-assistant/review.py) — auditor review workflow
- [`mohamed/evidence-assistant/requirements.txt`](../../mohamed/evidence-assistant/requirements.txt) — dependencies
- [`mohamed/evidence-assistant/test_assistant.py`](../../mohamed/evidence-assistant/test_assistant.py)
- [`mohamed/evidence-assistant/test_review.py`](../../mohamed/evidence-assistant/test_review.py)
- [`mohamed/evidence-assistant/README.md`](../../mohamed/evidence-assistant/README.md) — setup guide

### How to run

```bash
pip install -r mohamed/evidence-assistant/requirements.txt
export OPENAI_API_KEY="sk-..."

# Analyze a document against all 59 questions
python3 mohamed/evidence-assistant/assistant.py --document company_policy.pdf

# Only GOV and LLM domains, save suggestions
python3 mohamed/evidence-assistant/assistant.py \
  --document audit.txt --domains GOV LLM --output suggestions.json

# Auditor review
python3 mohamed/evidence-assistant/review.py --input suggestions.json

# Tests (fake OpenAI client, no key needed)
python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py' -v
```

### Design decisions

- **Model:** `gpt-4o-mini` by default (fast, cheap); `--model gpt-4o` for quality.
- **Batching:** 5 questions per API call (12 batches for 59 questions).
- **Low temperature (0.1):** factual, not creative answers.
- **No hallucination by design:** if no evidence is found, the level is `null`,
  not a guess.
- **Lazy SDK import:** `--help` and document/referential loading work without
  `openai` installed.
- **Reliable JSON:** the prompt asks for `{"suggestions": [...]}`, matching
  `response_format={"type": "json_object"}`. Parsing accepts objects, bare
  arrays, Markdown fences and embedded JSON.
- **Validation:** unknown question codes are dropped; levels outside 1–5 become
  `null`; invalid confidence values become `LOW`; duplicates are ignored.
- **Honest coverage:** every question gets exactly one entry, even when an API
  batch fails. `meta.api_errors` counts failed batches.
- **Default referential path** is resolved relative to the script
  (`../../referential/questions`), so the tool works from any directory.
- **No stray files:** without `--output`, the report is printed only.

---

## Task 3 — Multi-Respondent Case Study

### What it does

Extends the Banque Atlas Digitale case study from one respondent (RSSI) to
three independent respondents (RSSI, CDO, IT Risk Manager) and computes
**inter-rater reliability** to validate how consistent the AIMA questionnaire is
across different knowledgeable people.

> ⚠️ **Simulated respondents.** The three profiles are **simulated** from the
> case study, not real employees. The report demonstrates the reliability
> *method* on a realistic questionnaire; it does not measure a real organization.

### Files

- [`scripts/case-study/scenario-multi-respondents.json`](../../scripts/case-study/scenario-multi-respondents.json) — 3 × 59 answers
- [`scripts/case-study/multi-respondent.mjs`](../../scripts/case-study/multi-respondent.mjs) — analysis script
- [`docs/mohamed/inter-rater-reliability.md`](inter-rater-reliability.md) — generated report

### How to run

```bash
node scripts/case-study/multi-respondent.mjs
# Report written to docs/mohamed/inter-rater-reliability.md
```

### Results (November 2026)

| Metric | Value |
|--------|-------|
| Exact agreement | 24/59 (40.7%) |
| Within-1 agreement | 56/59 (94.9%) |
| Contested questions (spread ≥ 2) | 3/59 (5.1%) |
| **Fleiss' κ (unweighted)** | **0.407** (95% CI [0.247, 0.540]) — Moderate |
| **Linear weighted κ** | **0.557** (95% CI [0.410, 0.669]) — Moderate |
| Mean pairwise Cohen's κ | 0.422 |

**Interpretation:** inter-rater reliability is **moderate**. The grid is usable
with several respondents, but a single respondent must not be treated as ground
truth. The 94.9% within-1 rate shows that disagreements are small (±1 level),
not wild divergences. The main source of variability is role-based perspective
(RSSI vs CDO vs Risk Manager), not questionnaire ambiguity.

**Why Fleiss' κ?** With three raters, the standard coefficient is Fleiss' kappa
(1971), not a pairwise average. It corrects for chance agreement using the
observed marginal distribution, not a fixed 20% assumption. The linear weighted
kappa gives partial credit for nearby levels and is the appropriate coefficient
for an ordinal 1–5 scale.

---

## Task 4 — AHP Weight Validation & Sensitivity Analysis

### What it does

Validates the AIMA domain weights (GOV/RISK/DATA/DEV/LLM = 15%, OPS/TRUST = 10%,
PEOPLE = 5%) using the **Analytic Hierarchy Process** (Saaty, 1980), then runs a
sensitivity analysis: if any domain weight moves **±5% (relative)**, does the
global score or the roadmap ranking change?

### Files

- [`mohamed/ahp/ahp_weights.py`](../../mohamed/ahp/ahp_weights.py) — AHP + sensitivity script
- [`docs/mohamed/ahp-sensitivity.md`](ahp-sensitivity.md) — generated results

### How to run

```bash
python3 mohamed/ahp/ahp_weights.py
```

### Results

| Metric | Value | Verdict |
|--------|-------|---------|
| λ_max | 8.0052 | Expected ≈ 8 for n = 8 ✓ |
| Consistency Ratio (CR) | 0.0005 | ✅ Acceptable (< 0.10) |
| Max AHP-vs-current weight difference | 1.9 percentage points (PEOPLE) | Coherent with current weights |
| Max global score shift under ±5% | 0.0048 points | Maturity level never changes |
| Min Spearman ρ across 16 scenarios | 0.996 | Ranking highly stable |
| Top-1 action stability | 16/16 scenarios | Stable |
| Top-5 membership changes | 2/16 scenarios | Tie-breaking effect |
| Top-10 membership changes | 10/16 scenarios | Tie-breaking effect |

**Conclusion:** the pairwise matrix is consistent (CR = 0.0005), the AHP weights
are close to the referential (≤ 1.9 pp), and a ±5% move in any single domain
weight never changes the maturity level. The overall ranking is highly stable
(ρ ≥ 0.996). The current weights are kept for AIMA 2.0.0. The exact top-5/top-10
composition is sensitive to ties, so the platform should make its tie-breaking
rule explicit.

**Limitations:** the pairwise matrix is the project team's structured judgment,
not an independent external expert panel. A near-zero CR proves internal
consistency, not external validity.

---

## Task 5 — ISO/IEC 42001 Correspondence Matrix

### What it does

Maps every control in **ISO/IEC 42001:2023 Annex A** to AIMA question codes and
explicitly identifies controls that are **not covered** (gaps). The matrix is
generated from a machine-readable file and validated in CI.

### Files

- [`mohamed/iso42001/mapping.json`](../../mohamed/iso42001/mapping.json) — source of truth
- [`mohamed/iso42001/iso42001_matrix.py`](../../mohamed/iso42001/iso42001_matrix.py) — validator/generator
- [`docs/mohamed/iso42001-matrix.md`](iso42001-matrix.md) — generated matrix
- [`mohamed/iso42001/README.md`](../../mohamed/iso42001/README.md) — usage guide

### How to run

```bash
python3 mohamed/iso42001/iso42001_matrix.py          # regenerate
python3 mohamed/iso42001/iso42001_matrix.py --check  # CI: fail if stale
```

### Results

ISO/IEC 42001:2023 Annex A contains **38 controls in 9 objectives (A.2–A.10)** —
not 40. The earlier matrix was wrong: it invented two controls and used partly
incorrect titles. The corrected mapping is:

| Status | Controls | % |
|--------|----------|---|
| ✅ Fully covered | 21 | 55.3% |
| 🟡 Partially covered | 13 | 34.2% |
| ⚠️ Not covered (gaps) | 4 | 10.5% |
| **Total** | **38** | **100%** |

**Identified gaps (candidates for a future AIMA release):**

- **A.3.3** — Reporting of concerns (whistleblowing channel)
- **A.8.3** — External reporting (regulators, certification bodies, public)
- **A.9.4** — Intended use of the AI system (misuse detection)
- **A.10.4** — Customers (information and obligations passed to deployers)

The validator fails if a control is missing, duplicated, attached to the wrong
objective, linked to a non-existent AIMA code, or if a gap wrongly lists a
question.

---

## Task 6 — Rebalance Criticality Levels

### What it does

Reduces the share of `criticality: 3` questions from **24/59 (40.7%)** to
**9/59 (15.3%)** so that the **weakest-link rule** (a critical question at
level 1 caps the domain at 2.00) keeps its discriminating power. Too many
critical questions dilute the rule.

### Rubric for criticality 3

A question is `criticality: 3` **only if** its failure alone can cause:

- a direct violation of a binding regulation (AI Act, GDPR/09-08), **or**
- a critical safety or security incident with no compensating control available.

### Questions kept at criticality 3 (9 total)

| Question | Domain | Why critical |
|----------|--------|--------------|
| GOV.1.2 | GOV | AI policy: regulatory obligation under ISO 42001 + AI Act |
| GOV.4.1 | GOV | Legal tracking: direct compliance obligation |
| LLM.1.1 | LLM | Prompt injection: direct active attack vector |
| LLM.2.2 | LLM | Data disclosure: GDPR/09-08 violation risk |
| LLM.3.2 | LLM | Human validation of sensitive actions: safety-critical |
| LLM.4.1 | LLM | RAG access control: direct data breach risk |
| RISK.1.1 | RISK | Risk methodology: foundation of the entire framework |
| TRUST.2.1 | TRUST | Bias control: required for AI Act high-risk systems |
| DATA.2.1 | DATA | Personal data protection: GDPR direct requirement |

### Final distribution

| Criticality | Meaning | Questions | Share |
|-------------|---------|-----------|-------|
| 3 | Critical | 9 | 15.3% |
| 2 | Important | 46 | 78.0% |
| 1 | Normal | 4 | 6.8% |

### Validation

```bash
node scripts/referential-tool.mjs --check
# ✅ Référentiel valide : 8 domaines, 29 sous-domaines, 59 questions (criticité 1/2/3 : 4/46/9)
```

---

## Referential Changes (v1.0.0 → v2.0.0)

### `referential/referentiel-v1.json`

1. **Version:** `"1.0.0"` → **`"2.0.0"`**.
2. **New source entry:** `EU_AI_ACT` — *EU AI Act 2024/1689 (Regulation (EU) 2024/1689)*.

### Why a major version?

[`docs/12-contrats-modules.md`](../12-contrats-modules.md) requires a **major**
version when weights or scoring change. Criticality drives the weakest-link rule
and the roadmap priority, so the 15 criticality changes affect scoring and
priority calculations. The JSON **shape is unchanged**; only the version and the
`criticality` values changed.

### Questions changed (15 questions downgraded from criticality 3 → 2)

`DATA.2.2`, `DATA.3.1`, `DEV.1.2`, `DEV.2.1`, `DEV.2.2`, `DEV.3.1`,
`GOV.2.1`, `GOV.3.1`, `LLM.2.1`, `LLM.3.1`, `OPS.1.1`, `OPS.4.1`,
`PEOPLE.2.1`, `RISK.2.1`, `TRUST.3.1`

### Backward compatibility

Existing assessments remain bound to the referential version they used. A new
assessment created after this change uses 2.0.0 and may produce different domain
caps and roadmap priorities for the 15 downgraded questions. This is why the
version is major, not minor.

---

## Verification Pass — Defects Found and Fixed

This push also fixes defects found while checking the existing work. Full details
are in [`audit-changelog.md`](audit-changelog.md).

| # | Defect | Fix |
|---|--------|-----|
| 1 | ISO 42001 matrix claimed 40 controls and invented two controls | Rebuilt from the official 38-control list, with validator/generator |
| 2 | Inter-rater report used a fixed-20%-chance "kappa-like" formula (κ = 0.258) | Replaced with Fleiss' κ (0.407), linear weighted κ (0.557), bootstrap CI |
| 3 | AHP doc claimed a ±5% **percentage-point** shift and contradicted its own table | Regenerated with a relative ±5% shift and consistent results |
| 4 | `docs/03b` was stale (wrong counts/version) | Regenerated from the referential (v2.0.0) |
| 5 | `docs/03` reported wrong criticality counts and a wrong example | Corrected to 9/46/4 and fixed the LLM.3 example math |
| 6 | Referential version stayed 1.1.0 despite scoring changes | Bumped to 2.0.0 per the module contract |
| 7 | CONTRIB links pointed to `docs/mohamed/mohamed/...` | Fixed to `../../mohamed/...` |
| 8 | EU AI Act classified fraud detection as high risk | Corrected to minimal risk (not in Annex III) |
| 9 | EU AI Act wrote its report in the caller's CWD and hardcoded 1.1.0 | Writes next to the script and reads the referential version |
| 10 | Evidence assistant had a broken default referential path, imported `openai` eagerly, requested a JSON array while using JSON-object mode, and silently dropped questions | Fixed path, lazy import, object response, validation, full coverage, and added `review.py` |

---

## How to Reproduce Everything

```bash
# 0. Install Python test dependencies (only for the evidence assistant)
pip install -r mohamed/evidence-assistant/requirements.txt   # optional

# 1. Validate the referential and regenerate docs/03b
node scripts/referential-tool.mjs --check

# 2. Multi-respondent reliability
node scripts/case-study/multi-respondent.mjs

# 3. AHP weight validation and sensitivity
python3 mohamed/ahp/ahp_weights.py

# 4. ISO 42001 matrix (validate + check freshness)
python3 mohamed/iso42001/iso42001_matrix.py --check

# 5. EU AI Act demo + tests
python3 mohamed/eu-ai-act/eu_ai_act.py --demo
python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py' -v

# 6. Evidence assistant + review tests (no API key needed)
python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py' -v

# 7. Evidence assistant against a real document (requires OPENAI_API_KEY)
export OPENAI_API_KEY="sk-..."
python3 mohamed/evidence-assistant/assistant.py \
  --document <your_doc.pdf> --output suggestions.json
python3 mohamed/evidence-assistant/review.py --input suggestions.json --summary
```

---

## Limitations and Blocked Checks

- **OpenAI API calls were not executed**: the `openai` package and an API key are
  unavailable in this environment. The assistant is tested with a fake client,
  and the request/response contract is covered by unit tests.
- **Backend/Java tests were not executed**: no Java runtime and no Docker are
  available here. They are unchanged by this work.
- **The multi-respondent scenario is synthetic**: the reliability numbers
  demonstrate the method, not a real organization.
- **The AHP panel is internal**: the pairwise matrix reflects the project team's
  prioritisation, not an independent expert survey.

---

## Git Commit Message

```
fix(governance): Mohamed — correct ISO 42001 (38 controls), Fleiss kappa,
AHP sensitivity, EU AI Act fraud risk, evidence assistant review, v2.0.0
```
