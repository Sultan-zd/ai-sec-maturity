#!/usr/bin/env python3
"""Unit tests for the EU AI Act classifier (stdlib unittest, no API needed)."""

import importlib.util
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parent / "eu_ai_act.py"
spec = importlib.util.spec_from_file_location("eu_ai_act", MODULE_PATH)
eu_ai_act = importlib.util.module_from_spec(spec)
spec.loader.exec_module(eu_ai_act)


class TestDemoClassification(unittest.TestCase):
    """The Banque Atlas Digitale demo must match the legal analysis."""

    @classmethod
    def setUpClass(cls):
        cls.report = eu_ai_act.classify_portfolio(eu_ai_act.DEMO_SYSTEMS)
        cls.by_name = {
            c["system_name"]: c for c in cls.report["classifications"]
        }

    def test_assistant_is_limited_risk(self):
        result = self.by_name["Atlas Assistant (Conversational LLM)"]
        self.assertEqual(result["risk_category"], "LIMITED_RISK")

    def test_credit_scoring_is_high_risk(self):
        result = self.by_name["Credit Scoring Model"]
        self.assertEqual(result["risk_category"], "HIGH_RISK")
        self.assertIn("Annex III.5b", result["annex_iii_match"])

    def test_fraud_detection_is_minimal_risk(self):
        """Fraud detection is not listed in Annex III -> minimal risk."""
        result = self.by_name["Fraud Detection Model"]
        self.assertEqual(result["risk_category"], "MINIMAL_RISK")
        self.assertIsNone(result["annex_iii_match"])

    def test_summary_counts(self):
        self.assertEqual(self.report["summary"]["LIMITED_RISK"], 1)
        self.assertEqual(self.report["summary"]["HIGH_RISK"], 1)
        self.assertEqual(self.report["summary"]["MINIMAL_RISK"], 1)
        self.assertEqual(self.report["summary"]["UNACCEPTABLE_RISK"], 0)

    def test_referential_version_is_current(self):
        self.assertEqual(self.report["referential_version"], "2.0.0")

    def test_markdown_mentions_version(self):
        markdown = eu_ai_act.render_markdown(self.report)
        self.assertIn("**AIMA Version:** 2.0.0", markdown)
        self.assertIn("Fraud Detection Model", markdown)


class TestClassificationRules(unittest.TestCase):
    """Cascade: Article 5 -> Annex III -> Article 50 -> Minimal."""

    def test_prohibited_social_scoring(self):
        result = eu_ai_act.classify_system({
            "name": "Citizen Score",
            "purpose": "A social credit system that scores citizen trustworthiness.",
        })
        self.assertEqual(result["risk_category"], "UNACCEPTABLE_RISK")
        self.assertIn("Article 5", result["article_5_match"])

    def test_annex_iii_keyword_match(self):
        result = eu_ai_act.classify_system({
            "name": "Recruitment Ranker",
            "purpose": "Automated hiring and job applicant screening.",
        })
        self.assertEqual(result["risk_category"], "HIGH_RISK")
        self.assertIn("Annex III.4", result["annex_iii_match"])

    def test_explicit_annex_override(self):
        result = eu_ai_act.classify_system({
            "name": "Custom High Risk",
            "purpose": "A system whose purpose text has no Annex III keyword.",
            "annex_iii_category": "Annex III.5b -- Credit scoring",
        })
        self.assertEqual(result["risk_category"], "HIGH_RISK")
        self.assertEqual(result["annex_iii_match"], "Annex III.5b -- Credit scoring")

    def test_article_50_human_interaction(self):
        result = eu_ai_act.classify_system({
            "name": "Chatbot",
            "purpose": "Customer support chatbot.",
            "has_human_interaction": True,
        })
        self.assertEqual(result["risk_category"], "LIMITED_RISK")

    def test_article_50_generative(self):
        result = eu_ai_act.classify_system({
            "name": "Image Generator",
            "purpose": "Generates synthetic images.",
            "is_generative": True,
        })
        self.assertEqual(result["risk_category"], "LIMITED_RISK")

    def test_plain_model_is_minimal(self):
        result = eu_ai_act.classify_system({
            "name": "Demand Forecast",
            "purpose": "Forecasts aggregate product demand.",
        })
        self.assertEqual(result["risk_category"], "MINIMAL_RISK")
        self.assertEqual(result["aima_questions_relevant"], [])

    def test_high_risk_maps_aima_questions(self):
        result = eu_ai_act.classify_system({
            "name": "Credit Score",
            "purpose": "Creditworthiness assessment.",
            "annex_iii_category": "Annex III.5b",
        })
        self.assertIn("GOV.4.1 -- Legal/regulatory tracking (AI Act Annex III compliance)",
                      result["aima_questions_relevant"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
