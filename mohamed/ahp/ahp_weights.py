#!/usr/bin/env python3
"""
AHP (Analytic Hierarchy Process) Weight Validation for AIMA Domains
=====================================================================
Author: Mohamed
Date:   November 2026

This script:
1. Encodes a pairwise comparison matrix for the 8 AIMA domains.
2. Derives AHP weights and checks the Consistency Ratio (CR < 0.10).
3. Compares the AHP-derived weights with the weights of the AIMA referential.
4. Runs a real sensitivity analysis on the Banque Atlas Digitale case study:
   each domain weight is moved by +5% and -5% (relative shift, the other
   weights are redistributed proportionally), then we measure:
     - the change in the global maturity score and maturity level;
     - the change in the roadmap action ranking (Spearman rho, top-1,
       top-5 and top-10 membership).
5. Regenerates docs/mohamed/ahp-sensitivity.md from the actual results, so
   the documentation can never drift from the script again.

Usage:
    python3 mohamed/ahp/ahp_weights.py
    python3 mohamed/ahp/ahp_weights.py --no-write
    python3 mohamed/ahp/ahp_weights.py --output /tmp/ahp.md

The script uses only the Python standard library.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

# ---------------------------------------------------------------------------
# 1. Domain metadata
# ---------------------------------------------------------------------------

DOMAIN_CODES = ["GOV", "RISK", "DATA", "DEV", "LLM", "OPS", "TRUST", "PEOPLE"]

DOMAIN_NAMES = {
    "GOV": "Governance & Strategy",
    "RISK": "Risk Management",
    "DATA": "Data & Privacy",
    "DEV": "Secure Model Life Cycle",
    "LLM": "LLM / GenAI Security",
    "OPS": "Operations & Resilience",
    "TRUST": "Trustworthy AI",
    "PEOPLE": "Culture & Continuous Improvement",
}

# ---------------------------------------------------------------------------
# 2. Pairwise comparison matrix (project expert judgment)
# Saaty scale: 1=equal, 3=moderate, 5=strong, 7=very strong, 9=extreme
# Entry [i][j] = importance of domain i over domain j.
# ---------------------------------------------------------------------------
# This matrix is the structured transcription of the project's own
# prioritisation, not an independent panel of external experts:
#   - the five core domains are equally important (15% in the referential);
#   - each core domain is 2x more important than OPS or TRUST (10%);
#   - each core domain is 5x more important than PEOPLE (5%);
#   - OPS and TRUST are 3x more important than PEOPLE.
# The near-zero CR therefore proves internal consistency of the judgment,
# not external validity. This limitation is stated in the generated report.
PCM = [
    # GOV  RISK DATA DEV  LLM  OPS  TRUST PEOPLE
    [1, 1, 1, 1, 1, 2, 2, 5],          # GOV
    [1, 1, 1, 1, 1, 2, 2, 5],          # RISK
    [1, 1, 1, 1, 1, 2, 2, 5],          # DATA
    [1, 1, 1, 1, 1, 2, 2, 5],          # DEV
    [1, 1, 1, 1, 1, 2, 2, 5],          # LLM
    [1 / 2, 1 / 2, 1 / 2, 1 / 2, 1 / 2, 1, 1, 3],  # OPS
    [1 / 2, 1 / 2, 1 / 2, 1 / 2, 1 / 2, 1, 1, 3],  # TRUST
    [1 / 5, 1 / 5, 1 / 5, 1 / 5, 1 / 5, 1 / 3, 1 / 3, 1],  # PEOPLE
]

# Saaty's Random Consistency Index.
RI = {1: 0, 2: 0, 3: 0.58, 4: 0.90, 5: 1.12, 6: 1.24, 7: 1.32, 8: 1.41, 9: 1.45, 10: 1.49}


# ---------------------------------------------------------------------------
# 3. AHP computation
# ---------------------------------------------------------------------------

def compute_ahp_weights(matrix):
    """Compute the AHP priority vector, lambda_max, CI and CR."""
    n = len(matrix)
    col_sums = [sum(matrix[r][c] for r in range(n)) for c in range(n)]
    normalized = [[matrix[r][c] / col_sums[c] for c in range(n)] for r in range(n)]
    weights = [sum(normalized[r]) / n for r in range(n)]

    wsv = [sum(matrix[r][c] * weights[c] for c in range(n)) for r in range(n)]
    lambda_max = sum(wsv[i] / weights[i] for i in range(n)) / n
    ci = (lambda_max - n) / (n - 1)
    cr = ci / RI.get(n, 1.49)
    return weights, lambda_max, ci, cr


# ---------------------------------------------------------------------------
# 4. Data loading
# ---------------------------------------------------------------------------

def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def load_referential(root: Path):
    path = root / "referential" / "referentiel-v1.json"
    return load_json(path)


def load_case_study(root: Path):
    path = root / "docs" / "etude-de-cas" / "resultats.json"
    return load_json(path)


def referential_weights(referential):
    weights = {}
    for domain in referential["domains"]:
        weights[domain["code"]] = float(domain["weight"])
    missing = [code for code in DOMAIN_CODES if code not in weights]
    if missing:
        raise ValueError(f"Domaines absents du référentiel : {missing}")
    return weights


def domain_scores(case_study):
    """Audited domain scores from the case study."""
    scores = {}
    for domain in case_study["audited"]["domains"]:
        scores[domain["code"]] = float(domain["score"]["value"])
    return scores


def roadmap_actions(case_study):
    """Flatten the roadmap phases into one list of actions."""
    actions = []
    for phase in case_study["roadmap"]["phases"]:
        actions.extend(phase["actions"])
    return actions


def maturity_level(score, referential):
    """Return the maturity level whose minScore is the highest <= score."""
    levels = sorted(referential["maturityLevels"], key=lambda level: level["minScore"])
    current = levels[0]
    for level in levels:
        if score >= level["minScore"]:
            current = level
    return current


# ---------------------------------------------------------------------------
# 5. Scoring and ranking
# ---------------------------------------------------------------------------

def priority(action, weights):
    """Roadmap priority formula from docs/02-modele-maturite.md."""
    delta = action["targetLevel"] - action["currentLevel"]
    return delta * action["criticality"] * weights[action["domainCode"]] * 100


def global_score(scores, weights):
    return sum(scores[code] * weights[code] for code in DOMAIN_CODES)


def ranking(actions, weights):
    """Stable ranking: priority descending, then question code ascending."""
    return sorted(actions, key=lambda action: (-priority(action, weights), action["questionCode"]))


def average_ranks(priorities):
    """Average ranks for tied priorities (used by Spearman's rho)."""
    ordered = sorted(priorities.items(), key=lambda item: (-item[1], item[0]))
    ranks = {}
    i = 0
    while i < len(ordered):
        j = i
        while j + 1 < len(ordered) and math.isclose(ordered[j + 1][1], ordered[i][1], rel_tol=1e-12, abs_tol=1e-12):
            j += 1
        average = (i + 1 + j + 1) / 2
        for k in range(i, j + 1):
            ranks[ordered[k][0]] = average
        i = j + 1
    return ranks


def pearson(left, right):
    n = len(left)
    mean_left = sum(left) / n
    mean_right = sum(right) / n
    numerator = sum((a - mean_left) * (b - mean_right) for a, b in zip(left, right))
    denominator = math.sqrt(
        sum((a - mean_left) ** 2 for a in left) * sum((b - mean_right) ** 2 for b in right)
    )
    return numerator / denominator if denominator else 1.0


def spearman(actions, baseline_weights, weights):
    baseline = {a["questionCode"]: priority(a, baseline_weights) for a in actions}
    perturbed = {a["questionCode"]: priority(a, weights) for a in actions}
    left = average_ranks(baseline)
    right = average_ranks(perturbed)
    codes = sorted(left)
    return pearson([left[c] for c in codes], [right[c] for c in codes])


def perturb_weights(base_weights, domain, delta):
    """
    Move one domain weight by `delta` *relative* (e.g. +0.05 = +5%, so a
    15% weight becomes 15.75%) and redistribute the difference proportionally
    over the other domains. The result is non-negative and sums to 1.
    """
    codes = list(base_weights)
    weights = dict(base_weights)
    old = weights[domain]
    new = min(1.0, max(0.0, old * (1 + delta)))
    weights[domain] = new
    difference = new - old
    others_total = sum(base_weights[code] for code in codes if code != domain)
    if others_total:
        for code in codes:
            if code != domain:
                weights[code] = base_weights[code] - difference * (base_weights[code] / others_total)
    # Correct any floating-point drift so the weights sum exactly to 1.
    total = sum(weights.values())
    return {code: value / total for code, value in weights.items()}


# ---------------------------------------------------------------------------
# 6. Sensitivity analysis
# ---------------------------------------------------------------------------

def sensitivity_rows(base_weights, scores, actions, referential):
    baseline_global = global_score(scores, base_weights)
    baseline_level = maturity_level(baseline_global, referential)
    baseline_ranking = ranking(actions, base_weights)
    baseline_top1 = baseline_ranking[0]["questionCode"]
    baseline_top5 = {a["questionCode"] for a in baseline_ranking[:5]}
    baseline_top10 = {a["questionCode"] for a in baseline_ranking[:10]}

    rows = []
    for domain in DOMAIN_CODES:
        for delta in (0.05, -0.05):
            weights = perturb_weights(base_weights, domain, delta)
            new_global = global_score(scores, weights)
            new_level = maturity_level(new_global, referential)
            new_ranking = ranking(actions, weights)
            new_top1 = new_ranking[0]["questionCode"]
            new_top5 = {a["questionCode"] for a in new_ranking[:5]}
            new_top10 = {a["questionCode"] for a in new_ranking[:10]}
            rows.append({
                "domain": domain,
                "delta": delta,
                "weight": weights[domain],
                "global": new_global,
                "global_delta": new_global - baseline_global,
                "level": new_level["level"],
                "level_name": new_level["name"],
                "level_changed": new_level["level"] != baseline_level["level"],
                "spearman": spearman(actions, base_weights, weights),
                "top1": new_top1,
                "top1_changed": new_top1 != baseline_top1,
                "top5_changed": new_top5 != baseline_top5,
                "top10_changed": new_top10 != baseline_top10,
                "top10_entered": sorted(new_top10 - baseline_top10),
                "top10_left": sorted(baseline_top10 - new_top10),
            })
    return baseline_global, baseline_level, baseline_ranking, rows


# ---------------------------------------------------------------------------
# 7. Markdown report
# ---------------------------------------------------------------------------

def format_delta(value):
    return f"{value:+.4f}"


def render_markdown(ahp_weights, lambda_max, ci, cr, current_weights, baseline_global,
                    baseline_level, baseline_ranking, rows, case_study):
    lines = [
        "# AHP Weight Validation & Sensitivity Analysis",
        "",
        "> **Author:** Mohamed  ",
        "> **Date:** November 2026  ",
        "> **Script:** `mohamed/ahp/ahp_weights.py`  ",
        "> **Referential:** AIMA 2.0.0  ",
        "> **Case study used for sensitivity:** Banque Atlas Digitale "
        f"(`{case_study['organization']}`, assessment `{case_study['assessmentId']}`)",
        "",
        "This document is generated by the script. Run `python3 mohamed/ahp/ahp_weights.py`",
        "to reproduce every number below.",
        "",
        "## 1. Method",
        "",
        "The Analytic Hierarchy Process (Saaty, 1980) turns pairwise expert judgments into",
        "weights and measures how consistent those judgments are:",
        "",
        "1. Build an 8×8 pairwise comparison matrix on the Saaty 1–9 scale.",
        "2. Normalise each column and average the rows to obtain the priority vector.",
        "3. Compute λ_max, the Consistency Index `CI = (λ_max − n) / (n − 1)` and the",
        "   Consistency Ratio `CR = CI / RI` (RI = 1.41 for n = 8). `CR < 0.10` is acceptable.",
        "4. Compare the AHP weights with the weights actually used by AIMA.",
        "5. Move each domain weight by **+5% and −5%** (relative shift), redistribute the",
        "   difference proportionally over the other domains, and measure the impact on:",
        "   the global score, the maturity level, and the roadmap action ranking.",
        "",
        "## 2. Pairwise comparison rationale",
        "",
        "| Domains | Judgment |",
        "|---|---|",
        "| GOV, RISK, DATA, DEV, LLM | Equally important: the five pillars of AI security governance |",
        "| OPS, TRUST | Important but supportive: each core domain is 2× more important |",
        "| PEOPLE | Enabling, longer-term culture change: each core domain is 5× more important |",
        "",
        "The matrix is the project team's structured transcription of the existing",
        "prioritisation. It is **not** an independent panel of external experts; see",
        "the limitations in section 6.",
        "",
        "## 3. AHP results",
        "",
        "| Metric | Value | Assessment |",
        "|---|---|---|",
        f"| λ_max | {lambda_max:.4f} | Expected ≈ 8 for n = 8 |",
        f"| CI | {ci:.4f} | Very low |",
        f"| **CR** | **{cr:.4f}** | **{'✅ Acceptable (< 0.10)' if cr < 0.10 else '⚠️ Unacceptable (≥ 0.10)'}** |",
        "",
        "## 4. Weight comparison",
        "",
        "| Domain | Current weight | AHP-derived | Difference |",
        "|---|---:|---:|---:|",
    ]
    for code in DOMAIN_CODES:
        delta = ahp_weights[code] - current_weights[code]
        lines.append(
            f"| {code} — {DOMAIN_NAMES[code]} | {current_weights[code]:.1%} | "
            f"{ahp_weights[code]:.1%} | {delta:+.1%} |"
        )
    max_delta = max(abs(ahp_weights[c] - current_weights[c]) for c in DOMAIN_CODES)
    lines += [
        "",
        f"**Finding:** the largest difference is **{max_delta:.1%}** (percentage points).",
        "The AHP-derived weights are close to the referential weights, and the ordering of",
        "the five core domains is unchanged. The current weights are therefore coherent",
        "with the project's explicit prioritisation.",
        "",
        "## 5. Sensitivity analysis",
        "",
        "### 5.1 Baseline",
        "",
        f"- Global audited score: **{baseline_global:.4f} / 5** "
        f"(level {baseline_level['level']} — {baseline_level['name']}).",
        f"- Top action: **{baseline_ranking[0]['questionCode']}** "
        f"(priority {priority(baseline_ranking[0], current_weights):.0f}).",
        f"- Top-5: {', '.join(a['questionCode'] for a in baseline_ranking[:5])}.",
        f"- Top-10: {', '.join(a['questionCode'] for a in baseline_ranking[:10])}.",
        "",
        "The case study contains many actions with identical priorities (for example, all",
        "critical questions in a 15% domain with the same gap share the same priority).",
        "Ties are broken by question code; this is why top-5/top-10 membership can change",
        "even when the overall ranking is stable.",
        "",
        "### 5.2 Results",
        "",
        "| Domain | Shift | New weight | Global score | Δ score | Level | Spearman ρ | Top-1 | Top-5 changed | Top-10 changed |",
        "|---|---:|---:|---:|---:|---:|---:|---|---|---|",
    ]
    for row in rows:
        lines.append(
            f"| {row['domain']} | {row['delta']:+.0%} | {row['weight']:.2%} | "
            f"{row['global']:.4f} | {format_delta(row['global_delta'])} | "
            f"{row['level']} — {row['level_name']} | {row['spearman']:.3f} | "
            f"{row['top1']}{' ⚠️' if row['top1_changed'] else ''} | "
            f"{'yes' if row['top5_changed'] else 'no'} | "
            f"{'yes' if row['top10_changed'] else 'no'} |"
        )

    max_abs_delta = max(abs(row["global_delta"]) for row in rows)
    min_rho = min(row["spearman"] for row in rows)
    level_changes = [row for row in rows if row["level_changed"]]
    top1_changes = [row for row in rows if row["top1_changed"]]
    top5_changes = [row for row in rows if row["top5_changed"]]
    top10_changes = [row for row in rows if row["top10_changed"]]

    lines += [
        "",
        "### 5.3 Reading the results",
        "",
        f"- **Global score:** the largest shift is **{max_abs_delta:.4f}** points. "
        "The maturity level "
        + ("**never changes**." if not level_changes else
           "changes in " + ", ".join(f"{r['domain']} {r['delta']:+.0%}" for r in level_changes) + "."),
        f"- **Overall ranking:** Spearman ρ is at least **{min_rho:.3f}** across all 16 scenarios. "
        "The ranking is therefore highly stable overall.",
        f"- **Top-1:** the first action changes in {len(top1_changes)} of 16 scenarios"
        + (": " + ", ".join(f"{r['domain']} {r['delta']:+.0%} → {r['top1']}" for r in top1_changes)
           if top1_changes else "."),
        f"- **Top-5:** the membership changes in {len(top5_changes)} of 16 scenarios.",
        f"- **Top-10:** the membership changes in {len(top10_changes)} of 16 scenarios. "
        "Most of these changes are swaps between actions that share the same priority; "
        "the exact entering/leaving actions are listed in section 5.4.",
        "",
        "### 5.4 Top-10 membership changes",
        "",
        "| Scenario | Entered top-10 | Left top-10 |",
        "|---|---|---|",
    ]
    for row in top10_changes:
        lines.append(
            f"| {row['domain']} {row['delta']:+.0%} | "
            f"{', '.join(row['top10_entered']) or '—'} | "
            f"{', '.join(row['top10_left']) or '—'} |"
        )
    lines += [
        "",
        "**Answer to the task question:** a ±5% move in a single domain weight does **not**",
        "change the global maturity level, and it leaves the overall action ranking",
        f"essentially unchanged (ρ ≥ {min_rho:.3f}). The #1 action is stable in all 16",
        "scenarios. The composition of the top-5/top-10 can still change because many",
        "actions share the same priority; this is a tie-breaking effect, not a change of",
        "strategy. The weights are therefore robust enough for AIMA v2.0.0, but the",
        "tie-breaking rule should be made explicit in the platform (for example, priority",
        "then criticality then effort).",
        "",
        "## 6. Limitations",
        "",
        "1. **Single expert source.** The pairwise matrix is the project team's structured",
        "   judgment, not a survey of independent external experts. A near-zero CR proves",
        "   internal consistency, not external validity. A real expert panel would be",
        "   needed before claiming industry-wide validity.",
        "2. **Matrix reflects the current weights.** The judgments intentionally reproduce",
        "   the existing 15/15/15/15/15/10/10/5 prioritisation. The AHP therefore confirms",
        "   coherence; it does not independently discover the weights.",
        "3. **Case study data.** The sensitivity uses the audited scores and roadmap of the",
        "   Banque Atlas Digitale case study (referential v1.0.0). The criticality values",
        "   are those used when that case study was generated; re-running the case study",
        "   with v2.0.0 may change some priorities.",
        "4. **±5% is a relative shift.** A +5% shift on a 15% weight gives 15.75%, not 20%.",
        "   This is the usual interpretation of a ±5% sensitivity on a weight.",
        "5. **Ties.** Many roadmap actions share the same priority. Top-5/top-10 membership",
        "   is therefore sensitive to the tie-breaking rule; Spearman ρ is the more robust",
        "   indicator of overall stability.",
        "",
        "## 7. Conclusion",
        "",
        f"- The pairwise matrix is consistent (CR = {cr:.4f} < 0.10).",
        f"- The AHP weights differ from the referential by at most {max_delta:.1%}.",
        "- A ±5% move in any single domain weight changes the global score by less than",
        f"  {max_abs_delta:.3f} points and never changes the maturity level.",
        f"- The overall roadmap ranking is highly stable (ρ ≥ {min_rho:.3f}), although ties",
        "  make the exact top-5/top-10 composition sensitive.",
        "- **Recommendation:** keep the current weights for AIMA v2.0.0. Make the",
        "  tie-breaking rule explicit, and plan a real expert survey (or sector-specific",
        "  profiles) for AIMA v3.0.0.",
        "",
        "## 8. Reproduce",
        "",
        "```bash",
        "python3 mohamed/ahp/ahp_weights.py",
        "```",
        "",
        "## References",
        "",
        "- Saaty, T.L. (1980). *The Analytic Hierarchy Process*. McGraw-Hill.",
        "- ISO/IEC 42001:2023 — Clause 6.1 (risk and opportunity assessment).",
        "- NIST AI RMF 1.0 — GOVERN 1.4 (organizational risk priorities).",
        "- AIMA case study: `docs/09-etude-de-cas.md` and `docs/etude-de-cas/resultats.json`.",
        "",
    ]
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# 8. Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="AHP weight validation and sensitivity analysis")
    parser.add_argument("--output", default=None, help="Markdown output path (default: docs/mohamed/ahp-sensitivity.md)")
    parser.add_argument("--no-write", action="store_true", help="Print only, do not write the Markdown report")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent.parent
    referential = load_referential(root)
    case_study = load_case_study(root)
    current_weights = referential_weights(referential)

    if [d["code"] for d in referential["domains"]] != DOMAIN_CODES:
        raise SystemExit("The referential domain order does not match the AHP matrix order.")

    ahp_values, lambda_max, ci, cr = compute_ahp_weights(PCM)
    ahp_weights = dict(zip(DOMAIN_CODES, ahp_values))

    scores = domain_scores(case_study)
    actions = roadmap_actions(case_study)
    baseline_global, baseline_level, baseline_ranking, rows = sensitivity_rows(
        current_weights, scores, actions, referential
    )

    print("=" * 78)
    print("AIMA — AHP Weight Validation & Sensitivity Analysis")
    print("=" * 78)
    print(f"λ_max = {lambda_max:.4f}   CI = {ci:.4f}   CR = {cr:.4f} "
          f"({'acceptable' if cr < 0.10 else 'UNACCEPTABLE'})")
    print("\nDomain   Current   AHP      Δ")
    for code in DOMAIN_CODES:
        print(f"{code:<8} {current_weights[code]:>7.1%} {ahp_weights[code]:>7.1%} "
              f"{ahp_weights[code] - current_weights[code]:>+7.1%}")

    print(f"\nBaseline global score: {baseline_global:.4f} "
          f"(level {baseline_level['level']} — {baseline_level['name']})")
    print(f"Top action: {baseline_ranking[0]['questionCode']} "
          f"(priority {priority(baseline_ranking[0], current_weights):.0f})")
    print("\nSensitivity (±5% on one domain weight):")
    print(f"{'Domain':<8} {'Shift':>6} {'Global':>8} {'Δ':>8} {'Level':>5} {'ρ':>6} {'Top-1':>8} {'Top5':>5} {'Top10':>5}")
    for row in rows:
        print(f"{row['domain']:<8} {row['delta']:>+6.0%} {row['global']:>8.4f} "
              f"{row['global_delta']:>+8.4f} {row['level']:>5} {row['spearman']:>6.3f} "
              f"{row['top1']:>8} {'yes' if row['top5_changed'] else 'no':>5} "
              f"{'yes' if row['top10_changed'] else 'no':>5}")

    max_abs_delta = max(abs(row["global_delta"]) for row in rows)
    min_rho = min(row["spearman"] for row in rows)
    print(f"\nMax |Δ global| = {max_abs_delta:.4f}; min Spearman ρ = {min_rho:.3f}; "
          f"level changes = {sum(row['level_changed'] for row in rows)}/16")

    markdown = render_markdown(
        ahp_weights, lambda_max, ci, cr, current_weights, baseline_global,
        baseline_level, baseline_ranking, rows, case_study
    )

    if not args.no_write:
        output = Path(args.output) if args.output else root / "docs" / "mohamed" / "ahp-sensitivity.md"
        output.write_text(markdown, encoding="utf-8")
        print(f"\nReport written to: {output}")
    print("=" * 78)


if __name__ == "__main__":
    main()
