# Inter-Rater Reliability Report
## Banque Atlas Digitale — Multi-Respondent Case Study

> **Author:** Mohamed  
> **Date:** November 2026  
> **Scenario:** `scripts/case-study/scenario-multi-respondents.json`  
> **Organization:** Banque Atlas Digitale (fictive)  
> **Raters:** 3 — **Questions:** 59

> ⚠️ **Important — simulated respondents.** The three respondent profiles are
> **simulated** from the Banque Atlas Digitale case study, not real employees.
> The results demonstrate the reliability *method* on a realistic questionnaire;
> they do not measure a real organization. To use this tool in production,
> replace the scenario with answers collected from real respondents.

**Key result:** Fleiss' κ = **0.407** (95% CI [0.247, 0.540]), linear weighted κ = **0.557**. Inter-rater reliability is **moderate**: the grid is usable with several
respondents, but a single respondent should not be treated as ground truth.

## 1. Context

The same AIMA questionnaire was answered independently by several people from
the same (fictive) organization:

| Respondent | Role | Perspective |
|-----------|------|-------------|
| Nadia El Amrani (RSSI) | Chief Information Security Officer | Security-focused: knows controls but may underestimate business-side governance gaps |
| Karim Moussaoui (CDO) | Chief Data Officer | Data & AI-focused: more aware of data quality and AI governance, less aware of technical security |
| Salma Benali (IT Risk Manager) | IT Risk Manager | Risk-focused: conservative assessments, well-aware of compliance gaps |

This measures **inter-rater reliability**: how consistently different
knowledgeable people assess the same organization with the same grid.

## 2. Overall reliability

| Metric | Value | Interpretation |
|--------|-------|----------------|
| Questions | 59 | |
| Raters | 3 | |
| Exact agreement (all raters give the same level) | 24 (40.7%) | Modest: role-based perspectives differ |
| Within-1 agreement (spread ≤ 1) | 56 (94.9%) | High, but partly expected on an ordinal 1–5 scale |
| Contested questions (spread ≥ 2) | 3 (5.1%) | Few real divergences |
| **Fleiss' κ (unweighted)** | **0.407** | **Moderate** |
| Fleiss' κ 95% bootstrap CI | [0.247, 0.540] | Uncertainty due to the small sample |
| **Linear weighted κ** | **0.557** | **Moderate** (ordinal proximity) |
| Weighted κ 95% bootstrap CI | [0.410, 0.669] | |
| Mean pairwise Cohen's κ | 0.422 | Pairwise view of the same data |

> **Why Fleiss' κ?** With three raters, the standard coefficient is Fleiss'
> kappa (1971), not a pairwise average. It corrects for agreement expected by
> chance using the observed marginal distribution, not a fixed 20% assumption.
> The linear weighted kappa gives partial credit for nearby levels (1 vs 2) and
> is the appropriate coefficient for an ordinal 1–5 scale.

### Pairwise Cohen's κ

| Pair | κ | Interpretation |
|---|---:|---|
| Nadia El Amrani (RSSI) ↔ Karim Moussaoui (CDO) | 0.514 | Moderate |
| Nadia El Amrani (RSSI) ↔ Salma Benali (IT Risk Manager) | 0.505 | Moderate |
| Karim Moussaoui (CDO) ↔ Salma Benali (IT Risk Manager) | 0.246 | Fair |

## 3. Agreement by domain

| Domain | Questions | Exact | Within-1 | Contested | Fleiss' κ | Linear weighted κ |
|--------|-----------|-------|----------|-----------|-----------:|------------------:|
| DATA | 6 | 2 (33.3%) | 5 (83.3%) | 1 | 0.257 | 0.467 |
| DEV | 8 | 5 (62.5%) | 8 (100%) | 0 | 0.581 | 0.714 |
| GOV | 9 | 1 (11.1%) | 7 (77.8%) | 2 | -0.034 | 0.094 |
| LLM | 8 | 4 (50%) | 8 (100%) | 0 | 0.415 | 0.489 |
| OPS | 8 | 6 (75%) | 8 (100%) | 0 | 0.746 | 0.802 |
| PEOPLE | 6 | 0 (0%) | 6 (100%) | 0 | -0.137 | 0.018 |
| RISK | 6 | 2 (33.3%) | 6 (100%) | 0 | 0.250 | 0.357 |
| TRUST | 8 | 4 (50%) | 8 (100%) | 0 | 0.536 | 0.681 |

> Per-domain kappas are indicative only: a domain with 6–9 questions has a very
> wide confidence interval. The overall coefficient is the robust result.

## 4. Most contested questions

*(Spread ≥ 2 between the highest and lowest answer)*

| Question | RSSI | CDO | IT Risk Manager | Spread |
|----------|---:|---:|---:|---:|
| DATA.1.2 | 3 | 4 | 2 | 2 |
| GOV.1.1 | 3 | 4 | 2 | 2 |
| GOV.3.1 | 2 | 3 | 1 | 2 |

## 5. Interpretation

- **Fleiss' κ = 0.407 (Moderate)** sits on the
  Fair/Moderate boundary (Landis & Koch: 0.21–0.40 Fair, 0.41–0.60 Moderate);
  the confidence interval spans both. The agreement is moderate, not strong:
  the grid is usable, but a single respondent should not be treated as ground
  truth.
- **Linear weighted κ = 0.557 (Moderate)** is higher because
  most disagreements are only ±1 level. This is the more relevant coefficient
  for an ordinal maturity scale.
- **GOV and PEOPLE have near-zero or negative kappa** in this scenario. This is
  a useful warning: governance and culture questions are interpreted very
  differently by security, data and risk profiles. They need evidence and
  multi-respondent review more than technical questions do.
- **Exact agreement is 40.7%.** It is modest: different roles
  do not see the same organization in exactly the same way.
- **Within-1 agreement is 94.9%.** This is high, but a 5-point
  ordinal scale makes within-1 agreement mechanically likely; that is why the
  kappa coefficients, not the raw percentage, are the main result.
- The main source of variability is **role-based perspective** (RSSI vs CDO vs
  Risk Manager), not random answers.

## 6. Limitations

1. **Simulated respondents.** The three profiles are constructed from the
   Banque Atlas Digitale scenario; they are not real employees. The results
   demonstrate the method, not the reliability of a real organization.
2. **Small sample.** 59 questions and 3 raters give a wide bootstrap confidence
   interval. A production study should use more raters and, ideally, several
   organizations.
3. **No test-retest.** This analysis measures agreement between raters at one
   point in time, not the stability of the same rater over time.
4. **Fictive organization.** Banque Atlas Digitale is a realistic but fictive
   bank; the answers are a scenario, not an audit.
5. **Ordinal scale.** Kappa assumes categorical labels; the weighted version
   partially addresses the ordinal nature of the 1–5 scale, but no coefficient
   is perfect.

## 7. Recommendations

| Recommendation | Rationale |
|---|---|
| Use at least 2 respondents for high-stakes assessments | Contested questions reveal knowledge gaps |
| Provide per-level examples in the questionnaire | Reduces interpretation differences |
| Require evidence attachments for criticality-3 questions | Forces explicit knowledge |
| Average scores across respondents, or take the minimum | Conservative and defensible |
| Report the kappa and its confidence interval | A raw agreement percentage overstates reliability |

## 8. Reproduce

```bash
node scripts/case-study/multi-respondent.mjs
node scripts/case-study/multi-respondent.mjs --check
```

To analyse real data, copy the scenario shape and replace `answers` with the
answers exported from the platform:

```bash
node scripts/case-study/multi-respondent.mjs --scenario my-real-data.json
```

## References

- Fleiss, J.L. (1971). Measuring nominal scale agreement among many raters.
  *Psychological Bulletin*, 76(5), 378–382.
- Landis, J.R. & Koch, G.G. (1977). The measurement of observer agreement for
  categorical data. *Biometrics*, 33(1), 159–174.
- Cohen, J. (1960). A coefficient of agreement for nominal scales.
  *Educational and Psychological Measurement*, 20(1), 37–46.
