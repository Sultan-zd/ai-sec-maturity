#!/usr/bin/env python3
"""
AIMA Evidence Assistant -- Auditor Review Workflow
==================================================
Author: Mohamed
Date:   November 2026

The evidence assistant only produces *suggestions*. This script lets an auditor
confirm, override or skip each suggestion before it is used in an assessment.

Usage:
    # Interactive review
    python3 review.py --input suggestions.json

    # Non-interactive smoke test: confirm HIGH-confidence suggestions, skip the rest
    python3 review.py --input suggestions.json --auto

    # Show statistics without changing anything
    python3 review.py --input suggestions.json --summary
"""

import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path

DECISIONS = {"CONFIRMED", "OVERRIDDEN", "SKIPPED"}
VALID_LEVELS = {1, 2, 3, 4, 5}


def load_payload(path: Path) -> dict:
    """Load a suggestions JSON file and validate its top-level shape."""
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or not isinstance(data.get("suggestions"), list):
        raise ValueError(
            f"{path} is not a valid suggestions file (missing 'suggestions' list)"
        )
    return data


def apply_review(suggestion: dict, decision: str, final_level=None, comment=None) -> dict:
    """Return a copy of ``suggestion`` with the auditor decision applied.

    Raises ``ValueError`` for an unknown decision, an out-of-range override, or
    an attempt to confirm a suggestion that has no level.
    """
    decision = decision.upper()
    if decision not in DECISIONS:
        raise ValueError(f"Unknown decision {decision!r}; expected one of {sorted(DECISIONS)}")

    reviewed = dict(suggestion)

    if decision == "CONFIRMED":
        suggested = suggestion.get("suggestedLevel")
        if suggested not in VALID_LEVELS:
            raise ValueError("Cannot confirm a suggestion with no valid level")
        final_level = suggested
    elif decision == "OVERRIDDEN":
        if isinstance(final_level, bool) or final_level not in VALID_LEVELS:
            raise ValueError(f"Override level must be one of {sorted(VALID_LEVELS)}")
    else:  # SKIPPED
        final_level = None

    reviewed["auditorDecision"] = decision
    reviewed["auditorFinalLevel"] = final_level
    reviewed["auditorComment"] = comment
    reviewed["reviewedAt"] = datetime.now(timezone.utc).isoformat()
    return reviewed


def summarize(suggestions: list) -> dict:
    """Count decisions across a list of reviewed suggestions."""
    summary = {
        "total": len(suggestions),
        "confirmed": 0,
        "overridden": 0,
        "skipped": 0,
        "pending": 0,
    }
    for suggestion in suggestions:
        decision = suggestion.get("auditorDecision")
        if decision == "CONFIRMED":
            summary["confirmed"] += 1
        elif decision == "OVERRIDDEN":
            summary["overridden"] += 1
        elif decision == "SKIPPED":
            summary["skipped"] += 1
        else:
            summary["pending"] += 1
    return summary


def auto_decide(suggestion: dict) -> tuple:
    """Non-interactive policy: confirm HIGH confidence, skip everything else."""
    if suggestion.get("confidence") == "HIGH" and suggestion.get("suggestedLevel") in VALID_LEVELS:
        return "CONFIRMED", None, "Auto-confirmed (HIGH confidence)"
    return "SKIPPED", None, "Auto-skipped: requires human review"


def review_all(suggestions: list, decide_fn) -> list:
    """Apply a decision function to every suggestion."""
    reviewed = []
    for suggestion in suggestions:
        decision, level, comment = decide_fn(suggestion)
        reviewed.append(apply_review(suggestion, decision, level, comment))
    return reviewed


def print_summary(summary: dict) -> None:
    print(
        f"Total: {summary['total']} | "
        f"Confirmed: {summary['confirmed']} | "
        f"Overridden: {summary['overridden']} | "
        f"Skipped: {summary['skipped']} | "
        f"Pending: {summary['pending']}"
    )


def _prompt_level() -> int:
    while True:
        raw = input("  Override level (1-5): ").strip()
        if raw.isdigit() and int(raw) in VALID_LEVELS:
            return int(raw)
        print("  Please enter a number between 1 and 5.")


def interactive_decide(suggestion: dict):
    """Ask the auditor for one decision. Returns None when the auditor quits."""
    level = suggestion.get("suggestedLevel")
    confidence = suggestion.get("confidence", "LOW")
    print(f"\n[{suggestion['questionCode']}] {suggestion.get('questionText', '')}")
    print(f"  Suggested level: {level if level is not None else 'none'} | Confidence: {confidence}")
    print(f"  Reasoning: {suggestion.get('reasoning', '')}")
    if suggestion.get("citedText"):
        print(f"  Evidence: {suggestion['citedText']}")

    default = "s" if level is None else "c"
    while True:
        choice = input(
            f"  Decision [c]onfirm/[o]verride/[s]kip/[q]uit (default {default}): "
        ).strip().lower() or default
        if choice == "q":
            return None
        if choice == "c":
            if level is None:
                print("  Cannot confirm: there is no suggested level. Override or skip.")
                continue
            comment = input("  Comment (optional): ").strip() or None
            return "CONFIRMED", None, comment
        if choice == "o":
            override = _prompt_level()
            comment = input("  Justification (optional): ").strip() or None
            return "OVERRIDDEN", override, comment
        if choice == "s":
            comment = input("  Reason for skipping (optional): ").strip() or None
            return "SKIPPED", None, comment
        print("  Please choose c, o, s or q.")


def save_payload(path: Path, original: dict, reviewed: list) -> dict:
    payload = {
        "meta": original.get("meta", {}),
        "review": {
            "reviewedAt": datetime.now(timezone.utc).isoformat(),
            "reviewer": os.getenv("USER", "unknown"),
            "summary": summarize(reviewed),
        },
        "suggestions": reviewed,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return payload


def default_output_path(input_path: Path) -> Path:
    return input_path.with_name(f"{input_path.stem}.reviewed{input_path.suffix}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Review AIMA evidence-assistant suggestions")
    parser.add_argument("--input", required=True, help="Suggestions JSON produced by assistant.py")
    parser.add_argument("--output", help="Reviewed JSON path (default: <input>.reviewed.json)")
    parser.add_argument("--auto", action="store_true",
                        help="Confirm HIGH-confidence suggestions and skip the rest")
    parser.add_argument("--summary", action="store_true",
                        help="Print statistics without reviewing")
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        print(f"ERROR: input file not found: {input_path}")
        return 1

    try:
        payload = load_payload(input_path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"ERROR: {exc}")
        return 1

    suggestions = payload["suggestions"]

    if args.summary:
        print_summary(summarize(suggestions))
        return 0

    if args.auto:
        reviewed = review_all(suggestions, auto_decide)
    else:
        reviewed = []
        for index, suggestion in enumerate(suggestions, 1):
            print(f"\n--- Suggestion {index}/{len(suggestions)} ---")
            result = interactive_decide(suggestion)
            if result is None:
                print("Review stopped by the auditor; no file written.")
                return 1
            decision, level, comment = result
            reviewed.append(apply_review(suggestion, decision, level, comment))

    output_path = Path(args.output) if args.output else default_output_path(input_path)
    saved = save_payload(output_path, payload, reviewed)
    print(f"\nReviewed suggestions saved to: {output_path}")
    print_summary(saved["review"]["summary"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
