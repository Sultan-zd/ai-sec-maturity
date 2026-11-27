# ISO/IEC 42001:2023 Annex A — AIMA Correspondence Matrix

This module keeps the ISO/IEC 42001 mapping **machine-readable** and generates
the published document from a single source of truth.

## Files

| File | Purpose |
|------|---------|
| `mapping.json` | The 38 Annex A controls, their status (`full`, `partial`, `gap`), linked AIMA questions and rationale |
| `iso42001_matrix.py` | Validator and Markdown generator |
| `../../docs/mohamed/iso42001-matrix.md` | Generated report (do not edit by hand) |

## Why 38 controls?

ISO/IEC 42001:2023 Annex A contains **38 controls in 9 objectives** (A.2–A.10).
The numbering starts at A.2 because A.1 introduces the annex. This module
encodes the exact control list; the validator fails if a control is missing,
duplicated, or attached to the wrong objective.

## Usage

```bash
# Validate the mapping and regenerate the Markdown report
python3 mohamed/iso42001/iso42001_matrix.py

# Validate only (no file write)
python3 mohamed/iso42001/iso42001_matrix.py --validate

# CI check: fail if the committed report is stale
python3 mohamed/iso42001/iso42001_matrix.py --check
```

## Validation rules

The script fails when:

- the objective structure differs from the official 38-control layout;
- a control reference is duplicated or attached to the wrong objective;
- a linked AIMA question code does not exist in `referential/questions/*.json`;
- a covered control has no AIMA question, or a gap lists one;
- `total_controls` does not match the number of listed controls;
- no gap is declared (a matrix without gaps would be suspicious).

## Current coverage (AIMA 2.0.0)

| Status | Controls | Share |
|--------|----------|-------|
| ✅ Fully covered | 21 | 55.3% |
| 🟡 Partially covered | 13 | 34.2% |
| ⚠️ Gaps | 4 | 10.5% |
| **Total** | **38** | **100%** |

The four gaps are **A.3.3** (reporting of concerns), **A.8.3** (external
reporting), **A.9.4** (intended use) and **A.10.4** (customers). They are
documented as candidates for a future AIMA release.
