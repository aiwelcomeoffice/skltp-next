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

class Phase3ScenarioIntegrationTest {
    private static final String RUN_ID = "phase3-integration-test";

    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS)
    Path temporaryDirectory;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void phaseOneTwoAndThreeVariantsPassWithValidatedEvidence() throws Exception {
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
            assertEquals(55, combinations.size());

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
        // These semantic mutations bypass checksum verification deliberately: the observation oracle must reject them too.
        for (String[] mutation : List.of(
                new String[]{"telemetry/discovery.jsonl", "E001-DIS-002", "baseline", "endpointRevision"},
                new String[]{"telemetry/metadata.jsonl", "E001-META-002", "iam-stale", "ageMillis"},
                new String[]{"telemetry/transitions.jsonl", "E001-LIFE-001", "inactive-B-before-token", "elapsedMillis"},
                new String[]{"audit/records.jsonl", "E001-LIFE-001", "inactive-A-existing-token-after-offboarding", "remove"},
                new String[]{"contract/validations.jsonl", "E001-DIS-002", "baseline", "remove"})) {
            Path file = evidence.resolve(mutation[0]);
            String original = java.nio.file.Files.readString(file);
            var rows = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
            for (String line : original.lines().toList()) if (!line.isBlank()) rows.add(JsonSupport.MAPPER.readTree(line));
            for (int i = rows.size() - 1; i >= 0; i--) {
                var row = rows.get(i);
                if (mutation[1].equals(row.path("scenarioId").asText()) && mutation[2].equals(row.path("variantId").asText())) {
                    if (mutation[3].equals("remove")) rows.remove(i);
                    else ((com.fasterxml.jackson.databind.node.ObjectNode) row).put(mutation[3], 0);
                    break;
                }
            }
            java.nio.file.Files.writeString(file, "");
            for (var row : rows) JsonSupport.appendJsonLine(file, JsonSupport.MAPPER.convertValue(row, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}));
            assertEquals(false, new se.skltpnext.experiment001.evidence.PhaseThreeEvidence(evidence, RUN_ID)
                    .evaluate().get(mutation[1] + "/" + mutation[2]), "semantic mutation: " + mutation[0]);
            java.nio.file.Files.writeString(file, original);
        }
        Path sums = evidence.resolve("SHA256SUMS");
        String checksums = java.nio.file.Files.readString(sums);
        java.nio.file.Files.writeString(sums, "");
        assertEquals(false, collector.validate().passed(), "an empty checksum inventory must fail");
        java.nio.file.Files.writeString(sums, checksums);
        Path result = evidence.resolve("results/E001-DIS-002--baseline.json");
        byte[] resultBytes = java.nio.file.Files.readAllBytes(result);
        java.nio.file.Files.delete(result);
        assertEquals(false, collector.validate().passed(), "missing required variant must fail");
        java.nio.file.Files.write(result, resultBytes);

        // Repackage an impermissible call with valid checksums, proving this is more than byte-integrity checking.
        Path ledger = runtime.resolve("events/network/payload-call-ledger.jsonl");
        String original = java.nio.file.Files.readString(ledger);
        JsonSupport.appendJsonLine(ledger, java.util.Map.of("runId", RUN_ID,
                "scenarioId", "E001-REL-001", "variantId", "missing-ref", "sender", "consumer-a",
                "receiver", "authorization-server", "listenerId", "AS-LISTENER", "method", "POST",
                "pathTemplate", "/token", "apiDataReceived", false));
        assertEquals("inconclusive", collector.collect().status());
        assertEquals(false, collector.validate().passed(), "post-release-denial call must fail even with new checksums");
        java.nio.file.Files.writeString(ledger, original);
        assertEquals("pass", collector.collect().status());
        assertEquals("pass", collector.validate().status());
    }

}
