#!/usr/bin/env python3
"""Unit tests for the evidence-assistant auditor review workflow."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parent / "review.py"
spec = importlib.util.spec_from_file_location("evidence_review", MODULE_PATH)
review = importlib.util.module_from_spec(spec)
spec.loader.exec_module(review)


def suggestion(**overrides):
    base = {
        "questionCode": "GOV.1.2",
        "questionText": "Policy?",
        "suggestedLevel": 3,
        "confidence": "HIGH",
        "citedText": "Approved policy.",
        "reasoning": "The document states it.",
        "auditorDecision": None,
        "auditorFinalLevel": None,
        "auditorComment": None,
    }
    base.update(overrides)
    return base


class TestApplyReview(unittest.TestCase):
    def test_confirm_uses_suggested_level(self):
        reviewed = review.apply_review(suggestion(), "CONFIRMED", comment="Looks right")
        self.assertEqual(reviewed["auditorDecision"], "CONFIRMED")
        self.assertEqual(reviewed["auditorFinalLevel"], 3)
        self.assertEqual(reviewed["auditorComment"], "Looks right")
        self.assertIn("reviewedAt", reviewed)

    def test_override_uses_auditor_level(self):
        reviewed = review.apply_review(
            suggestion(), "OVERRIDDEN", final_level=4, comment="Evidence is stronger"
        )
        self.assertEqual(reviewed["auditorFinalLevel"], 4)

    def test_skip_clears_level(self):
        reviewed = review.apply_review(suggestion(), "SKIPPED", comment="Not relevant")
        self.assertIsNone(reviewed["auditorFinalLevel"])

    def test_decision_is_case_insensitive(self):
        reviewed = review.apply_review(suggestion(), "confirmed")
        self.assertEqual(reviewed["auditorDecision"], "CONFIRMED")

    def test_cannot_confirm_without_level(self):
        with self.assertRaises(ValueError):
            review.apply_review(suggestion(suggestedLevel=None), "CONFIRMED")

    def test_override_level_is_validated(self):
        for level in (0, 6, None, True, "3"):
            with self.assertRaises(ValueError):
                review.apply_review(suggestion(), "OVERRIDDEN", final_level=level)

    def test_unknown_decision_is_rejected(self):
        with self.assertRaises(ValueError):
            review.apply_review(suggestion(), "APPROVED")

    def test_original_is_not_mutated(self):
        original = suggestion()
        review.apply_review(original, "CONFIRMED")
        self.assertIsNone(original["auditorDecision"])


class TestSummariesAndAuto(unittest.TestCase):
    def test_summarize(self):
        items = [
            {"auditorDecision": "CONFIRMED"},
            {"auditorDecision": "CONFIRMED"},
            {"auditorDecision": "OVERRIDDEN"},
            {"auditorDecision": "SKIPPED"},
            {"auditorDecision": None},
        ]
        summary = review.summarize(items)
        self.assertEqual(summary["total"], 5)
        self.assertEqual(summary["confirmed"], 2)
        self.assertEqual(summary["overridden"], 1)
        self.assertEqual(summary["skipped"], 1)
        self.assertEqual(summary["pending"], 1)

    def test_auto_confirms_only_high_confidence(self):
        self.assertEqual(review.auto_decide(suggestion())[0], "CONFIRMED")
        self.assertEqual(
            review.auto_decide(suggestion(confidence="MEDIUM"))[0], "SKIPPED"
        )
        self.assertEqual(
            review.auto_decide(suggestion(suggestedLevel=None))[0], "SKIPPED"
        )

    def test_review_all(self):
        items = [suggestion(), suggestion(questionCode="RISK.1.1", confidence="LOW")]
        reviewed = review.review_all(items, review.auto_decide)
        self.assertEqual(reviewed[0]["auditorDecision"], "CONFIRMED")
        self.assertEqual(reviewed[1]["auditorDecision"], "SKIPPED")


class TestPersistence(unittest.TestCase):
    def test_default_output_path(self):
        self.assertEqual(
            review.default_output_path(Path("suggestions.json")),
            Path("suggestions.reviewed.json"),
        )

    def test_round_trip(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "suggestions.json"
            original = {
                "meta": {"model": "gpt-4o-mini", "total_questions": 1},
                "suggestions": [suggestion()],
            }
            path.write_text(json.dumps(original), encoding="utf-8")

            payload = review.load_payload(path)
            reviewed = review.review_all(payload["suggestions"], review.auto_decide)
            out = Path(tmp) / "reviewed.json"
            saved = review.save_payload(out, payload, reviewed)

            self.assertEqual(saved["review"]["summary"]["confirmed"], 1)
            self.assertEqual(saved["meta"]["model"], "gpt-4o-mini")
            reloaded = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(reloaded["suggestions"][0]["auditorFinalLevel"], 3)

    def test_invalid_payload_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "bad.json"
            path.write_text('{"not_suggestions": []}', encoding="utf-8")
            with self.assertRaises(ValueError):
                review.load_payload(path)


if __name__ == "__main__":
    unittest.main(verbosity=2)
