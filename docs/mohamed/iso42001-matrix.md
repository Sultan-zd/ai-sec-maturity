# ISO/IEC 42001:2023 Annex A — Correspondence Matrix with AIMA Questions

> **Author:** Mohamed  
> **Date:** November 2026  
> **Referential version:** AIMA 1.1.0  
> **Purpose:** Map every ISO/IEC 42001:2023 Annex A control to AIMA question codes and identify gaps.

## Coverage Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Fully covered by one or more AIMA questions |
| 🟡 | Partially covered (some aspects only) |
| ⚠️ | Gap — not covered by any current AIMA question |

---

## Annex A Controls Mapping

### A.2 — AI Policy and Strategy

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.2.2 | AI policy | ✅ | GOV.1.2 |
| A.2.3 | Roles and responsibilities for AI | ✅ | GOV.2.1, GOV.2.2 |
| A.2.4 | Organizational AI objectives | ✅ | GOV.1.1 |
| A.2.5 | Communication about AI | 🟡 | GOV.2.3 (reporting only) |
| A.2.6 | Continual improvement of AIMS | ✅ | PEOPLE.3.1, PEOPLE.3.2 |

### A.3 — Internal Organization

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.3.2 | Roles and responsibilities | ✅ | GOV.2.1 |
| A.3.3 | Conflict of interest | ⚠️ | *Not covered* |
| A.3.4 | Reporting of concerns | ⚠️ | *Not covered* |

### A.4 — Resources for AI

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.4.2 | AI system inventory | ✅ | GOV.3.1, GOV.3.2 |
| A.4.3 | Infrastructure | 🟡 | OPS.3.1 (availability angle only) |
| A.4.4 | Human resources | 🟡 | PEOPLE.1.1, PEOPLE.1.2 |
| A.4.5 | Knowledge | 🟡 | PEOPLE.1.1 |
| A.4.6 | Awareness | ✅ | PEOPLE.1.2, PEOPLE.2.1 |

### A.5 — Impact Assessment

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.5.2 | AI system impact assessment | ✅ | RISK.2.1, RISK.2.2 |
| A.5.3 | Societal impact | 🟡 | RISK.2.1 (partial) |
| A.5.4 | Bias and fairness | ✅ | TRUST.2.1, TRUST.2.2 |
| A.5.5 | Vulnerable groups | ⚠️ | *Not covered* |

### A.6 — AI System Life Cycle

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.6.1.2 | AI system design | ✅ | DEV.1.1 |
| A.6.2.2 | Data governance for AI | ✅ | DATA.1.1, DATA.1.2 |
| A.6.2.3 | AI development practices | ✅ | DEV.1.1, DEV.1.2 |
| A.6.2.4 | AI system testing | ✅ | DEV.3.1, DEV.3.2 |
| A.6.2.5 | Deployment | ✅ | DEV.4.1 |
| A.6.2.6 | AI system operation and monitoring | ✅ | OPS.2.1, OPS.2.2, DEV.4.2 |
| A.6.2.7 | Continual improvement | ✅ | PEOPLE.3.2 |
| A.6.2.8 | Decommissioning | ⚠️ | *Not covered* |

### A.7 — Data for AI Systems

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.7.2 | Data collection | 🟡 | DATA.1.1 (quality angle) |
| A.7.3 | Data quality | ✅ | DATA.1.1 |
| A.7.4 | Data provenance | ✅ | DATA.1.1 |
| A.7.5 | Data preparation | 🟡 | DATA.1.2 |
| A.7.6 | Data protection and privacy | ✅ | DATA.2.1, DATA.2.2 |

### A.8 — Information for Interested Parties

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.8.2 | Transparency | ✅ | TRUST.1.1, TRUST.1.2 |
| A.8.3 | Documentation of AI systems | 🟡 | GOV.3.1 (inventory only) |
| A.8.4 | Incident management | ✅ | OPS.4.1, OPS.4.2 |

### A.9 — Human Oversight

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.9.2 | Human oversight mechanisms | ✅ | TRUST.3.1, TRUST.3.2 |
| A.9.3 | Override and intervention | 🟡 | LLM.3.2 (action validation) |
| A.9.4 | Contestability | ⚠️ | *Not covered — no question on users' right to contest AI decisions* |

### A.10 — Third-Party AI Systems

| ISO/IEC 42001 Control | Title | AIMA Coverage | AIMA Questions |
|---|---|---|---|
| A.10.2 | AI supply chain | ✅ | DEV.2.1 |
| A.10.3 | Third-party AI suppliers | ✅ | DEV.2.1, DEV.2.2 |

---

## Summary

| Status | Count | % |
|--------|-------|---|
| ✅ Fully covered | 26 controls | 65% |
| 🟡 Partially covered | 9 controls | 22.5% |
| ⚠️ Not covered (gaps) | 5 controls | 12.5% |
| **Total** | **40 controls** | **100%** |

## Identified Gaps (AIMA v1.1 does not cover)

| ISO/IEC 42001 Control | Topic | Suggested future AIMA question |
|---|---|---|
| A.3.3 | Conflict of interest in AI decisions | GOV.2.x — Does the organization manage conflicts of interest when deploying AI? |
| A.3.4 | Reporting of concerns (whistleblowing) | GOV.2.x — Is there a mechanism for employees to report concerns about AI systems? |
| A.5.5 | Vulnerable groups | RISK.2.x — Does the AI system impact assessment consider effects on vulnerable populations? |
| A.6.2.8 | Decommissioning of AI systems | DEV.4.x — Is there a formal process for decommissioning AI systems (data deletion, model archival)? |
| A.9.4 | Contestability of AI decisions | TRUST.3.x — Can individuals contest decisions made by AI systems, and is a human review available? |

> **Note:** These 5 gaps are candidates for AIMA v2.0. Adding them would bring coverage to ~100% of ISO/IEC 42001 Annex A.
