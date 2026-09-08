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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase5ScenarioIntegrationTest {
    private static final String RUN_ID = "phase5-integration-test";

    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS)
    Path temporaryDirectory;

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void phaseOneThroughFiveVariantsPassWithIndependentEvidence() throws Exception {
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
            combinations.addAll(ExperimentConfig.PHASE_5_VARIANTS.stream().sorted().toList());
            assertEquals(66, combinations.size());

            for (String combination : combinations) {
                String[] parts = combination.split("/", 2);
                ScenarioEngine.ScenarioResult result = engine.run(parts[0], parts[1]);
                assertTrue(result.passed(), combination + " must match its external oracle: " + result.status() + " " + result.checkpoints() + " runtime=" + runtime);
            }

            EvidenceCollector collector = new EvidenceCollector(runtime, evidence, RUN_ID);
            assertEquals("pass", collector.collect().status());
            assertEquals("pass", collector.validate().status());
            rejectIncompleteOrContradictoryEvidence(runtime, evidence, collector);
            assertTrue(ExperimentConfig.IMPLEMENTED_VARIANTS.stream().noneMatch(key -> key.startsWith("E001-AUTHN-") || key.startsWith("E001-DPOP-")));
            // Re-running OBS creates fresh stimuli without changing any of the 64 accepted source results.
            String before = JsonSupport.sha256(runtime.resolve("results/E001-FLOW-001--baseline.json"));
            String prior = java.nio.file.Files.readString(runtime.resolve("phase-5/E001-OBS-001/1/capture.json"));
            assertTrue(engine.run("E001-OBS-001", "baseline").passed());
            assertEquals(before, JsonSupport.sha256(runtime.resolve("results/E001-FLOW-001--baseline.json")));
            assertTrue(!prior.equals(java.nio.file.Files.readString(runtime.resolve("phase-5/E001-OBS-001/1/capture.json"))));
            assertEquals("pass", collector.collect().status());
            assertEquals("pass", collector.validate().status());
        }
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    private void rejectIncompleteOrContradictoryEvidence(Path runtime, Path evidence, EvidenceCollector collector) throws Exception {
        var oracle = new se.skltpnext.experiment001.evidence.PhaseFiveEvidence(evidence, runtime.resolve("private"), RUN_ID);
        Path capture = evidence.resolve("phase-5/E001-OBS-002/1/capture.json");
        String originalCapture = java.nio.file.Files.readString(capture);
        Path decisions = capture.getParent().resolve("telemetry/observations.jsonl");
        String original = java.nio.file.Files.readString(decisions);
        for (String field : List.of("category", "actor", "auditRef", "traceId", "decisionRef")) {
            var rows = se.skltpnext.experiment001.evidence.PhaseFiveEvidence.rows(decisions);
            var row = (com.fasterxml.jackson.databind.node.ObjectNode) rows.getLast();
            switch (field) {
                case "category" -> row.put(field, "authorization");
                case "actor" -> row.put(field, "producer");
                case "auditRef" -> row.put(field, "AUDIT-00000000-0000-0000-0000-000000000000");
                case "traceId" -> row.put(field, "00000000000000000000000000000000");
                case "decisionRef" -> row.put(field, "DECISION-00000000-0000-0000-0000-000000000000");
            }
            java.nio.file.Files.writeString(decisions, String.join("\n", rows.stream().map(r -> {
                try { return JsonSupport.MAPPER.writer().without(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT).writeValueAsString(r); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }).toList()) + "\n");
            updateDigest(capture, "telemetry/observations.jsonl", decisions);
            assertEquals("fail", oracle.evaluateScenario("E001-OBS-002"), "independent semantic control: " + field);
            assertFalse(collector.validate().passed());
            java.nio.file.Files.writeString(decisions, original);
            java.nio.file.Files.writeString(capture, originalCapture);
        }
        Path missing = evidence.resolve("phase-5/E001-OBS-001/3/console/server.log");
        byte[] bytes = java.nio.file.Files.readAllBytes(missing);
        java.nio.file.Files.delete(missing);
        assertEquals("inconclusive", oracle.evaluateScenario("E001-OBS-001"));
        java.nio.file.Files.write(missing, bytes);
        Path stimulus = evidence.resolve("phase-5/E001-OBS-001/5/capture.json");
        bytes = java.nio.file.Files.readAllBytes(stimulus);
        java.nio.file.Files.delete(stimulus);
        assertEquals("inconclusive", oracle.evaluateScenario("E001-OBS-001"));
        java.nio.file.Files.write(stimulus, bytes);
        assertEquals("pass", collector.validate().status());
        // A fully rechecksummed package with a contradictory actor still fails independent evaluation.
        Path runtimeDecision = runtime.resolve("phase-5/E001-OBS-002/1/telemetry/observations.jsonl");
        String saved = java.nio.file.Files.readString(runtimeDecision);
        java.nio.file.Files.writeString(runtimeDecision, saved.replace("\"actor\":\"authorization-server\"", "\"actor\":\"producer\""));
        Path runtimeCapture = runtimeDecision.getParent().getParent().resolve("capture.json");
        String savedCapture = java.nio.file.Files.readString(runtimeCapture);
        updateDigest(runtimeCapture, "telemetry/observations.jsonl", runtimeDecision);
        assertEquals("fail", collector.collect().status());
        assertFalse(collector.validate().passed());
        java.nio.file.Files.writeString(runtimeDecision, saved);
        java.nio.file.Files.writeString(runtimeCapture, savedCapture);
        assertEquals("pass", collector.collect().status());
        assertEquals("pass", collector.validate().status());
        // Deliberate leakage is fail, and only safe findings plus a digest leave private quarantine.
        Path leakCapture = runtime.resolve("phase-5/E001-OBS-001/1/capture.json");
        var captureJson = se.skltpnext.experiment001.evidence.PhaseFiveEvidence.read(leakCapture);
        String captureSaved = java.nio.file.Files.readString(leakCapture);
        String stimulusRef = captureJson.path("stimulusRef").asText();
        Path registry = runtime.resolve("private/observation-canaries/" + stimulusRef + ".jsonl");
        String marker = se.skltpnext.experiment001.evidence.PhaseFiveEvidence.rows(registry).getFirst().path("value").asText();
        Path safeChannel = leakCapture.getParent().resolve("console/runner.log");
        byte[] safe = java.nio.file.Files.readAllBytes(safeChannel);
        Path quarantine = runtime.resolve("private/observation-quarantine/" + stimulusRef + "/console/runner.log");
        java.nio.file.Files.createDirectories(quarantine.getParent());
        java.nio.file.Files.writeString(quarantine, marker);
        for (var channel : captureJson.required("channels")) if (channel.path("channel").asText().equals("console/runner.log")) {
            var row = (com.fasterxml.jackson.databind.node.ObjectNode) channel;
            var findings = se.skltpnext.experiment001.evidence.ObservationScanner.scan(quarantine, registry);
            row.put("sha256", JsonSupport.sha256(quarantine)); row.put("hitCount", se.skltpnext.experiment001.evidence.ObservationScanner.hits(findings));
            row.set("findings", JsonSupport.MAPPER.valueToTree(findings)); row.put("exported", false);
        }
        JsonSupport.writeJson(leakCapture, captureJson);
        java.nio.file.Files.delete(safeChannel);
        assertEquals("fail", collector.collect().status());
        assertFalse(collector.validate().passed());
        try (var files = java.nio.file.Files.walk(evidence)) {
            for (Path file : files.filter(java.nio.file.Files::isRegularFile).toList())
                assertFalse(java.nio.file.Files.readString(file).contains(marker), "No forbidden value in the failing evidence package");
        }
        java.nio.file.Files.write(safeChannel, safe);
        java.nio.file.Files.writeString(leakCapture, captureSaved);
        assertEquals("pass", collector.collect().status());
        assertEquals("pass", collector.validate().status());
    }
    private static void updateDigest(Path capture, String channel, Path file) throws Exception {
        var json = se.skltpnext.experiment001.evidence.PhaseFiveEvidence.read(capture);
        for (var row : json.required("channels")) if (channel.equals(row.path("channel").asText()))
            ((com.fasterxml.jackson.databind.node.ObjectNode) row).put("sha256", JsonSupport.sha256(file));
        JsonSupport.writeJson(capture, json);
    }
}
