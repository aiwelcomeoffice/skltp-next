#!/usr/bin/env python3
"""Compare two preserved, independently validated core runs, without secrets.

Usage: python3 core-closeout-compare.py RUN_A_DIRECTORY RUN_B_DIRECTORY OUTPUT
Full Java evidence validation must have run before stop in each run. This
offline comparison rechecks hashes; it cannot repeat the private canary scan.
An unsuccessful comparison is inconclusive, not automatic falsification.
"""

import hashlib
import json
from pathlib import Path
import sys


# Only intentionally variable fields. Logical metadata ages, bounds, policy,
# reasons, status, endpoints, attempts and validation results remain compared.
VARIABLE = set("runId stimulusRef auditRef auditRecordId decisionRef traceId spanId parentSpanId "
               "durationMillis durationBucket scenarioDurationMillis atEpochMillis recordedAt "
               "activatedAt fetchedAt observedAt hostLoadAverage tokenAgeSeconds beforeSha256 afterSha256".split())


def read(path):
    return json.loads(path.read_text())


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalize(value):
    if isinstance(value, dict):
        return {k: ("<variable>" if v not in (None, "") else v) if k in VARIABLE
                else {key: "<digest>" for key in v} if k in ("beforeDigests", "afterDigests")
                else normalize(v) for k, v in value.items()}
    if isinstance(value, list):
        return [normalize(v) for v in value]
    return value


def stable_files(root):
    result = {}
    for path in sorted(root.rglob("*")):
        rel = path.relative_to(root).as_posix()
        if path.suffix not in (".json", ".jsonl"):
            continue
        if rel.split("/")[0] not in ("results", "telemetry", "audit", "contract", "errors",
                                      "network", "phase-4", "phase-5", "validation"):
            continue
        value = read(path) if path.suffix == ".json" else [json.loads(line) for line in path.read_text().splitlines()]
        if path.name == "capture.json":
            value = dict(value)
            # Materialized representations / private key lengths can vary.
            # Preserve channel, export flag, total hits and scanned field classes.
            value["channels"] = [{"channel": c["channel"], "exported": c["exported"],
                                   "hitCount": c["hitCount"],
                                   "fieldClasses": sorted({f["fieldClass"] for f in c["findings"]})}
                                  for c in value["channels"]]
        normalized = normalize(value)
        # JSONL is an event collection: cross-thread interleaving is not an oracle.
        # Multiplicity remains significant; causal order is validated per run.
        if path.suffix == ".jsonl":
            normalized = sorted(normalized, key=lambda v: json.dumps(v, sort_keys=True))
        result[rel] = normalized
    for name in ("artifact-digests.json", "parameters.json", "versions.json", "completeness.json"):
        result[name] = read(root / name)
    return result


def inspect_run(directory):
    root = directory / "evidence"
    manifest = read(root / "manifest.json")
    record = read(directory / "run-record.json")
    entries = {e["path"]: e["sha256"] for e in manifest["files"]}
    actual = {p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()}
    hashes = all(sha(root / name) == digest for name, digest in entries.items())
    sums = [line.split("  ", 1) for line in (root / "SHA256SUMS").read_text().splitlines()]
    hashes &= all(sha(root / name) == digest for digest, name in sums)
    hashes &= {name for _, name in sums} == actual - {"SHA256SUMS"}
    hashes &= actual == set(entries) | {"manifest.json", "SHA256SUMS"}
    # Load the canonical catalogue from the same committed tree as the run.
    import subprocess
    catalog = []
    for phase in range(1, 6):
        rel = f"experiments/001-version-bound-direct-api-flow/src/main/resources/experiment-001/scenarios/catalog-phase-{phase}-1.0.0.json"
        data = subprocess.check_output(["git", "show", manifest["sourceCommit"] + ":" + rel])
        parsed = json.loads(data)
        catalog.extend(parsed["variants"] if phase < 5 else
                       [{"scenarioId": key, "variantId": "baseline"} for key in parsed["scenarios"]])
    expected = {f'{v["scenarioId"]}--{v["variantId"]}.json' for v in catalog}
    results = {p.name: read(p) for p in (root / "results").glob("*.json")}
    leakage = read(root / "leakage/report.json")
    captures = [read(p) for p in sorted((root / "phase-5").glob("*/*/capture.json"))]
    checks = {
        "packageHashesAndInventory": hashes,
        "sameRecordedSource": record["sourceCommit"] == manifest["sourceCommit"],
        "cleanSource": manifest["gitStatusClass"] == "clean" and record["cleanBefore"] and record["cleanAfter"],
        "freshState": all(record[k] for k in ("targetAbsentBefore", "runtimeAbsentBeforePrepare", "evidenceAbsentBeforePrepare")),
        "stoppedAndPrivateRemoved": record["serverStopped"] and record["privateStateRemoved"],
        "fullValidationBeforeStop": record["fullValidationExit"] == 0,
        "suitePassed": any(c["argv"][3:4] == ["run-suite"] and c["exit"] == 0 for c in record["commands"]),
        "completeCore": len(expected) == 66 and len({v["scenarioId"] for v in catalog}) == 18 and set(results) == expected,
        "allResultsPass": all(v["status"] == "pass" and v["runId"] == manifest["runId"] for v in results.values()),
        "packagePass": manifest["status"] == "pass" and read(root / "completeness.json")["complete"],
        "toolGatesPass": read(root / "validation/tool-gates.json")["status"] == "pass",
        "phaseThreeOracle": all(v is True for v in read(root / "validation/phase-3.json").values()),
        "phaseFourOracle": all(v is True for v in read(root / "validation/phase-4.json").values()),
        "phaseFiveOracle": all(v == "pass" for v in read(root / "validation/phase-5.json").values()),
        "canaryCoverageAndNoHits": leakage["status"] == "pass" and leakage["hitCount"] == 0
            and len(leakage["canaryClasses"]) == 6 and len(captures) == 9
            and sum(len(c["channels"]) for c in captures) == 171
            and all(ch["hitCount"] == 0 for c in captures for ch in c["channels"]),
        "verificationSuite": record["tests"]["tests"] > 0 and all(record["tests"][k] == 0 for k in ("failures", "errors", "skipped")),
    }
    return {"runId": manifest["runId"], "sourceCommit": manifest["sourceCommit"], "checks": checks,
            "manifestSha256": sha(root / "manifest.json"), "sha256sumsSha256": sha(root / "SHA256SUMS"),
            "runRecordSha256": sha(directory / "run-record.json"), "results": len(results),
            "manifestEntries": len(entries), "stimuli": len(captures),
            "channelObservations": sum(len(c["channels"]) for c in captures),
            "channelHits": sum(ch["hitCount"] for c in captures for ch in c["channels"]),
            "leakage": {k: v for k, v in leakage.items() if k != "scannedFiles"}}


def compare(a, b):
    summaries = [inspect_run(p) for p in (a, b)]
    records = [read(p / "run-record.json") for p in (a, b)]
    fingerprints = {}
    for name in ("jwk-fingerprints.json", "tls-fingerprints.json", "metadata-authority-fingerprints.json"):
        left, right = (read(p / "evidence" / name) for p in (a, b))
        fingerprints[name] = left.keys() == right.keys() and all(left[k] != right[k] for k in left)
    left, right = (stable_files(p / "evidence") for p in (a, b))
    differences = [key for key in sorted(left.keys() | right.keys()) if left.get(key) != right.get(key)]
    checks = {"allRunControlsPass": all(all(s["checks"].values()) for s in summaries),
              "sameSource": summaries[0]["sourceCommit"] == summaries[1]["sourceCommit"],
              "distinctRuns": summaries[0]["runId"] != summaries[1]["runId"] and records[0]["checkout"] != records[1]["checkout"],
              "sequential": records[0]["finishedUtc"] < records[1]["startedUtc"],
              "freshPublicFingerprints": all(fingerprints.values()),
              "stableEvidenceEqual": not differences}
    return {"scope": "Experiment 001 core", "experiment001": "styrkt" if all(checks.values()) else "inkonklusiv",
            "basis": "Implementation plan sections 9 phase 6 and 10; specification section 1",
            "limitation": "Local synthetic harness; offline comparison relies on recorded full validation before private-state removal. Failed controls require review, never automatic falsification.",
            "runs": summaries, "checks": checks, "freshFingerprints": fingerprints,
            "comparedFiles": len(left), "semanticDifferences": differences,
            "variableFields": sorted(VARIABLE),
            "captureComparison": "channel/exported/hitCount/fieldClasses; private representation counts and digests validated per run",
            "eventOrdering": "JSONL multiset; within-run causal ordering validated by original oracles"}, left, right


if __name__ == "__main__":
    a, b, output = map(Path, sys.argv[1:4])
    result, left, right = compare(a, b)
    output.mkdir(parents=True, exist_ok=False)
    for name, value in (("comparison.json", result), ("run-a-stable.json", left), ("run-b-stable.json", right)):
        (output / name).write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"experiment001": result["experiment001"], "checks": result["checks"], "semanticDifferences": result["semanticDifferences"]}, indent=2))
    sys.exit(0 if result["experiment001"] == "styrkt" else 2)
