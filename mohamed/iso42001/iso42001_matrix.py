#!/usr/bin/env python3
"""
ISO/IEC 42001:2023 Annex A -- AIMA Correspondence Matrix Generator
==================================================================
Author: Mohamed
Date:   November 2026

Reads the machine-readable mapping in ``mapping.json``, validates it against
the AIMA referential, and regenerates ``docs/mohamed/iso42001-matrix.md``.

Usage:
    python3 mohamed/iso42001/iso42001_matrix.py            # regenerate the doc
    python3 mohamed/iso42001/iso42001_matrix.py --check    # fail if doc is stale
    python3 mohamed/iso42001/iso42001_matrix.py --validate # validate only
"""

import argparse
import json
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
MAPPING_PATH = SCRIPT_DIR / "mapping.json"
REFERENTIAL_PATH = REPO_ROOT / "referential" / "referentiel-v1.json"
QUESTIONS_DIR = REPO_ROOT / "referential" / "questions"
DEFAULT_OUTPUT = REPO_ROOT / "docs" / "mohamed" / "iso42001-matrix.md"

STATUS_LABELS = {
    "full": ("✅", "Fully covered"),
    "partial": ("🟡", "Partially covered"),
    "gap": ("⚠️", "Gap"),
}

# The 38 controls of ISO/IEC 42001:2023 Annex A, grouped under 9 objectives.
EXPECTED_OBJECTIVES = {
    "A.2": 3,
    "A.3": 2,
    "A.4": 5,
    "A.5": 4,
    "A.6": 9,
    "A.7": 5,
    "A.8": 4,
    "A.9": 3,
    "A.10": 3,
}
EXPECTED_TOTAL = sum(EXPECTED_OBJECTIVES.values())  # 38


def load_referential_version() -> str:
    data = json.loads(REFERENTIAL_PATH.read_text(encoding="utf-8"))
    return data.get("version", "unknown")


def load_question_codes() -> set:
    codes = set()
    for path in sorted(QUESTIONS_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for question in data.get("questions", []):
            codes.add(question["code"])
    return codes


def load_mapping() -> dict:
    return json.loads(MAPPING_PATH.read_text(encoding="utf-8"))


def validate(mapping: dict, question_codes: set) -> list:
    """Return a list of validation errors (empty when the mapping is valid)."""
    errors = []
    objectives = mapping.get("objectives", [])

    if mapping.get("standard") != "ISO/IEC 42001:2023":
        errors.append("mapping.standard must be 'ISO/IEC 42001:2023'")
    if mapping.get("annex") != "A":
        errors.append("mapping.annex must be 'A'")

    actual_objectives = {obj.get("id"): len(obj.get("controls", [])) for obj in objectives}
    if actual_objectives != EXPECTED_OBJECTIVES:
        errors.append(
            "objective structure mismatch: expected "
            f"{EXPECTED_OBJECTIVES}, got {actual_objectives}"
        )

    seen_refs = []
    status_counts = {"full": 0, "partial": 0, "gap": 0}

    for objective in objectives:
        obj_id = objective.get("id", "?")
        if not objective.get("title"):
            errors.append(f"{obj_id}: missing title")
        for control in objective.get("controls", []):
            ref = control.get("ref", "")
            seen_refs.append(ref)
            if not ref.startswith(f"{obj_id}."):
                errors.append(f"{ref}: reference does not belong to objective {obj_id}")
            if not control.get("title"):
                errors.append(f"{ref}: missing title")

            status = control.get("status")
            if status not in STATUS_LABELS:
                errors.append(f"{ref}: invalid status {status!r}")
                continue
            status_counts[status] += 1

            questions = control.get("aima_questions", [])
            if status == "gap" and questions:
                errors.append(f"{ref}: a gap must not list AIMA questions")
            if status != "gap" and not questions:
                errors.append(f"{ref}: covered control must list at least one AIMA question")

            for code in questions:
                if code not in question_codes:
                    errors.append(f"{ref}: unknown AIMA question code {code!r}")
            if len(set(questions)) != len(questions):
                errors.append(f"{ref}: duplicate AIMA question codes")

    if len(seen_refs) != len(set(seen_refs)):
        duplicates = sorted({r for r in seen_refs if seen_refs.count(r) > 1})
        errors.append(f"duplicate control references: {duplicates}")
    if len(seen_refs) != EXPECTED_TOTAL:
        errors.append(f"expected {EXPECTED_TOTAL} controls, found {len(seen_refs)}")
    if mapping.get("total_controls") != len(seen_refs):
        errors.append(
            f"mapping.total_controls={mapping.get('total_controls')} "
            f"but {len(seen_refs)} controls are listed"
        )
    if status_counts["gap"] == 0:
        errors.append("at least one gap is expected (the matrix must be honest)")

    return errors


def render_markdown(mapping: dict, version: str, question_codes: set) -> str:
    objectives = mapping["objectives"]
    counts = {"full": 0, "partial": 0, "gap": 0}
    for objective in objectives:
        for control in objective["controls"]:
            counts[control["status"]] += 1
    total = sum(counts.values())

    def pct(value: int) -> str:
        return f"{value / total * 100:.1f}%"

    lines = [
        "# ISO/IEC 42001:2023 Annex A — Correspondence Matrix with AIMA Questions",
        "",
        "> **Author:** Mohamed  ",
        "> **Date:** November 2026  ",
        f"> **Referential version:** AIMA {version}  ",
        "> **Generated by:** `mohamed/iso42001/iso42001_matrix.py` (do not edit by hand)  ",
        "> **Purpose:** Map every ISO/IEC 42001:2023 Annex A control to AIMA question codes and identify gaps.",
        "",
        "## Coverage Legend",
        "",
        "| Symbol | Meaning |",
        "|--------|---------|",
        "| ✅ | Fully covered by one or more AIMA questions |",
        "| 🟡 | Partially covered (some aspects only) |",
        "| ⚠️ | Gap — not covered by any current AIMA question |",
        "",
        "---",
        "",
        "## Annex A Controls Mapping",
        "",
    ]

    for objective in objectives:
        lines += [
            f"### {objective['id']} — {objective['title']}",
            "",
            "| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |",
            "|---|---|---|---|",
        ]
        for control in objective["controls"]:
            symbol, label = STATUS_LABELS[control["status"]]
            questions = control["aima_questions"]
            if questions:
                question_cell = ", ".join(f"`{q}`" for q in questions)
            else:
                question_cell = "*Not covered*"
            lines.append(
                f"| {control['ref']} | {control['title']} | {symbol} {label} | {question_cell} |"
            )
        lines.append("")

    lines += [
        "---",
        "",
        "## Summary",
        "",
        "| Status | Count | % |",
        "|--------|-------|---|",
        f"| ✅ Fully covered | {counts['full']} controls | {pct(counts['full'])} |",
        f"| 🟡 Partially covered | {counts['partial']} controls | {pct(counts['partial'])} |",
        f"| ⚠️ Not covered (gaps) | {counts['gap']} controls | {pct(counts['gap'])} |",
        f"| **Total** | **{total} controls** | **100%** |",
        "",
        "## Coverage Detail",
        "",
        "The notes below explain why each control is fully covered, partially covered, or a gap.",
        "",
        "| Control | Status | Rationale |",
        "|---|---|---|",
    ]
    for objective in objectives:
        for control in objective["controls"]:
            symbol, label = STATUS_LABELS[control["status"]]
            note = control.get("note", "").replace("|", "\\|")
            lines.append(f"| {control['ref']} | {symbol} {label} | {note} |")

    gaps = [
        control
        for objective in objectives
        for control in objective["controls"]
        if control["status"] == "gap"
    ]
    lines += [
        "",
        "## Identified Gaps (AIMA v2.0 does not cover)",
        "",
        "| ISO/IEC 42001 Control | Topic | Suggested future AIMA question |",
        "|---|---|---|",
    ]
    for control in gaps:
        lines.append(
            f"| {control['ref']} | {control['title']} | {control['note']} |"
        )

    lines += [
        "",
        "> **Note:** These gaps are candidates for a future AIMA minor release. "
        "Adding them would bring Annex A coverage closer to 100%. The matrix is "
        "versioned with the referential: this edition corresponds to AIMA "
        f"{version}.",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="ISO/IEC 42001 Annex A matrix generator")
    parser.add_argument("--check", action="store_true", help="exit 1 if the committed document is stale")
    parser.add_argument("--validate", action="store_true", help="validate the mapping only")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="output Markdown path")
    args = parser.parse_args()

    mapping = load_mapping()
    question_codes = load_question_codes()
    errors = validate(mapping, question_codes)
    if errors:
        print("ISO 42001 mapping is INVALID:")
        for error in errors:
            print(f"  - {error}")
        return 1

    version = load_referential_version()
    markdown = render_markdown(mapping, version, question_codes)
    output = Path(args.output)

    print(
        f"ISO 42001 mapping valid: {EXPECTED_TOTAL} controls, "
        f"{len(mapping['objectives'])} objectives, "
        f"{sum(1 for o in mapping['objectives'] for c in o['controls'] if c['status'] == 'full')} full, "
        f"{sum(1 for o in mapping['objectives'] for c in o['controls'] if c['status'] == 'partial')} partial, "
        f"{sum(1 for o in mapping['objectives'] for c in o['controls'] if c['status'] == 'gap')} gaps"
    )

    if args.validate:
        return 0

    if args.check:
        if not output.exists():
            print(f"ERROR: {output} does not exist; run the generator.")
            return 1
        if output.read_text(encoding="utf-8") != markdown:
            print(f"ERROR: {output} is stale; run the generator.")
            return 1
        print(f"{output} is up to date.")
        return 0

    output.write_text(markdown, encoding="utf-8")
    print(f"Report written to: {output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
