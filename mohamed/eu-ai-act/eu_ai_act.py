#!/usr/bin/env python3
"""
EU AI Act Classification Mode -- AIMA Module
=============================================
Author: Mohamed
Date:   November 2026

Classifies AI systems per EU AI Act 2024/1689 risk categories:
  - UNACCEPTABLE_RISK  (Article 5 -- prohibited)
  - HIGH_RISK          (Article 6 + Annex III -- regulated)
  - LIMITED_RISK       (Article 50 -- transparency obligations)
  - MINIMAL_RISK       (All others -- no specific obligations)

Usage:
    python3 eu_ai_act.py --demo
    python3 eu_ai_act.py --input systems.json --output report.json
"""

import json
import argparse
from pathlib import Path
from datetime import datetime, timezone


# ---------------------------------------------------------------------------
# Risk category metadata
# ---------------------------------------------------------------------------

RISK_CATEGORIES = {
    "UNACCEPTABLE_RISK": {
        "label": "Unacceptable Risk",
        "article": "Article 5",
        "color": "🔴",
        "description": "Prohibited AI practices. These systems cannot be deployed in the EU.",
        "obligations": [
            "PROHIBITED: System must not be deployed or marketed in the EU.",
            "Existing systems must be decommissioned immediately.",
            "Notify the competent national supervisory authority.",
        ],
    },
    "HIGH_RISK": {
        "label": "High Risk",
        "article": "Article 6 + Annex III",
        "color": "🟠",
        "description": "Requires conformity assessment, CE marking, and EU database registration.",
        "obligations": [
            "Establish a risk management system (Article 9)",
            "Ensure data governance for training data (Article 10)",
            "Create and maintain technical documentation (Article 11)",
            "Enable automatic logging / record-keeping (Article 12)",
            "Provide transparency to deployers (Article 13)",
            "Ensure human oversight capabilities (Article 14)",
            "Meet accuracy, robustness, and cybersecurity requirements (Article 15)",
            "Conduct a conformity assessment (Article 43)",
            "Register the system in the EU AI Act database before placement (Article 49)",
            "Appoint an EU representative if provider is based outside the EU (Article 22)",
            "Conduct a Fundamental Rights Impact Assessment if deployed by a public body (Article 27)",
        ],
    },
    "LIMITED_RISK": {
        "label": "Limited Risk",
        "article": "Article 50",
        "color": "🟡",
        "description": "Transparency obligations: users must know they interact with AI.",
        "obligations": [
            "Inform users they are interacting with an AI system (Article 50.1)",
            "Disclose AI-generated content (deepfakes, synthetic media) (Article 50.2)",
            "For emotion recognition / biometric categorisation: inform subjects (Article 50.3)",
            "No registration or conformity assessment required.",
        ],
    },
    "MINIMAL_RISK": {
        "label": "Minimal Risk",
        "article": "No specific obligations",
        "color": "🟢",
        "description": "No specific AI Act obligations. Voluntary codes of conduct may apply.",
        "obligations": [
            "No mandatory obligations under the AI Act.",
            "Voluntary: consider adherence to voluntary codes of conduct (Article 95).",
            "Standard GDPR and product liability rules still apply.",
        ],
    },
}

# ---------------------------------------------------------------------------
# Annex III patterns (keyword matching on system purpose description)
# ---------------------------------------------------------------------------

ANNEX_III_PATTERNS = [
    (["biometric", "face recognition", "facial recognition", "fingerprint", "iris scan"], "Annex III.1 -- Biometric identification"),
    (["critical infrastructure", "power grid", "water supply", "transport network", "digital infrastructure"], "Annex III.2 -- Critical infrastructure"),
    (["education", "student assessment", "exam scoring", "school admission", "vocational training"], "Annex III.3 -- Education and vocational training"),
    (["recruitment", "hiring", "job applicant screening", "work monitoring", "employee dismissal"], "Annex III.4 -- Employment and workers management"),
    (["creditworthiness", "credit scoring", "solvency assessment", "insurance risk scoring"], "Annex III.5b -- Credit scoring and solvency assessment"),
    (["social security benefit", "welfare entitlement", "asylum processing", "border control migration"], "Annex III.6 -- Access to essential services"),
    (["law enforcement", "criminal investigation", "crime prediction", "judicial sentencing"], "Annex III.7 -- Law enforcement"),
    (["election integrity", "voting system", "political advertising targeting", "democratic process"], "Annex III.8 -- Democratic processes"),
]

ARTICLE_5_PATTERNS = [
    (["subliminal manipulation", "exploit psychological vulnerability", "dark pattern coercion"], "Article 5.1a -- Subliminal manipulation"),
    (["social scoring", "social credit system", "citizen trustworthiness score based on social behaviour"], "Article 5.1c -- Social scoring"),
    (["real-time biometric surveillance in public", "mass surveillance live biometric"], "Article 5.1h -- Real-time remote biometric ID in public spaces"),
    (["emotion recognition in workplace", "emotion recognition in education institution"], "Article 5.1f -- Emotion recognition in workplace/education"),
]

# AIMA question mapping by risk category
AIMA_QUESTIONS = {
    "HIGH_RISK": [
        "GOV.4.1 -- Legal/regulatory tracking (AI Act Annex III compliance)",
        "GOV.3.2 -- AI system risk classification",
        "RISK.2.1 -- Impact assessment (Fundamental Rights Impact Assessment)",
        "TRUST.2.1 -- Bias and fairness measurement (required for high-risk systems)",
        "TRUST.1.1 -- Transparency disclosure to users and deployers",
        "TRUST.3.1 -- Human oversight capabilities",
        "DEV.3.1 -- Validation and testing before deployment",
        "DATA.1.1 -- Data quality and provenance for training data",
    ],
    "LIMITED_RISK": [
        "TRUST.1.1 -- Transparency: users must be informed they interact with AI",
        "TRUST.4.1 -- Reliability and confabulation control",
    ],
    "UNACCEPTABLE_RISK": [
        "GOV.4.1 -- Legal compliance: system must be decommissioned immediately",
        "RISK.3.1 -- Risk treatment plan for prohibited system",
    ],
    "MINIMAL_RISK": [],
}


# ---------------------------------------------------------------------------
# Classification logic
# ---------------------------------------------------------------------------

def classify_system(system: dict) -> dict:
    """
    Classify a single AI system per EU AI Act risk categories.

    Input dict fields:
      - name (str): system display name
      - purpose (str): free-text description of what the system does
      - sector (str): industry sector (informational only)
      - processes_personal_data (bool)
      - has_human_interaction (bool): users interact directly with the system
      - is_generative (bool): system generates text/images/etc.
      - annex_iii_category (str, optional): explicit Annex III override
        (use this when keyword matching would be ambiguous)
    """
    name = system.get("name", "Unknown")
    purpose = system.get("purpose", "").lower()
    explicit_category = system.get("annex_iii_category")

    result = {
        "system_name": name,
        "purpose": system.get("purpose", ""),
        "risk_category": "MINIMAL_RISK",
        "annex_iii_match": None,
        "article_5_match": None,
        "justification": [],
        "obligations": [],
        "aima_questions_relevant": [],
    }

    # --- Step 1: Article 5 check (prohibited practices) ---
    for patterns, label in ARTICLE_5_PATTERNS:
        if any(p in purpose for p in patterns):
            result["risk_category"] = "UNACCEPTABLE_RISK"
            result["article_5_match"] = label
            result["justification"].append(f"Matches prohibited practice: {label}")
            break

    if result["risk_category"] != "UNACCEPTABLE_RISK":
        # --- Step 2: Annex III check (high risk) ---
        if explicit_category:
            # Explicit override takes priority over keyword matching
            result["risk_category"] = "HIGH_RISK"
            result["annex_iii_match"] = explicit_category
            result["justification"].append(f"Explicitly declared Annex III category: {explicit_category}")
        else:
            for patterns, label in ANNEX_III_PATTERNS:
                if any(p in purpose for p in patterns):
                    result["risk_category"] = "HIGH_RISK"
                    result["annex_iii_match"] = label
                    result["justification"].append(f"Keyword match to Annex III: {label}")
                    break

        # --- Step 3: Article 50 check (limited risk / transparency) ---
        if result["risk_category"] not in ("HIGH_RISK", "UNACCEPTABLE_RISK"):
            if system.get("has_human_interaction") or system.get("is_generative"):
                result["risk_category"] = "LIMITED_RISK"
                result["justification"].append(
                    "System interacts with humans or generates content "
                    "-> transparency obligations apply (Article 50)"
                )
            else:
                result["justification"].append(
                    "No Annex III match and no direct human interaction -> Minimal Risk"
                )

    # Map to AIMA questions
    result["obligations"] = RISK_CATEGORIES[result["risk_category"]]["obligations"]
    result["aima_questions_relevant"] = AIMA_QUESTIONS.get(result["risk_category"], [])
    return result


def classify_portfolio(systems: list) -> dict:
    """Classify a list of AI systems and produce a summary report."""
    classifications = [classify_system(s) for s in systems]
    summary = {cat: 0 for cat in RISK_CATEGORIES}
    for c in classifications:
        summary[c["risk_category"]] += 1
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "regulation": "EU AI Act 2024/1689",
        "referential_version": "AIMA 1.1.0",
        "total_systems": len(systems),
        "summary": summary,
        "classifications": classifications,
    }


# ---------------------------------------------------------------------------
# Markdown renderer
# ---------------------------------------------------------------------------

def render_markdown(report: dict) -> str:
    lines = [
        "# EU AI Act Classification Report",
        "",
        f"**Generated:** {report['generated_at']}  ",
        f"**Regulation:** {report['regulation']}  ",
        f"**AIMA Version:** {report['referential_version']}  ",
        f"**Total AI systems assessed:** {report['total_systems']}",
        "",
        "## Summary",
        "",
    ]
    for cat, count in report["summary"].items():
        meta = RISK_CATEGORIES[cat]
        lines.append(f"- {meta['color']} **{meta['label']}** ({meta['article']}): {count} system(s)")

    lines += ["", "---", "", "## System Classifications", ""]

    for c in report["classifications"]:
        cat = c["risk_category"]
        meta = RISK_CATEGORIES[cat]
        lines += [
            f"### {meta['color']} {c['system_name']}",
            "",
            f"**Risk Category:** {meta['label']}  ",
            f"**Legal Basis:** {meta['article']}  ",
            f"**Description:** {meta['description']}  ",
            f"**Purpose:** {c['purpose']}",
            "",
            "**Classification justification:**",
        ]
        for j in c["justification"]:
            lines.append(f"- {j}")

        if c.get("annex_iii_match"):
            lines += ["", f"**Annex III category:** `{c['annex_iii_match']}`"]

        lines += ["", "**Legal obligations that apply:**"]
        for o in c["obligations"]:
            lines.append(f"- {o}")

        if c["aima_questions_relevant"]:
            lines += ["", "**Key AIMA questions to verify:**"]
            for q in c["aima_questions_relevant"]:
                lines.append(f"- {q}")

        lines += ["", "---", ""]

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Demo data: Banque Atlas Digitale systems
# ---------------------------------------------------------------------------

DEMO_SYSTEMS = [
    {
        "name": "Atlas Assistant (Conversational LLM)",
        "purpose": (
            "Customer-facing conversational AI assistant. LLM with RAG on product "
            "documentation. Calls internal card management API (card blocking/unblocking)."
        ),
        "sector": "banking",
        "processes_personal_data": True,
        "has_human_interaction": True,
        "is_generative": True,
        # No annex_iii_category: conversational assistant only -> Limited Risk (Art. 50)
        # (The card-action capability is a risk handled by AIMA LLM.3.2, not by AI Act classification)
    },
    {
        "name": "Credit Scoring Model",
        "purpose": "AI-assisted creditworthiness assessment and loan decision support for retail banking.",
        "sector": "banking",
        "processes_personal_data": True,
        "has_human_interaction": False,
        "is_generative": False,
        "annex_iii_category": "Annex III.5b -- Credit scoring and solvency assessment",
    },
    {
        "name": "Fraud Detection Model",
        "purpose": "Real-time machine learning model for card transaction fraud detection.",
        "sector": "banking",
        "processes_personal_data": True,
        "has_human_interaction": False,
        "is_generative": False,
        # Fraud detection in financial services is debated under AI Act;
        # EBA guidelines suggest Annex III.5b may apply. We use explicit override.
        "annex_iii_category": "Annex III.5b -- Financial services AI: fraud/risk management (EBA guidance)",
    },
]


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="EU AI Act Classifier -- AIMA module by Mohamed"
    )
    parser.add_argument("--input", help="JSON file with list of AI systems to classify")
    parser.add_argument("--demo", action="store_true", help="Run with Banque Atlas Digitale demo data")
    parser.add_argument("--output", help="Output JSON file path")
    args = parser.parse_args()

    if args.demo:
        systems = DEMO_SYSTEMS
    elif args.input:
        systems = json.loads(Path(args.input).read_text(encoding="utf-8"))
    else:
        parser.print_help()
        return

    report = classify_portfolio(systems)
    markdown = render_markdown(report)
    print(markdown)

    # Save JSON
    if args.output:
        out_json = Path(args.output)
        out_json.write_text(json.dumps(report, ensure_ascii=False, indent=2))
        out_md = out_json.with_suffix(".md")
        out_md.write_text(markdown)
        print(f"\n[JSON saved to {out_json}]")
        print(f"[Markdown saved to {out_md}]")
    else:
        Path("eu-ai-act-report.md").write_text(markdown)
        print("[Markdown saved to eu-ai-act-report.md]")


if __name__ == "__main__":
    main()
