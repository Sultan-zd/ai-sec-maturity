# EU AI Act Classification Module

**Author:** Mohamed  
**Date:** November 2026

## Purpose

This module classifies AI systems according to the **EU AI Act 2024/1689** risk categories and shows which legal obligations apply to each system.

## Risk Categories

| Category | Legal Basis | Description |
|----------|-------------|-------------|
| 🔴 Unacceptable Risk | Article 5 | Prohibited — cannot be deployed |
| 🟠 High Risk | Article 6 + Annex III | Conformity assessment + registration required |
| 🟡 Limited Risk | Article 50 | Transparency obligations (must inform users) |
| 🟢 Minimal Risk | — | No specific AI Act obligations |

## Usage

```bash
# Run with demo data (Banque Atlas Digitale)
python3 eu_ai_act.py --demo

# Run with custom systems JSON
python3 eu_ai_act.py --input my_systems.json --output report.json
```

## Input Format

```json
[
  {
    "name": "My AI System",
    "purpose": "Description of what the system does",
    "sector": "banking",
    "processes_personal_data": true,
    "has_human_interaction": true,
    "is_generative": false
  }
]
```

## Banque Atlas Results

| System | EU AI Act Category | Key Obligation |
|--------|--------------------|----------------|
| Atlas Assistant (LLM) | 🟡 Limited Risk | Inform users they interact with AI (Art. 50) |
| Credit Scoring Model | 🟠 High Risk | Annex III.5b — creditworthiness assessment |
| Fraud Detection Model | 🟢 Minimal Risk | No AI Act obligation; GDPR still applies |

The **Credit Scoring Model** is the most critical: it is a **High Risk** system under Annex III and requires a formal conformity assessment, bias testing (TRUST.2.1), and registration in the EU AI database.

**Why is fraud detection minimal risk?** Annex III.5b only covers
creditworthiness and credit scoring; Annex III.5c covers insurance pricing.
Transaction fraud detection is not listed, so it remains minimal risk under the
AI Act. A "risky-sounding" system is not automatically high-risk — the legal
basis matters, not the label.

## Tests

```bash
python3 -m unittest discover -s mohamed/eu-ai-act -p 'test_*.py' -v
```

The tests cover the demo portfolio, the Article 5 → Annex III → Article 50 →
minimal-risk cascade, explicit Annex III overrides, the AIMA question mapping,
and the referential version.

## Output

Without `--output`, the Markdown report is written next to the script
(`mohamed/eu-ai-act/eu-ai-act-report.md`), never in the caller's working
directory. With `--output report.json`, both `report.json` and `report.md` are
written at the requested location.
