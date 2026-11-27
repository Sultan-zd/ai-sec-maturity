#!/usr/bin/env python3
"""
AIMA Evidence Assistant
=======================
Author: Mohamed
Date:   November 2026

This tool reads a company document (policy PDF, audit report, etc.) and
uses an LLM (OpenAI GPT) to suggest an answer level for each AIMA question,
citing the exact paragraph from the document as evidence.

The auditor can then confirm or override each suggestion with ``review.py``.

Usage:
    # Set your API key
    export OPENAI_API_KEY="sk-..."

    # Run against a text document (referential is found automatically)
    python3 assistant.py --document my_policy.txt

    # Run against a PDF and save the suggestions
    python3 assistant.py --document audit_report.pdf --output suggestions.json

    # Only analyze selected domains
    python3 assistant.py --document policy.txt --domains GOV LLM

Requirements:
    pip install -r requirements.txt
"""

import os
import re
import json
import argparse
import textwrap
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
DEFAULT_REFERENTIAL_DIR = REPO_ROOT / "referential" / "questions"


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

MODEL = "gpt-4o-mini"          # Cheap and fast; use "gpt-4o" for best quality
MAX_DOC_CHARS = 12000          # Truncate document to this length for the prompt
MAX_QUESTIONS_PER_BATCH = 5    # Number of questions per API call (token limits)
VALID_CONFIDENCE = {"HIGH", "MEDIUM", "LOW"}
VALID_LEVELS = {1, 2, 3, 4, 5}


# ---------------------------------------------------------------------------
# Document loading
# ---------------------------------------------------------------------------

def load_document_text(path: Path) -> str:
    """Load text from a PDF or plain text file."""
    suffix = path.suffix.lower()

    if suffix == ".pdf":
        try:
            import PyPDF2
        except ImportError as exc:
            raise RuntimeError(
                "PyPDF2 not installed. Run: pip install -r requirements.txt"
            ) from exc
        try:
            reader = PyPDF2.PdfReader(str(path))
            pages = []
            for page in reader.pages:
                text = page.extract_text()
                if text:
                    pages.append(text)
            return "\n\n".join(pages)
        except Exception as exc:
            raise ValueError(f"Could not read PDF: {exc}") from exc

    if suffix in {".txt", ".md", ".rst"}:
        return path.read_text(encoding="utf-8")

    # Last resort: try to read it as UTF-8 text.
    try:
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        raise ValueError(f"Unsupported file format: {suffix}") from exc


# ---------------------------------------------------------------------------
# Referential loading
# ---------------------------------------------------------------------------

def load_questions(referential_dir: Path) -> list:
    """Load and validate all questions from the referential questions directory."""
    if not referential_dir.exists():
        raise FileNotFoundError(f"Referential directory not found: {referential_dir}")

    questions = []
    seen = set()
    for path in sorted(referential_dir.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            print(f"Warning: could not load {path.name}: {exc}")
            continue
        for question in data.get("questions", []):
            code = question.get("code")
            text = question.get("text")
            if not code or not text:
                print(f"Warning: skipping malformed question in {path.name}: {question!r}")
                continue
            if code in seen:
                print(f"Warning: duplicate question code {code} in {path.name}")
                continue
            seen.add(code)
            questions.append(question)
    return questions


# ---------------------------------------------------------------------------
# LLM interaction
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = """You are an AI security auditor assistant.
Your job is to analyze a company document (policy, audit report, procedure, etc.)
and suggest maturity levels for specific AI security governance questions.

For each question you receive:
1. Read the document carefully
2. Find the most relevant paragraph(s) in the document
3. Based on what the document actually says, suggest a maturity level (1 to 5)
4. Cite the exact sentence or paragraph from the document as evidence
5. Assign a confidence score: HIGH (clear evidence), MEDIUM (partial evidence), or LOW (no relevant content found)

IMPORTANT RULES:
- If you find NO relevant content in the document for a question, return level null, confidence LOW, citedText null
- Do NOT invent or assume information not present in the document
- Quote text EXACTLY as it appears in the document
- Be conservative: if in doubt between level 2 and 3, choose the lower level
- Return a JSON object with a single key "suggestions" whose value is an array

Level descriptions (apply to ALL questions):
1 = Absent or improvised practices
2 = Informal practices exist but are inconsistent
3 = Documented, approved, communicated, applied across the whole scope
4 = Measured with KPIs, controlled, audited
5 = Continuously improved based on data and feedback
"""


def build_question_prompt(document_text: str, questions: list) -> str:
    """Build a prompt for a batch of questions."""
    doc_excerpt = document_text[:MAX_DOC_CHARS]
    if len(document_text) > MAX_DOC_CHARS:
        doc_excerpt += "\n\n[Document truncated...]"

    questions_text = ""
    for index, question in enumerate(questions, 1):
        questions_text += f"\n{index}. [{question['code']}] {question['text']}\n"
        questions_text += f"   Evidence expected: {question.get('evidence', 'n/a')}\n"
        questions_text += (
            f"   Criticality: {question.get('criticality', 'n/a')} | "
            f"Effort: {question.get('effort', 'n/a')}\n"
        )

    return f"""DOCUMENT TO ANALYZE:
---
{doc_excerpt}
---

QUESTIONS (answer each one):
{questions_text}

Return a JSON object with this exact shape:
{{
  "suggestions": [
    {{
      "questionCode": "...",
      "suggestedLevel": 1-5 or null,
      "confidence": "HIGH" | "MEDIUM" | "LOW",
      "citedText": "exact quote from document or null",
      "reasoning": "1-2 sentence explanation"
    }}
  ]
}}

Return ONLY the JSON object, no other text."""


def create_client(api_key: str):
    """Create an OpenAI client, importing the SDK only when actually needed."""
    try:
        import openai
    except ImportError as exc:
        raise RuntimeError(
            "The 'openai' package is not installed. "
            "Run: pip install -r mohamed/evidence-assistant/requirements.txt"
        ) from exc
    return openai.OpenAI(api_key=api_key)


def parse_llm_response(content: str) -> list:
    """Parse an LLM response into a list of raw suggestion objects.

    Accepts a JSON object (``{"suggestions": [...]}``), a bare JSON array, or
    the same wrapped in Markdown code fences. Returns ``[]`` when the response
    cannot be parsed.
    """
    if not content or not content.strip():
        return []

    text = content.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)

    parsed = None
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        # Fall back to the first JSON object/array embedded in the response.
        match = re.search(r"\{.*\}|\[.*\]", text, re.DOTALL)
        if match:
            try:
                parsed = json.loads(match.group(0))
            except json.JSONDecodeError:
                return []

    if isinstance(parsed, dict):
        suggestions = parsed.get("suggestions")
        if isinstance(suggestions, list):
            return suggestions
        for value in parsed.values():
            if isinstance(value, list):
                return value
        return []
    if isinstance(parsed, list):
        return parsed
    return []


def _coerce_level(value):
    """Return an int in 1..5, or None for an invalid level."""
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return value if value in VALID_LEVELS else None
    if isinstance(value, float) and value.is_integer():
        return int(value) if int(value) in VALID_LEVELS else None
    if isinstance(value, str):
        stripped = value.strip()
        if stripped.isdigit() and int(stripped) in VALID_LEVELS:
            return int(stripped)
    return None


def normalize_suggestion(raw: dict, allowed_codes: set) -> dict | None:
    """Validate and normalize one raw LLM suggestion.

    Returns ``None`` when the suggestion is unusable (unknown code, not a dict).
    Invalid levels/confidences are downgraded to ``null``/``LOW`` instead of
    silently producing an out-of-range maturity score.
    """
    if not isinstance(raw, dict):
        return None
    code = raw.get("questionCode")
    if not isinstance(code, str) or code not in allowed_codes:
        return None

    level = _coerce_level(raw.get("suggestedLevel"))
    confidence = raw.get("confidence", "LOW")
    confidence = confidence.strip().upper() if isinstance(confidence, str) else "LOW"
    if confidence not in VALID_CONFIDENCE or level is None:
        confidence = "LOW"

    cited = raw.get("citedText")
    cited = cited.strip() if isinstance(cited, str) and cited.strip() else None

    reasoning = raw.get("reasoning")
    reasoning = reasoning.strip() if isinstance(reasoning, str) else ""
    if not reasoning:
        reasoning = (
            "No relevant evidence found in the document."
            if level is None
            else "The model did not provide a justification."
        )

    return {
        "questionCode": code,
        "suggestedLevel": level,
        "confidence": confidence,
        "citedText": cited,
        "reasoning": reasoning,
    }


def analyze_batch(client, document_text: str, questions: list) -> list:
    """Send a batch of questions to the LLM and return normalized suggestions."""
    prompt = build_question_prompt(document_text, questions)

    response = client.chat.completions.create(
        model=MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        temperature=0.1,  # Low temperature for consistent, factual answers
        response_format={"type": "json_object"},
    )
    content = response.choices[0].message.content
    raw_items = parse_llm_response(content)

    allowed_codes = {question["code"] for question in questions}
    normalized = []
    seen = set()
    for raw in raw_items:
        suggestion = normalize_suggestion(raw, allowed_codes)
        if suggestion is None:
            print(f"  Warning: ignoring invalid suggestion: {raw!r}")
            continue
        if suggestion["questionCode"] in seen:
            print(f"  Warning: duplicate suggestion for {suggestion['questionCode']}")
            continue
        seen.add(suggestion["questionCode"])
        normalized.append(suggestion)
    return normalized


# ---------------------------------------------------------------------------
# Main analysis
# ---------------------------------------------------------------------------

def _empty_suggestion(question: dict) -> dict:
    """Build a default 'no evidence' suggestion for a question."""
    code = question["code"]
    return {
        "questionCode": code,
        "domain": code.split(".")[0],
        "subdomain": question.get("subdomain", ""),
        "questionText": question.get("text", ""),
        "criticality": question.get("criticality"),
        "effort": question.get("effort"),
        "suggestedLevel": None,
        "confidence": "LOW",
        "citedText": None,
        "reasoning": "No relevant evidence found in the document.",
        "auditorDecision": None,
        "auditorFinalLevel": None,
        "auditorComment": None,
    }


def run_analysis(document_text: str, questions: list, client) -> dict:
    """Run the full evidence analysis across all questions.

    Every question gets exactly one suggestion entry, even when the API fails
    or the model omits it. This keeps the coverage statistics honest.
    """
    print(f"Analyzing {len(questions)} questions against the document...")
    print(f"Document length: {len(document_text):,} characters")
    print(f"Model: {MODEL}")
    print(f"Batch size: {MAX_QUESTIONS_PER_BATCH} questions/call\n")

    by_code = {question["code"]: _empty_suggestion(question) for question in questions}
    api_errors = 0
    total_batches = (len(questions) + MAX_QUESTIONS_PER_BATCH - 1) // MAX_QUESTIONS_PER_BATCH

    for start in range(0, len(questions), MAX_QUESTIONS_PER_BATCH):
        batch = questions[start:start + MAX_QUESTIONS_PER_BATCH]
        batch_num = start // MAX_QUESTIONS_PER_BATCH + 1
        print(f"  Batch {batch_num}/{total_batches}: {[q['code'] for q in batch]}")
        try:
            results = analyze_batch(client, document_text, batch)
        except Exception as exc:  # API/network error: keep going with other batches
            api_errors += 1
            print(f"  API error on batch {batch_num}: {exc}")
            continue

        for result in results:
            entry = by_code[result["questionCode"]]
            entry["suggestedLevel"] = result["suggestedLevel"]
            entry["confidence"] = result["confidence"]
            entry["citedText"] = result["citedText"]
            entry["reasoning"] = result["reasoning"]

    suggestions = sorted(by_code.values(), key=lambda item: item["questionCode"])

    total = len(suggestions)
    found = sum(1 for item in suggestions if item["suggestedLevel"] is not None)
    high_conf = sum(1 for item in suggestions if item["confidence"] == "HIGH")
    med_conf = sum(1 for item in suggestions if item["confidence"] == "MEDIUM")

    return {
        "meta": {
            "model": MODEL,
            "total_questions": total,
            "questions_with_evidence": found,
            "questions_without_evidence": total - found,
            "high_confidence": high_conf,
            "medium_confidence": med_conf,
            "low_confidence": total - high_conf - med_conf,
            "coverage_rate": f"{found / total * 100:.1f}%" if total else "0%",
            "api_errors": api_errors,
        },
        "suggestions": suggestions,
    }


def render_markdown_report(result: dict, document_name: str) -> str:
    """Render the suggestions as a readable Markdown report."""
    meta = result["meta"]
    lines = [
        "# AIMA Evidence Assistant — Suggestions Report",
        "",
        f"**Document analyzed:** `{document_name}`  ",
        f"**Model:** {meta['model']}  ",
        f"**Questions analyzed:** {meta['total_questions']}  ",
        f"**Coverage rate:** {meta['coverage_rate']} (questions where evidence was found)  ",
        f"**High confidence:** {meta['high_confidence']} | "
        f"**Medium:** {meta['medium_confidence']} | "
        f"**Low/None:** {meta['low_confidence']}",
        "",
        "> ⚠️ These are **suggestions only**. An auditor must review and confirm or override each answer.",
        "",
        "---",
        "",
        "## Suggestions by Domain",
        "",
    ]

    domains = {}
    for suggestion in result["suggestions"]:
        domains.setdefault(suggestion["domain"], []).append(suggestion)

    confidence_emoji = {"HIGH": "🟢", "MEDIUM": "🟡", "LOW": "🔴"}

    for domain, suggestions in sorted(domains.items()):
        lines.append(f"### Domain: {domain}")
        lines.append("")
        for suggestion in suggestions:
            level = suggestion["suggestedLevel"]
            confidence = suggestion.get("confidence", "LOW")
            lines += [
                f"#### {suggestion['questionCode']} "
                f"{confidence_emoji.get(confidence, '⚪')} (Confidence: {confidence})",
                f"> {suggestion['questionText']}",
                "",
            ]
            if level:
                lines += [
                    f"**Suggested Level:** {level}/5  ",
                    f"**Reasoning:** {suggestion['reasoning']}",
                    "",
                ]
                if suggestion.get("citedText"):
                    lines += [
                        "**Evidence cited:**",
                        f"> _{suggestion['citedText']}_",
                        "",
                    ]
            else:
                lines.append("**No relevant evidence found in document.**\n")

            lines.append(
                "**Auditor decision:** ☐ Confirm | ☐ Override (level: ___ ) | ☐ Skip"
            )
            lines.append("")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> int:
    global MODEL

    parser = argparse.ArgumentParser(
        description="AIMA Evidence Assistant — Mohamed's module",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""\
            Examples:
              python3 assistant.py --document policy.pdf
              python3 assistant.py --document audit.txt --domains GOV LLM
              python3 assistant.py --document policy.pdf --output suggestions.json
        """),
    )
    parser.add_argument("--document", required=True,
                        help="Path to the document to analyze (PDF or text)")
    parser.add_argument("--referential", default=str(DEFAULT_REFERENTIAL_DIR),
                        help="Path to referential questions directory "
                             f"(default: {DEFAULT_REFERENTIAL_DIR})")
    parser.add_argument("--output", help="Output JSON file for suggestions")
    parser.add_argument("--domains", nargs="*",
                        help="Only analyze specific domains (e.g. --domains GOV LLM)")
    parser.add_argument("--model", default=MODEL,
                        help=f"OpenAI model (default: {MODEL})")
    args = parser.parse_args()

    MODEL = args.model

    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("ERROR: OPENAI_API_KEY environment variable not set.")
        print("Run: export OPENAI_API_KEY='sk-...'")
        return 1

    doc_path = Path(args.document)
    if not doc_path.exists():
        print(f"ERROR: Document not found: {doc_path}")
        return 1

    print(f"Loading document: {doc_path.name}")
    try:
        document_text = load_document_text(doc_path)
    except (OSError, ValueError, RuntimeError) as exc:
        print(f"ERROR: {exc}")
        return 1
    print(f"Document loaded: {len(document_text):,} characters")

    try:
        questions = load_questions(Path(args.referential))
    except FileNotFoundError as exc:
        print(f"ERROR: {exc}")
        return 1

    if args.domains:
        questions = [
            question for question in questions
            if question["code"].split(".")[0] in args.domains
        ]
        print(f"Filtered to domains {args.domains}: {len(questions)} questions")
    else:
        print(f"Loaded {len(questions)} questions")

    if not questions:
        print("No questions to analyze.")
        return 1

    try:
        client = create_client(api_key)
    except RuntimeError as exc:
        print(f"ERROR: {exc}")
        return 1

    result = run_analysis(document_text, questions, client)

    report_md = render_markdown_report(result, doc_path.name)
    print("\n" + "=" * 70)
    print(report_md)

    if args.output:
        out_path = Path(args.output)
        out_path.write_text(
            json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(f"\n[JSON saved to {out_path}]")

        md_path = out_path.with_suffix(".md")
        md_path.write_text(report_md, encoding="utf-8")
        print(f"[Markdown saved to {md_path}]")
        print(f"[Review with: python3 {SCRIPT_DIR / 'review.py'} --input {out_path}]")
    else:
        print("\n[No file written. Use --output suggestions.json to save and review.]")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
