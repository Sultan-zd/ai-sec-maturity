# Audit and Change Log — Mohamed's Governance Deliverables

> **Author:** Mohamed  
> **Date:** 2026-11-26  
> **Scope:** verification pass on the governance/evidence/case-study work  
> **Base commit:** `086a581` (`origin/main`)  
> **Referential:** 1.1.0 → **2.0.0**  
> **Companion document:** [`CONTRIB.md`](CONTRIB.md)

This log records **what was checked, what was wrong, what was changed, and how it
was verified**. It is intentionally more detailed than `CONTRIB.md`: the goal is
that another team member can reproduce the audit and understand every decision.

---

## 1. Method

1. Re-read the task split PDF and listed Mohamed's six deliverables.
2. Audited the repository at commit `086a581`:
   - file inventory and generated artifacts;
   - referential version and criticality distribution;
   - ISO/IEC 42001 Annex A control list;
   - AHP script, generated report and claimed numbers;
   - multi-respondent script and reliability formula;
   - EU AI Act classifier and demo output;
   - evidence assistant paths, imports and JSON handling;
   - documentation links and counts.
3. Compared every claim in the documentation with the actual data or the
   authoritative standard.
4. Fixed the defects, regenerated generated artifacts, and added tests where the
   defect was code-level.
5. Re-ran every available check (see § 6).

The pass was **read-only** on the backend, frontend, and database: no Java,
Docker or live platform was required.

---

## 2. Initial state

```
$ git status --short
(clean)
$ git log --oneline -1
086a581 feat(governance): Mohamed — EU AI Act mode, evidence assistant,
multi-respondent case study, AHP weight validation, ISO 42001 matrix,
criticality rebalancing
```

The working tree was clean and `086a581` was already on `origin/main`. All
changes below are therefore **new fixes on top of the pushed commit**, not
rewrites of unpublished work.

---

## 3. Defects found

### 3.1 ISO/IEC 42001 matrix used 40 controls instead of 38

**Where:** `docs/mohamed/iso42001-matrix.md`

**Finding:** ISO/IEC 42001:2023 Annex A contains **38 controls in 9 objectives**
(A.2–A.10). The published matrix claimed **40 controls** and listed controls that
do not exist in the standard (for example a separate “conflict of interest”
control under A.3 and an “infrastructure” control under A.4). Several titles were
also paraphrased incorrectly.

**Impact:** the matrix overstated coverage and could not be defended in a
certification discussion.

**Fix:**

- created `mohamed/iso42001/mapping.json` with the exact 38-control list;
- created `mohamed/iso42001/iso42001_matrix.py`, which validates the structure
  (9 objectives, 38 controls, correct objective prefixes, existing AIMA codes)
  and regenerates the Markdown;
- corrected the mapping to **21 full / 13 partial / 4 gaps**;
- documented the four real gaps: **A.3.3** (reporting of concerns), **A.8.3**
  (external reporting), **A.9.4** (intended use), **A.10.4** (customers).

**Verification:**

```
$ python3 mohamed/iso42001/iso42001_matrix.py --check
ISO 42001 mapping valid: 38 controls, 9 objectives, 21 full, 13 partial, 4 gaps
docs/mohamed/iso42001-matrix.md is up to date.
```

### 3.2 Inter-rater reliability used an invalid “kappa-like” formula

**Where:** `scripts/case-study/multi-respondent.mjs`,
`docs/mohamed/inter-rater-reliability.md`

**Finding:** the old script computed a coefficient that assumed a **fixed 20%**
chance agreement (five levels) and reported **κ = 0.258 (Fair)**. That is not
Fleiss' kappa: chance agreement must be estimated from the observed marginal
distribution of the raters. With three raters and an ordinal 1–5 scale, the
standard coefficients are Fleiss' κ and a linear weighted κ.

**Impact:** the headline reliability number was wrong, and the “Fair” label
understated the actual agreement.

**Fix:**

- rewrote `multi-respondent.mjs` to compute:
  - observed agreement and within-1 agreement;
  - **Fleiss' κ** with the correct expected-agreement term;
  - **linear weighted κ** for the ordinal scale;
  - **bootstrap 95% confidence intervals** (2,000 resamples, fixed seed);
  - mean pairwise Cohen's κ as a cross-check;
- labelled the respondents explicitly as **simulated** in the generated report;
- regenerated `docs/mohamed/inter-rater-reliability.md`.

**Corrected results:**

| Metric | Old (invalid) | New (correct) |
|--------|---------------|---------------|
| Fleiss' κ | 0.258 | **0.407** (95% CI [0.247, 0.540]) |
| Linear weighted κ | not computed | **0.557** (95% CI [0.410, 0.669]) |
| Mean pairwise Cohen's κ | not computed | 0.422 |
| Exact agreement | 40.7% | 40.7% (unchanged) |
| Within-1 agreement | 94.9% | 94.9% (unchanged) |

**Verification:** rerunning the script reproduces the same numbers; the report
states that the three respondents are simulated.

### 3.3 AHP sensitivity analysis contradicted itself

**Where:** `mohamed/ahp/ahp_weights.py`,
`docs/mohamed/ahp-sensitivity.md`

**Finding:** the documentation claimed a **±5 percentage-point** shift in one
place and a **±5% relative** shift in another. The table’s “new weight” column
was consistent with a relative shift (15% → 15.75%, displayed as 15.8%), but the
narrative described percentage points (15% → 20%). The report also claimed
“max score shift < 0.05” without showing the actual maximum, and the script did
not generate the document.

**Impact:** the sensitivity conclusion was not reproducible and the method was
ambiguous.

**Fix:**

- rewrote `ahp_weights.py` so the document is **generated** by the script;
- implemented a **relative ±5%** shift (15% → 15.75% / 14.25%), with the
  difference redistributed proportionally over the other domains;
- computed the global score, maturity level, Spearman ρ, top-1/top-5/top-10
  membership for all 16 scenarios;
- added explicit limitations (single expert source, matrix reflects current
  weights, case study data from v1.0.0, ties).

**Corrected results:**

| Metric | Value |
|--------|-------|
| λ_max | 8.0052 |
| CI | 0.0007 |
| CR | **0.0005** (< 0.10) |
| Max AHP-vs-current weight difference | 1.9 percentage points |
| Max global score shift under ±5% | **0.0048** points |
| Maturity level changes | **0 / 16** |
| Min Spearman ρ | **0.996** |
| Top-1 stability | **16 / 16** |
| Top-5 membership changes | 2 / 16 |
| Top-10 membership changes | 10 / 16 (tie-breaking) |

**Verification:** `python3 mohamed/ahp/ahp_weights.py` regenerates the report;
`git diff` after a second run is empty.

### 3.4 `docs/03b` was stale and `docs/03` had wrong counts

**Where:** `docs/03-grille-audit.md`, `docs/03b-grille-audit-detaillee.md`

**Finding:**

- `docs/03b` still described the referential as v1.1.0 and used the old
  criticality counts.
- `docs/03` stated the wrong number of critical questions and contained an
  arithmetic error in the LLM.3 scoring example.

**Fix:**

- regenerated `docs/03b` with `node scripts/referential-tool.mjs` (now v2.0.0);
- corrected `docs/03` to **9 critical / 46 important / 4 normal**;
- fixed the LLM.3 example math.

**Verification:** the counts in `docs/03`, `docs/03b` and the referential now
agree (4/46/9 for criticality 1/2/3).

### 3.5 Referential version violated the module contract

**Where:** `referential/referentiel-v1.json`

**Finding:** the criticality rebalancing changes the weakest-link caps and the
roadmap priorities, but the version remained **1.1.0**. `docs/12-contrats-modules.md`
requires a **major** version when weights or scoring change.

**Fix:** bumped the version to **2.0.0**. The JSON **shape is unchanged**; only
the version and 15 `criticality` values changed.

**Verification:**

```
$ node scripts/referential-tool.mjs --check
✅ Référentiel valide : 8 domaines, 29 sous-domaines, 59 questions
   (criticité 1/2/3 : 4/46/9)
```

### 3.6 CONTRIB links were broken

**Where:** `docs/mohamed/CONTRIB.md`

**Finding:** links such as `../mohamed/eu-ai-act/eu_ai_act.py` resolve to
`docs/mohamed/mohamed/...`, which does not exist. Several numbers were also
outdated (40 controls, κ = 0.258, v1.1.0, “±5% < 0.05”).

**Fix:** rewrote `CONTRIB.md` with correct relative links (`../../mohamed/...`),
the corrected numbers, the v2.0.0 rationale, and a link to this log.

**Verification:** every relative link was resolved with a small script; see § 6.

### 3.7 EU AI Act misclassified fraud detection

**Where:** `mohamed/eu-ai-act/eu_ai_act.py`,
`mohamed/eu-ai-act/eu-ai-act-report.md`

**Finding:** the demo forced **Fraud Detection** to high risk with an explicit
`annex_iii_category` and cited “EBA guidance”. Annex III.5b only covers
**creditworthiness / credit scoring**; Annex III.5c covers **insurance pricing**.
Transaction fraud detection is not listed in Annex III, so it is **minimal
risk** under the AI Act (GDPR still applies).

**Fix:**

- removed the explicit override and corrected the comment;
- added `test_eu_ai_act.py` (13 tests) covering the demo, the Article 5 → Annex
  III → Article 50 → minimal cascade, overrides and the AIMA mapping;
- made the default output path stable (next to the script, not the caller's
  CWD);
- replaced the hardcoded `AIMA 1.1.0` with a read of the referential version;
- regenerated the report.

**Corrected demo:**

| System | Category |
|--------|----------|
| Atlas Assistant | 🟡 Limited Risk |
| Credit Scoring Model | 🟠 High Risk (Annex III.5b) |
| Fraud Detection Model | 🟢 Minimal Risk |

**Verification:** `python3 -m unittest discover -s mohamed/eu-ai-act` → 13 tests
pass.

### 3.8 Evidence assistant had path, import, and JSON-contract bugs

**Where:** `mohamed/evidence-assistant/assistant.py`

**Finding:**

1. The default referential path was `../../referential/questions`, resolved
   against the **caller's working directory**, so the tool failed when run from
   anywhere but `mohamed/evidence-assistant`.
2. `openai` was imported at module load, so `--help` crashed when the package was
   not installed.
3. The system prompt asked for a JSON **array**, while the API call used
   `response_format={"type": "json_object"}`, which requires a JSON **object**.
   This is an inconsistent contract and can produce empty parses.
4. The code silently dropped questions when the model omitted them or when a
   batch failed, so `meta.total_questions` and the coverage rate were misleading.
5. There was no auditor review workflow, despite the README saying the auditor
   confirms/overrides suggestions.

**Fix:**

- resolve the default referential path relative to `__file__`;
- import `openai` lazily inside `create_client()`;
- request `{"suggestions": [...]}` and parse object/array/fenced/embedded JSON;
- validate every suggestion (known code, level 1–5, confidence in
  HIGH/MEDIUM/LOW, string citation), downgrade invalid values, and drop
  duplicates;
- create exactly one entry per question, including API failures, and expose
  `meta.api_errors`;
- stop writing files unless `--output` is given;
- added `review.py` (confirm/override/skip, auto mode, summary) and
  `requirements.txt`;
- added `test_assistant.py` (18 tests, fake client) and `test_review.py`
  (15 tests).

**Verification:**

```
$ python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py'
Ran 33 tests ... OK

$ python3 mohamed/evidence-assistant/assistant.py --help
(works without the openai package)
```

### 3.9 Stray generated file at the repository root

**Where:** `eu-ai-act-report.md` (untracked)

**Finding:** an earlier demo run wrote the report into the repository root.

**Fix:** deleted the stray file; the script now writes next to itself by default.

---

## 4. Files changed in this pass

### Modified

| File | Change |
|------|--------|
| `referential/referentiel-v1.json` | version 1.1.0 → 2.0.0 |
| `docs/03-grille-audit.md` | corrected counts and LLM.3 example |
| `docs/03b-grille-audit-detaillee.md` | regenerated (v2.0.0) |
| `docs/mohamed/ahp-sensitivity.md` | regenerated (relative ±5%, correct results) |
| `docs/mohamed/inter-rater-reliability.md` | regenerated (Fleiss' κ, weighted κ, CI) |
| `docs/mohamed/iso42001-matrix.md` | rebuilt (38 controls) |
| `docs/mohamed/CONTRIB.md` | rewritten (numbers, links, v2.0.0) |
| `mohamed/ahp/ahp_weights.py` | rewritten (generator + correct sensitivity) |
| `mohamed/eu-ai-act/eu_ai_act.py` | fraud risk, output path, version |
| `mohamed/eu-ai-act/eu-ai-act-report.md` | regenerated |
| `mohamed/eu-ai-act/README.md` | fraud explanation, tests, output |
| `mohamed/evidence-assistant/assistant.py` | path, lazy import, JSON object, validation, coverage |
| `mohamed/evidence-assistant/README.md` | setup, review workflow, tests |
| `scripts/case-study/multi-respondent.mjs` | Fleiss' κ, weighted κ, bootstrap CI, simulated label |
| `README.md` | new governance/evidence modules section |
| `docs/09-etude-de-cas.md` | referential version note |

### Added

| File | Purpose |
|------|---------|
| `mohamed/iso42001/mapping.json` | machine-readable 38-control mapping |
| `mohamed/iso42001/iso42001_matrix.py` | validator + generator |
| `mohamed/iso42001/README.md` | module guide |
| `mohamed/eu-ai-act/test_eu_ai_act.py` | 13 tests |
| `mohamed/evidence-assistant/review.py` | auditor review workflow |
| `mohamed/evidence-assistant/requirements.txt` | dependencies |
| `mohamed/evidence-assistant/test_assistant.py` | 18 tests |
| `mohamed/evidence-assistant/test_review.py` | 15 tests |
| `docs/mohamed/audit-changelog.md` | this log |

### Deleted

| File | Reason |
|------|--------|
| `eu-ai-act-report.md` (repo root, untracked) | stray output from a demo run |

---

## 5. Decisions and rationale

| Decision | Rationale |
|----------|-----------|
| **Major version 2.0.0** | Criticality changes affect the weakest-link cap and roadmap priority; `docs/12-contrats-modules.md` requires a major version when scoring changes. |
| **Relative ±5% AHP shift** | The usual interpretation of “±5% sensitivity on a weight”; a ±5 percentage-point shift would be a different (and much larger) experiment. |
| **Fleiss' κ + linear weighted κ** | Three raters, ordinal 1–5 scale; chance agreement must use observed marginals, not a fixed 20%. |
| **Simulated respondents** | The scenario is synthetic; the report must not imply a real organization was surveyed. |
| **38 ISO controls** | Official ISO/IEC 42001:2023 Annex A count; verified against multiple public control lists. |
| **Fraud detection = minimal risk** | Not listed in Annex III; the AI Act is purpose-based, not label-based. |
| **JSON object mode** | Matches OpenAI's `response_format={"type": "json_object"}` contract; the old array request was inconsistent. |
| **One suggestion per question** | Keeps coverage statistics honest when a batch fails. |
| **No file write without `--output`** | Avoids stray files in the repository. |

---

## 6. Verification checklist

| Check | Command | Result |
|-------|---------|--------|
| Referential schema/coherence | `node scripts/referential-tool.mjs --check` | ✅ 8 domains, 29 subdomains, 59 questions (4/46/9) |
| ISO 42001 mapping | `python3 mohamed/iso42001/iso42001_matrix.py --check` | ✅ 38 controls, 21 full / 13 partial / 4 gaps, doc up to date |
| EU AI Act tests | `python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py'` | ✅ 13 tests |
| Evidence assistant tests | `python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py'` | ✅ 33 tests |
| AHP reproducibility | `python3 mohamed/ahp/ahp_weights.py` twice | ✅ identical output |
| Multi-respondent reproducibility | `node scripts/case-study/multi-respondent.mjs` twice | ✅ identical numbers |
| Python syntax | `python3 -m py_compile ...` | ✅ all modules compile |
| Relative links in CONTRIB | script resolving every Markdown link | ✅ all exist |
| Stray files | `git status --short` | ✅ only intended changes |

### What could not be verified here

| Check | Reason |
|-------|--------|
| OpenAI API end-to-end | `openai` package and API key unavailable; covered by a fake-client test suite |
| Java/backend tests | No Java runtime and no Docker in this environment |
| Live platform case-study replay | Requires the backend/database; the static `resultats.json` was not regenerated |
| Real respondent reliability | The scenario is synthetic by design |

---

## 7. Reproduce the audit

```bash
# 1. Referential and generated grid
node scripts/referential-tool.mjs --check

# 2. ISO 42001
python3 mohamed/iso42001/iso42001_matrix.py --check

# 3. AHP
python3 mohamed/ahp/ahp_weights.py

# 4. Multi-respondent reliability
node scripts/case-study/multi-respondent.mjs

# 5. EU AI Act
python3 mohamed/eu-ai-act/eu_ai_act.py --demo
python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py' -v

# 6. Evidence assistant
python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py' -v
```

---

## 8. Commit

All changes are committed under Mohamed's identity and pushed to
`origin/main`. The commit message is:

```
fix(governance): Mohamed — correct ISO 42001 (38 controls), Fleiss kappa,
AHP sensitivity, EU AI Act fraud risk, evidence assistant review, v2.0.0
```
