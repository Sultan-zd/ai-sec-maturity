#!/usr/bin/env python3
"""Unit tests for the AIMA evidence assistant (no OpenAI API needed)."""

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest import mock

MODULE_PATH = Path(__file__).resolve().parent / "assistant.py"
spec = importlib.util.spec_from_file_location("evidence_assistant", MODULE_PATH)
assistant = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assistant)

REPO_ROOT = Path(__file__).resolve().parents[2]
REFERENTIAL_DIR = REPO_ROOT / "referential" / "questions"


def fake_response(payload: dict) -> SimpleNamespace:
    return SimpleNamespace(
        choices=[
            SimpleNamespace(
                message=SimpleNamespace(content=json.dumps(payload))
            )
        ]
    )


class FakeCompletions:
    """Minimal stand-in for ``client.chat.completions``."""

    def __init__(self, questions, fail_calls=()):
        self.questions = questions
        self.fail_calls = set(fail_calls)
        self.calls = 0

    def create(self, **kwargs):
        self.calls += 1
        if self.calls in self.fail_calls:
            raise RuntimeError("simulated API failure")
        prompt = kwargs["messages"][-1]["content"]
        codes = [
            question["code"]
            for question in self.questions
            if f"[{question['code']}]" in prompt
        ]
        suggestions = []
        for code in codes:
            if code == "GOV.1.2":
                suggestions.append({
                    "questionCode": code,
                    "suggestedLevel": None,
                    "confidence": "LOW",
                    "citedText": None,
                    "reasoning": "No relevant content.",
                })
            else:
                suggestions.append({
                    "questionCode": code,
                    "suggestedLevel": 3,
                    "confidence": "HIGH",
                    "citedText": "The policy is approved.",
                    "reasoning": "The document states it.",
                })
        return fake_response({"suggestions": suggestions})


def fake_client(questions, fail_calls=()):
    completions = FakeCompletions(questions, fail_calls)
    return SimpleNamespace(chat=SimpleNamespace(completions=completions)), completions


class TestDocumentAndReferential(unittest.TestCase):
    def test_load_real_referential(self):
        questions = assistant.load_questions(REFERENTIAL_DIR)
        self.assertEqual(len(questions), 59)
        self.assertEqual(len({q["code"] for q in questions}), 59)

    def test_load_text_document(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "policy.txt"
            path.write_text("AI policy content.", encoding="utf-8")
            self.assertEqual(assistant.load_document_text(path), "AI policy content.")

    def test_missing_referential_raises(self):
        with self.assertRaises(FileNotFoundError):
            assistant.load_questions(Path("/does/not/exist"))


class TestResponseParsing(unittest.TestCase):
    def test_object_with_suggestions(self):
        content = json.dumps({"suggestions": [{"questionCode": "GOV.1.1"}]})
        self.assertEqual(
            assistant.parse_llm_response(content),
            [{"questionCode": "GOV.1.1"}],
        )

    def test_bare_array(self):
        content = json.dumps([{"questionCode": "GOV.1.1"}])
        self.assertEqual(len(assistant.parse_llm_response(content)), 1)

    def test_markdown_fence(self):
        content = '```json\n{"suggestions": [{"questionCode": "GOV.1.1"}]}\n```'
        self.assertEqual(len(assistant.parse_llm_response(content)), 1)

    def test_embedded_json(self):
        content = 'Here is the result:\n{"suggestions": [{"questionCode": "GOV.1.1"}]}\nDone.'
        self.assertEqual(len(assistant.parse_llm_response(content)), 1)

    def test_invalid_response(self):
        self.assertEqual(assistant.parse_llm_response("not json"), [])
        self.assertEqual(assistant.parse_llm_response(""), [])


class TestNormalization(unittest.TestCase):
    def test_valid_suggestion(self):
        result = assistant.normalize_suggestion(
            {
                "questionCode": "GOV.1.2",
                "suggestedLevel": 4,
                "confidence": "high",
                "citedText": "  Approved policy.  ",
                "reasoning": "  It exists.  ",
            },
            {"GOV.1.2"},
        )
        self.assertEqual(result["suggestedLevel"], 4)
        self.assertEqual(result["confidence"], "HIGH")
        self.assertEqual(result["citedText"], "Approved policy.")
        self.assertEqual(result["reasoning"], "It exists.")

    def test_string_level_is_accepted(self):
        result = assistant.normalize_suggestion(
            {"questionCode": "GOV.1.2", "suggestedLevel": "3", "confidence": "MEDIUM"},
            {"GOV.1.2"},
        )
        self.assertEqual(result["suggestedLevel"], 3)

    def test_out_of_range_level_becomes_null(self):
        result = assistant.normalize_suggestion(
            {"questionCode": "GOV.1.2", "suggestedLevel": 9, "confidence": "HIGH"},
            {"GOV.1.2"},
        )
        self.assertIsNone(result["suggestedLevel"])
        self.assertEqual(result["confidence"], "LOW")

    def test_boolean_level_is_rejected(self):
        result = assistant.normalize_suggestion(
            {"questionCode": "GOV.1.2", "suggestedLevel": True},
            {"GOV.1.2"},
        )
        self.assertIsNone(result["suggestedLevel"])

    def test_unknown_code_is_rejected(self):
        self.assertIsNone(
            assistant.normalize_suggestion(
                {"questionCode": "XXX.9.9", "suggestedLevel": 3},
                {"GOV.1.2"},
            )
        )

    def test_non_dict_is_rejected(self):
        self.assertIsNone(assistant.normalize_suggestion("oops", {"GOV.1.2"}))


class TestAnalysis(unittest.TestCase):
    def setUp(self):
        self.questions = [
            {"code": "GOV.1.1", "text": "Strategy?", "subdomain": "1.1",
             "criticality": 2, "effort": 2, "evidence": "doc"},
            {"code": "GOV.1.2", "text": "Policy?", "subdomain": "1.2",
             "criticality": 3, "effort": 2, "evidence": "doc"},
            {"code": "RISK.1.1", "text": "Method?", "subdomain": "1.1",
             "criticality": 3, "effort": 3, "evidence": "doc"},
        ]

    def test_every_question_gets_an_entry(self):
        client, completions = fake_client(self.questions)
        result = assistant.run_analysis("policy text", self.questions, client)

        self.assertEqual(completions.calls, 1)
        self.assertEqual(len(result["suggestions"]), 3)
        self.assertEqual(
            [s["questionCode"] for s in result["suggestions"]],
            ["GOV.1.1", "GOV.1.2", "RISK.1.1"],
        )
        self.assertEqual(result["meta"]["questions_with_evidence"], 2)
        self.assertEqual(result["meta"]["questions_without_evidence"], 1)
        self.assertEqual(result["meta"]["coverage_rate"], "66.7%")

    def test_api_error_keeps_all_questions(self):
        # Two batches, first one fails.
        questions = self.questions + [
            {"code": "DATA.1.1", "text": "Data?", "subdomain": "1.1",
             "criticality": 2, "effort": 2, "evidence": "doc"},
            {"code": "DATA.1.2", "text": "Quality?", "subdomain": "1.2",
             "criticality": 2, "effort": 2, "evidence": "doc"},
            {"code": "OPS.2.1", "text": "Logs?", "subdomain": "2.1",
             "criticality": 2, "effort": 2, "evidence": "doc"},
        ]
        client, completions = fake_client(questions, fail_calls={1})
        result = assistant.run_analysis("policy text", questions, client)

        self.assertEqual(completions.calls, 2)
        self.assertEqual(len(result["suggestions"]), 6)
        self.assertEqual(result["meta"]["api_errors"], 1)
        # First batch (5 questions) failed -> only the last one has evidence.
        self.assertEqual(result["meta"]["questions_with_evidence"], 1)

    def test_prompt_requests_json_object(self):
        prompt = assistant.build_question_prompt("doc", self.questions[:1])
        self.assertIn('"suggestions"', prompt)
        self.assertIn("[GOV.1.1]", prompt)
        self.assertIn("Return ONLY the JSON object", prompt)

    def test_report_contains_review_warning(self):
        client, _ = fake_client(self.questions)
        result = assistant.run_analysis("policy text", self.questions, client)
        markdown = assistant.render_markdown_report(result, "policy.txt")
        self.assertIn("suggestions only", markdown)
        self.assertIn("Auditor decision", markdown)


class TestLazyImport(unittest.TestCase):
    def test_create_client_without_openai(self):
        with mock.patch.dict(sys.modules, {"openai": None}):
            with self.assertRaises(RuntimeError) as context:
                assistant.create_client("sk-test")
        self.assertIn("openai", str(context.exception))


if __name__ == "__main__":
    unittest.main(verbosity=2)
