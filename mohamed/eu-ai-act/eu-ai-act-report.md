# EU AI Act Classification Report

**Generated:** 2026-11-25T19:02:26.864769+00:00  
**Regulation:** EU AI Act 2024/1689  
**AIMA Version:** AIMA 1.1.0  
**Total AI systems assessed:** 3

## Summary

- 🔴 **Unacceptable Risk** (Article 5): 0 system(s)
- 🟠 **High Risk** (Article 6 + Annex III): 2 system(s)
- 🟡 **Limited Risk** (Article 50): 1 system(s)
- 🟢 **Minimal Risk** (No specific obligations): 0 system(s)

---

## System Classifications

### 🟡 Atlas Assistant (Conversational LLM)

**Risk Category:** Limited Risk  
**Legal Basis:** Article 50  
**Description:** Transparency obligations: users must know they interact with AI.  
**Purpose:** Customer-facing conversational AI assistant. LLM with RAG on product documentation. Calls internal card management API (card blocking/unblocking).

**Classification justification:**
- System interacts with humans or generates content -> transparency obligations apply (Article 50)

**Legal obligations that apply:**
- Inform users they are interacting with an AI system (Article 50.1)
- Disclose AI-generated content (deepfakes, synthetic media) (Article 50.2)
- For emotion recognition / biometric categorisation: inform subjects (Article 50.3)
- No registration or conformity assessment required.

**Key AIMA questions to verify:**
- TRUST.1.1 -- Transparency: users must be informed they interact with AI
- TRUST.4.1 -- Reliability and confabulation control

---

### 🟠 Credit Scoring Model

**Risk Category:** High Risk  
**Legal Basis:** Article 6 + Annex III  
**Description:** Requires conformity assessment, CE marking, and EU database registration.  
**Purpose:** AI-assisted creditworthiness assessment and loan decision support for retail banking.

**Classification justification:**
- Explicitly declared Annex III category: Annex III.5b -- Credit scoring and solvency assessment

**Annex III category:** `Annex III.5b -- Credit scoring and solvency assessment`

**Legal obligations that apply:**
- Establish a risk management system (Article 9)
- Ensure data governance for training data (Article 10)
- Create and maintain technical documentation (Article 11)
- Enable automatic logging / record-keeping (Article 12)
- Provide transparency to deployers (Article 13)
- Ensure human oversight capabilities (Article 14)
- Meet accuracy, robustness, and cybersecurity requirements (Article 15)
- Conduct a conformity assessment (Article 43)
- Register the system in the EU AI Act database before placement (Article 49)
- Appoint an EU representative if provider is based outside the EU (Article 22)
- Conduct a Fundamental Rights Impact Assessment if deployed by a public body (Article 27)

**Key AIMA questions to verify:**
- GOV.4.1 -- Legal/regulatory tracking (AI Act Annex III compliance)
- GOV.3.2 -- AI system risk classification
- RISK.2.1 -- Impact assessment (Fundamental Rights Impact Assessment)
- TRUST.2.1 -- Bias and fairness measurement (required for high-risk systems)
- TRUST.1.1 -- Transparency disclosure to users and deployers
- TRUST.3.1 -- Human oversight capabilities
- DEV.3.1 -- Validation and testing before deployment
- DATA.1.1 -- Data quality and provenance for training data

---

### 🟠 Fraud Detection Model

**Risk Category:** High Risk  
**Legal Basis:** Article 6 + Annex III  
**Description:** Requires conformity assessment, CE marking, and EU database registration.  
**Purpose:** Real-time machine learning model for card transaction fraud detection.

**Classification justification:**
- Explicitly declared Annex III category: Annex III.5b -- Financial services AI: fraud/risk management (EBA guidance)

**Annex III category:** `Annex III.5b -- Financial services AI: fraud/risk management (EBA guidance)`

**Legal obligations that apply:**
- Establish a risk management system (Article 9)
- Ensure data governance for training data (Article 10)
- Create and maintain technical documentation (Article 11)
- Enable automatic logging / record-keeping (Article 12)
- Provide transparency to deployers (Article 13)
- Ensure human oversight capabilities (Article 14)
- Meet accuracy, robustness, and cybersecurity requirements (Article 15)
- Conduct a conformity assessment (Article 43)
- Register the system in the EU AI Act database before placement (Article 49)
- Appoint an EU representative if provider is based outside the EU (Article 22)
- Conduct a Fundamental Rights Impact Assessment if deployed by a public body (Article 27)

**Key AIMA questions to verify:**
- GOV.4.1 -- Legal/regulatory tracking (AI Act Annex III compliance)
- GOV.3.2 -- AI system risk classification
- RISK.2.1 -- Impact assessment (Fundamental Rights Impact Assessment)
- TRUST.2.1 -- Bias and fairness measurement (required for high-risk systems)
- TRUST.1.1 -- Transparency disclosure to users and deployers
- TRUST.3.1 -- Human oversight capabilities
- DEV.3.1 -- Validation and testing before deployment
- DATA.1.1 -- Data quality and provenance for training data

---
