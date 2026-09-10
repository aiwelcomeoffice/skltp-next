#!/usr/bin/env python3
"""Run the unchanged core harness in a fresh, clean local Git checkout.

Usage: JAVA_HOME=/pinned/jdk python3 core-closeout-run.py CHECKOUT OUTPUT RUN_ID
OUTPUT must be outside CHECKOUT and must not exist. Never copies private state.
The phase-5 suite already selects all 18 core scenarios / 66 combinations.
"""

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def main():
    os.umask(0o077)
    checkout, output = (Path(p).resolve() for p in sys.argv[1:3])
    run_id = sys.argv[3]
    module = checkout / "experiments/001-version-bound-direct-api-flow"
    java = Path(os.environ["JAVA_HOME"]) / "bin/java"
    if output.is_relative_to(checkout):
        raise ValueError("Output must be outside the checkout")
    assert not output.exists()
    assert not (module / "target").exists(), "Use a fresh checkout"
    assert not subprocess.check_output(["git", "status", "--porcelain"], cwd=checkout).strip()
    output.mkdir(parents=True)
    record = {"runId": run_id, "checkout": str(checkout),
              "sourceCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=checkout).decode().strip(),
              "startedUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "cleanBefore": True, "targetAbsentBefore": True,
              "javaSha256": hashlib.sha256(java.read_bytes()).hexdigest(), "commands": []}
    cli = [str(java), "-jar", "target/experiment-001-cli.jar"]
    runtime = module / "target/experiment-001/runtime" / run_id
    evidence = module / "target/experiment-001/evidence" / run_id

    def command(args, log, required=True):
        with (output / log).open("ab") as stream:
            result = subprocess.run(args, cwd=module, stdout=stream, stderr=subprocess.STDOUT)
        record["commands"].append({"argv": args, "log": log, "exit": result.returncode})
        print(f"{args[0] if len(args) < 4 else args[3]}: exit {result.returncode}", flush=True)
        if required and result.returncode:
            raise RuntimeError(f"Command failed; see {log}")
        return result.returncode

    server = None
    try:
        command(["./mvnw", "-B", "-ntp", "clean", "verify"], "verify.log")
        reports = list((module / "target/surefire-reports").glob("TEST-*.xml"))
        record["tests"] = {key: sum(int(ET.parse(p).getroot().get(key, 0)) for p in reports)
                           for key in ("tests", "failures", "errors", "skipped")}
        shutil.copytree(module / "target/surefire-reports", output / "surefire-reports")
        record["runtimeAbsentBeforePrepare"] = not runtime.exists()
        record["evidenceAbsentBeforePrepare"] = not evidence.exists()
        assert record["runtimeAbsentBeforePrepare"] and record["evidenceAbsentBeforePrepare"]
        command(cli + ["verify-prerequisites"], "lifecycle.log")
        command(cli + ["prepare-fixtures", "--run-id", run_id, "--release", "1.0.0", "--parameters", "1.0.0"], "lifecycle.log")
        command(cli + ["validate", "--run-id", run_id], "lifecycle.log")
        start = cli + ["start-environment", "--run-id", run_id]
        with (runtime / "console.log").open("wb") as console:
            server = subprocess.Popen(start, cwd=module, stdout=console, stderr=subprocess.STDOUT)
        record["commands"].append({"argv": start, "log": "evidence/console/captured.log", "background": True})
        for attempt in range(5):
            if command(cli + ["check-readiness", "--run-id", run_id], "lifecycle.log", False) == 0:
                break
            time.sleep(1)
        command(cli + ["check-readiness", "--run-id", run_id], "lifecycle.log")
        # Preserve negative results too; never replace the run with a retry.
        command(cli + ["run-suite", "--run-id", run_id, "--through-phase", "5"], "suite.log", False)
        command(cli + ["collect-evidence", "--run-id", run_id], "lifecycle.log", False)
        record["fullValidationExit"] = command(cli + ["validate-evidence", "--run-id", run_id], "lifecycle.log", False)
        if evidence.exists():
            shutil.copytree(evidence, output / "evidence")
    finally:
        if runtime.exists():
            command(cli + ["stop-environment", "--run-id", run_id], "lifecycle.log", False)
            if server is not None:
                server.wait(timeout=15)
            command(cli + ["stop-environment", "--run-id", run_id], "lifecycle.log", False)
        if server is not None:
            server.wait(timeout=15)
        record["serverStopped"] = server is None or server.poll() is not None
        record["privateStateRemoved"] = not (runtime / "private").exists()
        record["cleanAfter"] = not subprocess.check_output(["git", "status", "--porcelain"], cwd=checkout).strip()
        record["finishedUtc"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        (output / "run-record.json").write_text(json.dumps(record, indent=2) + "\n")


if __name__ == "__main__":
    main()
