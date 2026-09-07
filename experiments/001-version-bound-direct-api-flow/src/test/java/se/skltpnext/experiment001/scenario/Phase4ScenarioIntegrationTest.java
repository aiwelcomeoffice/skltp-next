package se.skltpnext.experiment001.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.contract.ConformanceGates;
import se.skltpnext.experiment001.evidence.EvidenceCollector;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase4ScenarioIntegrationTest {
    private static final String RUN_ID = "phase4-integration-test";

    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS)
    Path temporaryDirectory;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void phaseOneThroughFourVariantsPassWithValidatedEvidence() throws Exception {
        Path runtime = temporaryDirectory.resolve("runtime");
        Path evidence = temporaryDirectory.resolve("target/experiment-001/evidence");
        java.nio.file.Files.createDirectories(runtime);
        var originalOut = System.out;
        var originalErr = System.err;
        try (var captured = new java.io.PrintStream(java.nio.file.Files.newOutputStream(runtime.resolve("console.log")), true, java.nio.charset.StandardCharsets.UTF_8)) {
            System.setOut(captured);
            System.setErr(captured);
        CryptoMaterial.generate(runtime);
        TlsMaterial.generate(runtime);
        var gates = new ConformanceGates().run();
        assertTrue(gates.passed(), "pinned tool gates must pass");
        JsonSupport.writeJson(runtime.resolve("validation/tool-gates.json"), gates);

        try (RuntimeEnvironment.RunningEnvironment ignored =
                     new RuntimeEnvironment(runtime, RUN_ID).start()) {
            ScenarioEngine engine = new ScenarioEngine(runtime, RUN_ID);
            assertTrue(engine.ready(), "both isolated HTTPS listeners must be ready");

            List<String> combinations = new ArrayList<>(List.of(
                    "E001-REL-001/valid",
                    "E001-DIS-001/baseline",
                    "E001-FLOW-001/baseline",
                    "E001-CON-001/baseline"));
            combinations.addAll(ExperimentConfig.PHASE_2_VARIANTS.stream().sorted().toList());
            combinations.addAll(ExperimentConfig.PHASE_3_VARIANTS.stream().sorted().toList());
            combinations.addAll(ExperimentConfig.PHASE_4_VARIANTS.stream().sorted().toList());
            assertEquals(64, combinations.size());

            for (String combination : combinations) {
                String[] parts = combination.split("/", 2);
                ScenarioEngine.ScenarioResult result = engine.run(parts[0], parts[1]);
                assertTrue(result.passed(), combination + " must match its external oracle: " + result.status() + " " + result.checkpoints() + " runtime=" + runtime);
            }

            EvidenceCollector collector = new EvidenceCollector(runtime, evidence, RUN_ID);
            assertEquals("pass", collector.collect().status());
            assertEquals("pass", collector.validate().status());
            rejectIncompleteOrContradictoryEvidence(runtime, evidence, collector);
        }
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    private void rejectIncompleteOrContradictoryEvidence(Path runtime, Path evidence, EvidenceCollector collector) throws Exception {
        for (String[] mutation : List.of(
                new String[]{"contract/validations.jsonl", "E001-CON-002", "invalid-response", "remove"},
                new String[]{"telemetry/decisions.jsonl", "E001-DEP-001", "token-slow", "checkpoint"},
                new String[]{"telemetry/dependencies.jsonl", "E001-DEP-001", "producer-unavailable", "attempts"},
                new String[]{"phase-4/observations.jsonl", "E001-DEP-001", "token-unavailable", "remove"},
                new String[]{"phase-4/observations.jsonl", "E001-DEP-001", "producer-slow", "afterSha256"},
                new String[]{"phase-4/observations.jsonl", "E001-CON-002", "problem-details-internal-detail", "internalDetailAbsent"})) {
            Path file = evidence.resolve(mutation[0]);
            String original = java.nio.file.Files.readString(file);
            var rows = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
            for (String line : original.lines().toList()) if (!line.isBlank()) rows.add(JsonSupport.MAPPER.readTree(line));
            boolean mutated = false;
            for (int i = rows.size() - 1; i >= 0; i--) {
                var row = rows.get(i);
                if (mutation[1].equals(row.path("scenarioId").asText()) && mutation[2].equals(row.path("variantId").asText())
                        && (mutation[3].equals("remove") || row.has(mutation[3]))) {
                    if (mutation[3].equals("remove")) rows.remove(i);
                    else if (mutation[3].equals("attempts")) ((com.fasterxml.jackson.databind.node.ObjectNode) row).put(mutation[3], 2);
                    else if (mutation[3].equals("internalDetailAbsent")) ((com.fasterxml.jackson.databind.node.ObjectNode) row).put(mutation[3], false);
                    else ((com.fasterxml.jackson.databind.node.ObjectNode) row).put(mutation[3], "wrong");
                    mutated = true; break;
                }
            }
            assertTrue(mutated);
            java.nio.file.Files.writeString(file, String.join("\n", rows.stream().map(JsonSupport::compact).toList()) + "\n");
            assertEquals(false, new se.skltpnext.experiment001.evidence.PhaseFourEvidence(evidence, RUN_ID)
                    .evaluate().get(mutation[1] + "/" + mutation[2]), "semantic mutation: " + mutation[3]);
            java.nio.file.Files.writeString(file, original);
        }
        // A new producer call after token timeout must fail even when a package is rechecksummed.
        Path ledger = runtime.resolve("events/network/payload-call-ledger.jsonl");
        String original = java.nio.file.Files.readString(ledger);
        JsonSupport.appendJsonLine(ledger, java.util.Map.of("runId", RUN_ID,
                "scenarioId", "E001-DEP-001", "variantId", "token-slow", "sender", "consumer-a",
                "receiver", "producer-b", "listenerId", "PRODUCER-ENDPOINT-REV-1", "method", "GET",
                "pathTemplate", "/synthetic-records/{recordId}", "apiDataReceived", true));
        assertEquals("inconclusive", collector.collect().status());
        assertEquals(false, collector.validate().passed());
        java.nio.file.Files.writeString(ledger, original);
        assertEquals("pass", collector.collect().status());
        assertEquals("pass", collector.validate().status());
        Path external = runtime.resolve("events/errors/external.jsonl");
        String safeExternal = java.nio.file.Files.readString(external);
        String marker = new se.skltpnext.experiment001.evidence.CanaryRegistry(runtime.resolve("private")).newValue("sensitive_claim");
        JsonSupport.appendJsonLine(external, java.util.Map.of("runId", RUN_ID,
                "scenarioId", "E001-CON-002", "variantId", "problem-details-internal-detail", "detail", marker));
        assertEquals(false, collector.collect().leakagePass(), "an internal Problem Details canary must be found");
        assertEquals(false, collector.validate().passed());
        java.nio.file.Files.writeString(external, safeExternal);
        assertEquals("pass", collector.collect().status());
        assertEquals("pass", collector.validate().status());
    }
}
