package se.skltpnext.experiment001.scenario;

import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.consumer.Consumer;
import se.skltpnext.experiment001.evidence.*;
import se.skltpnext.experiment001.metadata.MetadataStores;

import java.nio.file.*;
import java.util.*;

/** Isolates fresh source executions; no earlier result is an OBS input. */
public final class PhaseFiveScenarioRunner {
    private final Path root;
    private final String run;
    public PhaseFiveScenarioRunner(Path root, String run) { this.root = root; this.run = run; }

    public ScenarioEngine.ScenarioResult run(String scenario) throws Exception {
        var engine = new ScenarioEngine(root, run);
        engine.reset();
        Path backup = root.resolve("private/observation-backup");
        Files.createDirectories(backup);
        for (String name : List.of("events", "results"))
            if (Files.exists(root.resolve(name))) Files.move(root.resolve(name), backup.resolve(name));
        Path output = root.resolve("phase-5/" + scenario);
        EvidenceCollector.deleteTree(output);
        try {
            int index = 0;
            // The stimulus list is fixed here; the independent validator owns the expected table.
            List<String> stimuli = scenario.equals("E001-OBS-001") ? List.of(
                    "E001-FLOW-001/baseline", "E001-AUTHN-001/bad-signature", "E001-TOK-001/wrong-issuer",
                    "E001-DPOP-001/resource-bad-signature", "E001-CON-002/invalid-request") : List.of(
                    "E001-AUTHN-001/bad-signature", "E001-TOK-001/wrong-issuer",
                    "E001-SEC-002/baseline", "E001-AUTHZ-001/local-policy-deny");
            for (String source : stimuli) capture(engine, scenario, source, ++index, output);
        } finally {
            Files.deleteIfExists(root.resolve("private/observation-context.json"));
            engine.reset();
            for (String name : List.of("events", "results")) {
                EvidenceCollector.deleteTree(root.resolve(name));
                if (Files.exists(backup.resolve(name))) Files.move(backup.resolve(name), root.resolve(name));
            }
            Files.delete(backup);
        }
        String status = new PhaseFiveEvidence(root, root.resolve("private"), run).evaluateScenario(scenario);
        return result(root, run, scenario, status);
    }

    private void capture(ScenarioEngine engine, String scenario, String source, int index, Path output) throws Exception {
        engine.reset();
        for (String name : List.of("events", "results")) EvidenceCollector.deleteTree(root.resolve(name));
        for (String channel : PhaseFiveEvidence.CHANNELS) {
            if (channel.startsWith("console/") || channel.equals("stimulus-result.json")) continue;
            Path file = root.resolve("events/" + channel);
            Files.createDirectories(file.getParent()); Files.write(file, new byte[0]);
        }
        String ref = "STIMULUS-" + UUID.randomUUID();
        String[] parts = source.split("/", 2);
        var context = Map.of("scenarioId", scenario, "sourceScenarioId", parts[0], "sourceVariantId", parts[1], "stimulusRef", ref);
        JsonSupport.writeJson(root.resolve("private/observation-context.json"), context);
        Path registry = root.resolve("private/canaries.jsonl");
        long canaryStart = Files.size(registry);
        var canaries = new CanaryRegistry(root.resolve("private"));
        // Representatives also exist when an early deny prevents actual token/proof/payload production.
        for (String type : ObservationScanner.CLASSES) canaries.newValue(type);
        Path console = root.resolve("console.log");
        long consoleStart = Files.size(console); // Missing capture is insufficient evidence, never an empty substitute.
        Path captured = root.resolve("private/observation-console.log");
        var out = System.out; var err = System.err;
        boolean fixtureValid;
        try (var stream = new java.io.PrintStream(Files.newOutputStream(captured), true, java.nio.charset.StandardCharsets.UTF_8)) {
            System.setOut(stream); System.setErr(stream);
            try {
                fixtureValid = materialize(engine, parts[0], parts[1]);
            } finally {
                engine.reset(); // Drain both servers before closing and scanning any bytes.
                System.out.flush(); System.err.flush(); System.setOut(out); System.setErr(err);
            }
        }
        Path target = output.resolve(Integer.toString(index));
        Files.createDirectories(target);
        Path privateRegistry = root.resolve("private/observation-canaries/" + ref + ".jsonl");
        Files.createDirectories(privateRegistry.getParent());
        byte[] allCanaries = Files.readAllBytes(registry);
        Files.write(privateRegistry, Arrays.copyOfRange(allCanaries, (int) canaryStart, allCanaries.length));
        Path sourceResult = root.resolve("events/stimulus-result.json");
        JsonSupport.writeJson(sourceResult, Map.of("runId", run, "stimulusRef", ref,
                "sourceScenarioId", parts[0], "sourceVariantId", parts[1], "fixtureValid", fixtureValid));
        List<Map<String, Object>> channels = new ArrayList<>();
        for (String channel : PhaseFiveEvidence.CHANNELS) {
            Path file = root.resolve("events/" + channel);
            if (channel.equals("console/runner.log")) file = captured;
            if (channel.equals("console/server.log")) {
                byte[] bytes = Files.readAllBytes(console);
                file = root.resolve("private/observation-server-console.log");
                Files.write(file, Arrays.copyOfRange(bytes, (int) consoleStart, bytes.length));
            }
            var findings = ObservationScanner.scan(file, privateRegistry);
            long hits = ObservationScanner.hits(findings);
            Path exported = target.resolve(channel);
            // Never export forbidden bytes, even on a valid falsification.
            if (hits == 0) { Files.createDirectories(exported.getParent()); Files.copy(file, exported); }
            channels.add(Map.of("channel", channel, "sha256", JsonSupport.sha256(file), "hitCount", hits,
                    "findings", findings, "exported", hits == 0));
        }
        var observation = new LinkedHashMap<String, Object>(context);
        observation.put("runId", run); observation.put("variantId", "baseline");
        observation.put("fixtureValid", fixtureValid); observation.put("channels", channels);
        JsonSupport.writeJson(target.resolve("capture.json"), observation);
    }

    private boolean materialize(ScenarioEngine engine, String scenario, String variant) {
        if (ExperimentConfig.IMPLEMENTED_VARIANTS.contains(scenario + "/" + variant))
            return engine.run(scenario, variant).passed();
        var stimulus = scenario.equals("E001-AUTHN-001") ? Consumer.ObservationStimulus.BAD_ASSERTION_SIGNATURE
                : Consumer.ObservationStimulus.BAD_RESOURCE_SIGNATURE;
        var consumer = new Consumer(root, run, false, stimulus);
        var discovery = new MetadataStores(root).discover();
        try {
            var token = consumer.obtainToken(scenario, variant, discovery, Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
            if (stimulus == Consumer.ObservationStimulus.BAD_ASSERTION_SIGNATURE) return false;
            var response = consumer.callResource(scenario, variant, discovery, "DPoP", token.value(),
                    CryptoMaterial.load(root).dpopKey(), "consumer-a");
            return response.status() == 401 && response.contractValidated();
        } catch (Consumer.TokenRequestDenied denied) {
            return stimulus == Consumer.ObservationStimulus.BAD_ASSERTION_SIGNATURE && denied.status() == 401;
        }
    }

    public static ScenarioEngine.ScenarioResult result(Path root, String run, String scenario, String status) {
        var result = Map.of("schemaVersion", "5.0.0", "runId", run, "scenarioId", scenario, "variantId", "baseline",
                "releaseId", ExperimentConfig.RELEASE_ID, "releaseVersion", ExperimentConfig.RELEASE_VERSION,
                "parameterSetId", ExperimentConfig.PARAMETER_SET_ID, "status", status, "observationsRef", "phase-5/" + scenario);
        JsonSupport.validateResource("experiment-001/schemas/scenario-result-phase-5.schema.json",
                JsonSupport.MAPPER.valueToTree(result), "Phase 5 result");
        JsonSupport.writeJson(root.resolve("results/" + scenario + "--baseline.json"), result);
        return new ScenarioEngine.ScenarioResult(scenario, "baseline", status, status,
                List.of("observability.independent-evidence"), null, "not-applicable", status, null);
    }
}
