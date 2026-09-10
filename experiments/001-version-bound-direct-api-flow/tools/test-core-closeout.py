#!/usr/bin/env python3
"""Regression controls using preserved packages: python3 tools/test-core-closeout.py A B."""
import importlib.util
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("closeout", Path(__file__).with_name("core-closeout-compare.py"))
closeout = importlib.util.module_from_spec(spec)
spec.loader.exec_module(closeout)
A, B = map(Path, sys.argv[1:3])
del sys.argv[1:3]


class ComparisonControls(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.b = Path(self.temp.name) / "run-b"
        shutil.copytree(B, self.b)

    def write(self, relative, value):
        (self.b / relative).write_text(json.dumps(value))

    def rehash(self):
        root = self.b / "evidence"
        manifest = closeout.read(root / "manifest.json")
        for entry in manifest["files"]:
            entry["sha256"] = closeout.sha(root / entry["path"])
        self.write("evidence/manifest.json", manifest)
        (root / "SHA256SUMS").write_text("".join(
            f"{closeout.sha(p)}  {p.relative_to(root).as_posix()}\n"
            for p in sorted(root.rglob("*")) if p.is_file() and p.name != "SHA256SUMS"))

    def test_independent_runs_pass(self):
        result, _, _ = closeout.compare(A, self.b)
        self.assertEqual("styrkt", result["experiment001"])

    def test_semantic_change_even_with_new_checksums_is_inconclusive(self):
        relative = "evidence/results/E001-AUTHZ-001--local-policy-deny.json"
        value = closeout.read(self.b / relative)
        value["reason"] = "different-policy-reason"
        self.write(relative, value)
        self.rehash()
        result, _, _ = closeout.compare(A, self.b)
        self.assertEqual("inkonklusiv", result["experiment001"])
        self.assertTrue(result["runs"][1]["checks"]["packageHashesAndInventory"])
        self.assertIn(relative.removeprefix("evidence/"), result["semanticDifferences"])

    def test_missing_variant_rejected(self):
        (self.b / "evidence/results/E001-FLOW-001--baseline.json").unlink()
        # Missing input must fail closed, whether as a failed control or an error.
        with self.assertRaises(FileNotFoundError):
            closeout.compare(A, self.b)

    def test_no_full_validation_is_inconclusive(self):
        value = closeout.read(self.b / "run-record.json")
        value["fullValidationExit"] = 2
        self.write("run-record.json", value)
        self.assertEqual("inkonklusiv", closeout.compare(A, self.b)[0]["experiment001"])

    def test_reused_keys_are_inconclusive(self):
        shutil.copyfile(A / "evidence/jwk-fingerprints.json", self.b / "evidence/jwk-fingerprints.json")
        self.rehash()
        result, _, _ = closeout.compare(A, self.b)
        self.assertFalse(result["checks"]["freshPublicFingerprints"])
        self.assertEqual("inkonklusiv", result["experiment001"])

    def test_dirty_source_is_inconclusive(self):
        value = closeout.read(self.b / "evidence/manifest.json")
        value["gitStatusClass"] = "phase-5-working-tree"
        self.write("evidence/manifest.json", value)
        self.rehash()
        self.assertEqual("inkonklusiv", closeout.compare(A, self.b)[0]["experiment001"])

    def test_normalization_retains_oracle_values(self):
        value = {"reason": "deny", "policyVersion": "1.0.0", "timeoutMillis": 300,
                 "ageMillis": 60001, "attempts": 1, "httpStatus": 403, "durationMillis": 19}
        normalized = closeout.normalize(value)
        self.assertEqual({k: v for k, v in value.items() if k != "durationMillis"},
                         {k: v for k, v in normalized.items() if k != "durationMillis"})


if __name__ == "__main__":
    unittest.main()
