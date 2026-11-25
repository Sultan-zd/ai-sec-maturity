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
pip install openai PyPDF2

# Set your OpenAI API key
export OPENAI_API_KEY="sk-..."
```

## Usage

```bash
# Analyze a PDF policy document
python3 assistant.py --document company_ai_policy.pdf

# Analyze only specific domains
python3 assistant.py --document audit_report.pdf --domains GOV LLM DATA

# Use GPT-4o for best quality
python3 assistant.py --document policy.txt --model gpt-4o

# Save results to JSON
python3 assistant.py --document policy.pdf --output suggestions.json
```

## Output

The tool produces:
1. **A Markdown report** (`evidence-suggestions.md`) with each question's suggested level, confidence, and cited text
2. **A JSON file** (if `--output` is specified) for programmatic use or import into the platform

### Confidence Levels

| Confidence | Meaning |
|------------|---------|
| 🟢 HIGH | Clear, direct evidence found in the document |
| 🟡 MEDIUM | Partial or indirect evidence found |
| 🔴 LOW | No relevant content found — auditor must answer manually |

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
Evidence Assistant analyzes it
         ↓
Suggestions JSON → imported into AIMA platform
         ↓
Auditor reviews each suggestion (confirm / override)
         ↓
Final audited assessment
```

## Privacy Note

The document content is sent to OpenAI's API. For sensitive internal documents, consider:
- Using `--model gpt-4o-mini` (cheaper, smaller context)
- Redacting confidential information before analysis
- Deploying a local LLM (replace the OpenAI client in `assistant.py`)
