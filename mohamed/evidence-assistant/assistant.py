#!/usr/bin/env python3
"""
AIMA Evidence Assistant
=======================
Author: Mohamed
Date:   November 2026

This tool reads a company document (policy PDF, audit report, etc.) and
uses an LLM (OpenAI GPT) to suggest an answer level for each AIMA question,
citing the exact paragraph from the document as evidence.

The auditor can then confirm or override each suggestion in the platform.

Usage:
    # Set your API key
    export OPENAI_API_KEY="sk-..."

    # Run against a text document
    python3 assistant.py --document my_policy.txt --referential ../../referential/questions

    # Run against a PDF
    python3 assistant.py --document audit_report.pdf --referential ../../referential/questions

    # Save output to JSON
    python3 assistant.py --document policy.txt --output suggestions.json

Requirements:
    pip install openai PyPDF2
"""

import os
import json
import argparse
import textwrap
from pathlib import Path

try:
    import openai
except ImportError:
    print("ERROR: openai package not installed. Run: pip install openai")
    exit(1)


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

MODEL = "gpt-4o-mini"          # Cheap and fast; use "gpt-4o" for best quality
MAX_DOC_CHARS = 12000          # Truncate document to this length for the prompt
MAX_QUESTIONS_PER_BATCH = 5    # Number of questions per API call (to stay under token limits)


# ---------------------------------------------------------------------------
# Document loading
# ---------------------------------------------------------------------------

def load_document_text(path: Path) -> str:
    """Load text from a PDF or plain text file."""
    suffix = path.suffix.lower()

    if suffix == ".pdf":
        try:
            import PyPDF2
            reader = PyPDF2.PdfReader(str(path))
            pages = []
            for page in reader.pages:
                text = page.extract_text()
                if text:
                    pages.append(text)
            return "\n\n".join(pages)
        except ImportError:
            raise ImportError("PyPDF2 not installed. Run: pip install PyPDF2")
        except Exception as e:
            raise ValueError(f"Could not read PDF: {e}")

    elif suffix in {".txt", ".md", ".rst"}:
        return path.read_text(encoding="utf-8")

    else:
        # Try as plain text
        try:
            return path.read_text(encoding="utf-8")
        except Exception:
            raise ValueError(f"Unsupported file format: {suffix}")


# ---------------------------------------------------------------------------
# Referential loading
# ---------------------------------------------------------------------------

def load_questions(referential_dir: Path) -> list:
    """Load all questions from the referential questions directory."""
    questions = []
    for f in sorted(referential_dir.glob("*.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            questions.extend(data.get("questions", []))
        except Exception as e:
            print(f"Warning: could not load {f.name}: {e}")
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
5. Assign a confidence score: HIGH (you found clear evidence), MEDIUM (partial evidence), or LOW (no relevant content found)

IMPORTANT RULES:
- If you find NO relevant content in the document for a question, return level null, confidence LOW, cited_text null
- Do NOT invent or assume information not present in the document
- Quote text EXACTLY as it appears in the document
- Be conservative: if in doubt between level 2 and 3, choose the lower level
- Return your answers as a JSON array

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
    for i, q in enumerate(questions, 1):
        questions_text += f"\n{i}. [{q['code']}] {q['text']}\n"
        questions_text += f"   Evidence expected: {q['evidence']}\n"
        questions_text += f"   Criticality: {q['criticality']} | Effort: {q['effort']}\n"

    return f"""DOCUMENT TO ANALYZE:
---
{doc_excerpt}
---

QUESTIONS (answer each one):
{questions_text}

Return a JSON array where each element has:
{{
  "questionCode": "...",
  "suggestedLevel": 1-5 or null,
  "confidence": "HIGH" | "MEDIUM" | "LOW",
  "citedText": "exact quote from document or null",
  "reasoning": "1-2 sentence explanation"
}}

Return ONLY the JSON array, no other text."""


def analyze_batch(client, document_text: str, questions: list) -> list:
    """Send a batch of questions to the LLM and parse the response."""
    prompt = build_question_prompt(document_text, questions)

    try:
        response = client.chat.completions.create(
            model=MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            temperature=0.1,     # Low temperature for consistent, factual answers
            response_format={"type": "json_object"} if "gpt-4" in MODEL else None,
        )
        content = response.choices[0].message.content.strip()

        # Parse JSON (handle both array and object wrapping)
        try:
            parsed = json.loads(content)
            if isinstance(parsed, dict):
                # Sometimes GPT wraps in {"suggestions": [...]}
                for key in parsed:
                    if isinstance(parsed[key], list):
                        parsed = parsed[key]
                        break
            if isinstance(parsed, list):
                return parsed
        except json.JSONDecodeError:
            # Try to extract JSON array from the response
            import re
            match = re.search(r'\[.*\]', content, re.DOTALL)
            if match:
                return json.loads(match.group(0))

        print(f"  Warning: could not parse LLM response as JSON for this batch.")
        return []

    except Exception as e:
        print(f"  API error: {e}")
        return []


# ---------------------------------------------------------------------------
# Main analysis
# ---------------------------------------------------------------------------

def run_analysis(document_text: str, questions: list, api_key: str) -> dict:
    """Run the full evidence analysis across all questions."""
    client = openai.OpenAI(api_key=api_key)

    print(f"Analyzing {len(questions)} questions against the document...")
    print(f"Document length: {len(document_text):,} characters")
    print(f"Model: {MODEL}")
    print(f"Batch size: {MAX_QUESTIONS_PER_BATCH} questions/call\n")

    all_suggestions = []

    # Process in batches
    for i in range(0, len(questions), MAX_QUESTIONS_PER_BATCH):
        batch = questions[i:i + MAX_QUESTIONS_PER_BATCH]
        batch_num = i // MAX_QUESTIONS_PER_BATCH + 1
        total_batches = (len(questions) + MAX_QUESTIONS_PER_BATCH - 1) // MAX_QUESTIONS_PER_BATCH

        print(f"  Batch {batch_num}/{total_batches}: {[q['code'] for q in batch]}")
        results = analyze_batch(client, document_text, batch)

        # Merge with question metadata
        code_to_q = {q["code"]: q for q in batch}
        for r in results:
            code = r.get("questionCode")
            if code and code in code_to_q:
                q = code_to_q[code]
                all_suggestions.append({
                    "questionCode": code,
                    "domain": code.split(".")[0],
                    "subdomain": q.get("subdomain", ""),
                    "questionText": q.get("text", ""),
                    "criticality": q.get("criticality"),
                    "effort": q.get("effort"),
                    "suggestedLevel": r.get("suggestedLevel"),
                    "confidence": r.get("confidence", "LOW"),
                    "citedText": r.get("citedText"),
                    "reasoning": r.get("reasoning", ""),
                    "auditorDecision": None,       # To be filled by the auditor
                    "auditorComment": None,
                })

    # Sort by question code
    all_suggestions.sort(key=lambda x: x["questionCode"])

    # Statistics
    total = len(all_suggestions)
    found = sum(1 for s in all_suggestions if s["suggestedLevel"] is not None)
    high_conf = sum(1 for s in all_suggestions if s["confidence"] == "HIGH")
    med_conf = sum(1 for s in all_suggestions if s["confidence"] == "MEDIUM")

    return {
        "meta": {
            "model": MODEL,
            "total_questions": total,
            "questions_with_evidence": found,
            "questions_without_evidence": total - found,
            "high_confidence": high_conf,
            "medium_confidence": med_conf,
            "low_confidence": total - high_conf - med_conf,
            "coverage_rate": f"{found/total*100:.1f}%" if total > 0 else "0%",
        },
        "suggestions": all_suggestions,
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

    # Group by domain
    domains = {}
    for s in result["suggestions"]:
        d = s["domain"]
        domains.setdefault(d, []).append(s)

    conf_emoji = {"HIGH": "🟢", "MEDIUM": "🟡", "LOW": "🔴"}

    for domain, suggestions in sorted(domains.items()):
        lines.append(f"### Domain: {domain}")
        lines.append("")
        for s in suggestions:
            level = s["suggestedLevel"]
            conf = s.get("confidence", "LOW")
            lines += [
                f"#### {s['questionCode']} {conf_emoji.get(conf, '⚪')} (Confidence: {conf})",
                f"> {s['questionText']}",
                "",
            ]
            if level:
                lines += [
                    f"**Suggested Level:** {level}/5  ",
                    f"**Reasoning:** {s['reasoning']}",
                    "",
                ]
                if s.get("citedText"):
                    lines += [
                        "**Evidence cited:**",
                        f"> _{s['citedText']}_",
                        "",
                    ]
            else:
                lines.append("**No relevant evidence found in document.**\n")

            lines.append("**Auditor decision:** ☐ Confirm | ☐ Override (level: ___ ) | ☐ Skip")
            lines.append("")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="AIMA Evidence Assistant \u2014 Mohamed's module",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""\
            Examples:
              python3 assistant.py --document policy.pdf --referential ../../referential/questions
              python3 assistant.py --document audit.txt --output suggestions.json
        """)
    )
    parser.add_argument("--document", required=True, help="Path to the document to analyze (PDF or text)")
    parser.add_argument("--referential", default="../../referential/questions",
                        help="Path to referential questions directory (default: ../../referential/questions)")
    parser.add_argument("--output", help="Output JSON file for suggestions")
    parser.add_argument("--domains", nargs="*",
                        help="Only analyze specific domains (e.g. --domains GOV LLM)")
    parser.add_argument("--model", default=MODEL, help=f"OpenAI model (default: {MODEL})")
    args = parser.parse_args()

    global MODEL
    MODEL = args.model

    # API key
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("ERROR: OPENAI_API_KEY environment variable not set.")
        print("Run: export OPENAI_API_KEY='sk-...'")
        exit(1)

    # Load document
    doc_path = Path(args.document)
    if not doc_path.exists():
        print(f"ERROR: Document not found: {doc_path}")
        exit(1)

    print(f"Loading document: {doc_path.name}")
    document_text = load_document_text(doc_path)
    print(f"Document loaded: {len(document_text):,} characters")

    # Load questions
    ref_dir = Path(args.referential)
    if not ref_dir.exists():
        print(f"ERROR: Referential directory not found: {ref_dir}")
        exit(1)

    questions = load_questions(ref_dir)
    if args.domains:
        questions = [q for q in questions if q["code"].split(".")[0] in args.domains]
        print(f"Filtered to domains {args.domains}: {len(questions)} questions")
    else:
        print(f"Loaded {len(questions)} questions")

    if not questions:
        print("No questions to analyze.")
        exit(1)

    # Run analysis
    result = run_analysis(document_text, questions, api_key)

    # Print markdown report
    report_md = render_markdown_report(result, doc_path.name)
    print("\n" + "=" * 70)
    print(report_md)

    # Save JSON output
    if args.output:
        out_path = Path(args.output)
        out_path.write_text(json.dumps(result, ensure_ascii=False, indent=2))
        print(f"\n[JSON saved to {out_path}]")

        md_path = out_path.with_suffix(".md")
        md_path.write_text(report_md)
        print(f"[Markdown saved to {md_path}]")
    else:
        # Save markdown beside the script
        md_path = Path("evidence-suggestions.md")
        md_path.write_text(report_md)
        print(f"[Markdown saved to {md_path}]")


if __name__ == "__main__":
    main()
