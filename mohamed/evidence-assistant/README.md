# Evidence Assistant — AIMA Module

**Author:** Mohamed  
**Date:** November 2026

## Purpose

This tool reads a company document (security policy, audit report, ISO 27001 certificate, etc.) and uses an **OpenAI LLM** to automatically suggest maturity level answers for each AIMA question, with the **exact paragraph from the document cited as evidence**.

The auditor can then confirm or override each suggestion directly in the platform.

## Why This Matters

Today, completing the AIMA questionnaire requires a human to manually read all company policies and map them to 59 questions. This tool:

- **Cuts assessment time** by pre-filling answers with document evidence
- **Increases evidence confidence** — every suggested answer is backed by a cited paragraph
- **Reduces bias** — the LLM reads the document objectively, without the organization's self-reporting optimism

## Setup

```bash
# Install dependencies
pip install -r requirements.txt

# Set your OpenAI API key
export OPENAI_API_KEY="sk-..."
```

The referential is loaded from `../../referential/questions` **relative to this
script**, so the tool works from any working directory.

## Usage

```bash
# Analyze a PDF policy document (all 59 questions)
python3 assistant.py --document company_ai_policy.pdf

# Analyze only specific domains
python3 assistant.py --document audit_report.pdf --domains GOV LLM DATA

# Use GPT-4o for best quality
python3 assistant.py --document policy.txt --model gpt-4o

# Save results to JSON (also writes a Markdown report)
python3 assistant.py --document policy.pdf --output suggestions.json
```

## Auditor Review Workflow

The assistant only produces **suggestions**. Use `review.py` to record the
auditor's decision before importing anything into the platform:

```bash
# Interactive: confirm / override / skip each suggestion
python3 review.py --input suggestions.json

# Non-interactive smoke test: confirm HIGH confidence, skip the rest
python3 review.py --input suggestions.json --auto

# Statistics only
python3 review.py --input suggestions.json --summary
```

The reviewed file (`suggestions.reviewed.json` by default) adds
`auditorDecision` (`CONFIRMED` / `OVERRIDDEN` / `SKIPPED`),
`auditorFinalLevel`, `auditorComment` and a review summary.

## Output

- Without `--output`, the report is printed to the terminal and **no file is
  written** (no stray files in the repository).
- With `--output suggestions.json`, the tool writes both the JSON file and a
  Markdown report (`suggestions.md`).
- Every question gets exactly one entry, even when the API fails or the model
  omits it. Missing evidence is reported as `suggestedLevel: null` /
  `confidence: LOW`, so the coverage rate stays honest.

### Confidence Levels

| Confidence | Meaning |
|------------|---------|
| 🟢 HIGH | Clear, direct evidence found in the document |
| 🟡 MEDIUM | Partial or indirect evidence found |
| 🔴 LOW | No relevant content found — auditor must answer manually |

## Reliability Safeguards

- The OpenAI SDK is imported **lazily**: `--help` and document/referential
  loading work without the package installed.
- The model is asked for a JSON **object** (`{"suggestions": [...]}`), matching
  OpenAI's `response_format={"type": "json_object"}` contract.
- Responses are parsed defensively (object, bare array, Markdown fences,
  embedded JSON) and every suggestion is validated: unknown question codes are
  dropped, levels outside 1–5 become `null`, and invalid confidence values
  become `LOW`.
- Duplicate suggestions and duplicate referential codes are ignored with a
  warning.

## Tests

```bash
python3 -m unittest discover -s mohamed/evidence-assistant -p 'test_*.py' -v
```

The tests use a fake OpenAI client: no API key and no network access are
required.

## Example Output (excerpt)

```
#### GOV.1.2 🟢 (Confidence: HIGH)
> Existe-t-il une politique IA approuvée et diffusée ?

**Suggested Level:** 3/5
**Reasoning:** Document is a formally approved AI policy communicated to all staff.

**Evidence cited:**
> _"La présente politique IA a été approuvée par le Directeur Général le 15 octobre 2026
> et est applicable à l'ensemble des collaborateurs de la banque."_
```

## Workflow Integration

```
Company uploads document
         ↓
Evidence Assistant analyzes it (suggestions JSON + Markdown)
         ↓
review.py records auditor decisions (confirm / override / skip)
         ↓
Reviewed JSON → imported into AIMA platform
         ↓
Final audited assessment
```

## Privacy Note

The document content is sent to OpenAI's API. For sensitive internal documents, consider:
- Using `--model gpt-4o-mini` (cheaper, smaller context)
- Redacting confidential information before analysis
- Deploying a local LLM (replace the OpenAI client in `assistant.py`)
