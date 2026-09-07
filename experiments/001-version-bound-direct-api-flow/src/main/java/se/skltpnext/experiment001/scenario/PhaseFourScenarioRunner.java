package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.node.ObjectNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.consumer.Consumer;
import se.skltpnext.experiment001.contract.ContractValidators;
import se.skltpnext.experiment001.evidence.*;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.release.ReleaseValidator;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Executes stimuli; the catalog is consumed only by the independent observation oracle. */
public final class PhaseFourScenarioRunner {
    private final Path root;
    private final String run;
    private final RuntimeEnvironment.EnvironmentInfo environment;
    private final HttpClient control;
    public PhaseFourScenarioRunner(Path root, String run) {
        this.root = root; this.run = run;
        environment = RuntimeEnvironment.read(root);
        control = HttpClient.newBuilder().sslContext(TlsMaterial.clientContext(root)).build();
    }

    public ScenarioEngine.ScenarioResult run(String scenario, String variant) throws Exception {
        long start = System.nanoTime();
        new ReleaseValidator().validate();
        if (variant.equals("wrong-contract-version")) {
            var fixture = JsonSupport.readResource("experiment-001/scenarios/contract-faults-phase-4-1.0.0.json");
            String offered = fixture.required("offeredContractVersion").asText();
            var validation = new ContractValidators().bindVersion(offered);
            try (var telemetry = new TelemetryRecorder(root, run, scenario, variant, "consumer")) {
                telemetry.contract(validation.role(), validation.phase(), validation.result());
                telemetry.decision("consumer.contract-binding", "contract_validation", validation.passed() ? "allow" : "deny",
                        validation.passed() ? "contract-version-bound" : "wrong-contract-version");
            }
            PhaseFourEvents.record(root, run, scenario, variant, "binding", Map.of("offeredVersion", offered,
                    "boundVersion", ExperimentConfig.CONTRACT_VERSION, "contractSha256",
                    JsonSupport.sha256(JsonSupport.readResourceBytes(ContractValidators.CONTRACT_RESOURCE))));
        } else {
            configure(variant);
            var discovery = new MetadataStores(root).discover();
            var consumer = new Consumer(root, run, true);
            try {
                var token = consumer.obtainToken(scenario, variant, discovery, Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
                consumer.callResource(scenario, variant, discovery, "DPoP", token.value(),
                        CryptoMaterial.load(root).dpopKey(), "consumer-a", variant.equals("invalid-request"));
            } catch (Consumer.DependencyFailure failure) {
                // The transport observer already recorded the actual cause and terminal checkpoint.
            }
        }
        var observer = new PhaseFourEvidence(root, run, true);
        var decisions = observer.rows("telemetry/decisions.jsonl", scenario, variant);
        var terminal = decisions.getLast();
        ObjectNode result = JsonSupport.MAPPER.createObjectNode();
        result.put("schemaVersion", "4.0.0"); result.put("runId", run);
        result.put("scenarioId", scenario); result.put("variantId", variant);
        result.put("releaseId", ExperimentConfig.RELEASE_ID); result.put("releaseVersion", ExperimentConfig.RELEASE_VERSION);
        result.put("parameterSetId", ExperimentConfig.PARAMETER_SET_ID); result.put("expected", "deny");
        result.put("actual", terminal.path("result").asText()); result.put("status", "pass");
        result.put("terminalCheckpoint", terminal.path("checkpoint").asText()); result.put("reason", terminal.path("reason").asText());
        result.putArray("checkpoints").add(terminal.path("checkpoint").asText());
        result.put("telemetryRef", "telemetry/decisions.jsonl"); result.put("observationsRef", "phase-4/observations.jsonl");
        var audits = observer.rows("audit/records.jsonl", scenario, variant);
        if (audits.isEmpty()) result.putNull("auditRef"); else result.put("auditRef", audits.getLast().path("auditRecordId").asText());
        result.put("contractValidation", scenario.equals("E001-CON-002") ? "denied" : "not-applicable");
        result.put("leakageValidation", "pending-collection");
        Path file = root.resolve("results/" + scenario + "--" + variant + ".json");
        JsonSupport.writeJson(file, result);
        String status = !observer.timingDistinguishable(scenario, variant) ? "inconclusive"
                : observer.matches(scenario, variant, false) ? "pass" : "fail";
        result.put("status", status);
        JsonSupport.validateResource("experiment-001/schemas/scenario-result-phase-4.schema.json", result, "Phase 4 result");
        JsonSupport.writeJson(file, result);
        String digest = JsonSupport.sha256(file);
        PhaseFourEvents.record(root, run, scenario, variant, "finalized", Map.of("beforeSha256", digest));
        // The result above is final. Waiting here is evidence finalization, outside the HTTP retry budget.
        post(environment.authorizationServerEndpoint().resolve("/__drain"));
        post(environment.producerEndpoint().resolve("/__drain"));
        PhaseFourEvents.record(root, run, scenario, variant, "drained", Map.of("beforeSha256", digest,
                "afterSha256", JsonSupport.sha256(file), "scenarioDurationMillis", (System.nanoTime() - start) / 1_000_000,
                "hostLoadAverage", java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
        return new ScenarioEngine.ScenarioResult(scenario, variant, result.path("actual").asText(), status,
                List.of(result.path("terminalCheckpoint").asText()), result.path("auditRef").asText(null),
                result.path("contractValidation").asText(), "pending-collection", null);
    }

    public static ScenarioEngine.ScenarioResult inconclusive(Path root, String run, String scenario, String variant) {
        var result = JsonSupport.MAPPER.createObjectNode();
        result.put("schemaVersion", "4.0.0"); result.put("runId", run);
        result.put("scenarioId", scenario); result.put("variantId", variant);
        result.put("releaseId", ExperimentConfig.RELEASE_ID); result.put("releaseVersion", ExperimentConfig.RELEASE_VERSION);
        result.put("parameterSetId", ExperimentConfig.PARAMETER_SET_ID); result.put("expected", "deny");
        result.put("actual", "harness-error"); result.put("status", "inconclusive");
        result.put("terminalCheckpoint", "safe-evidence-finalization"); result.put("reason", "harness-error");
        result.putArray("checkpoints").add("safe-evidence-finalization"); result.putNull("auditRef");
        result.put("telemetryRef", "telemetry/decisions.jsonl"); result.put("observationsRef", "phase-4/observations.jsonl");
        result.put("contractValidation", "not-applicable"); result.put("leakageValidation", "pending-collection");
        JsonSupport.validateResource("experiment-001/schemas/scenario-result-phase-4.schema.json", result, "Phase 4 inconclusive");
        JsonSupport.writeJson(root.resolve("results/" + scenario + "--" + variant + ".json"), result);
        return new ScenarioEngine.ScenarioResult(scenario, variant, "harness-error", "inconclusive",
                List.of("safe-evidence-finalization"), null, "not-applicable", "pending-collection", null);
    }

    private void configure(String variant) throws Exception {
        String fault = switch (variant) {
            case "token-slow", "producer-slow" -> "SLOW";
            case "token-unavailable", "producer-unavailable" -> "UNAVAILABLE";
            case "invalid-response" -> "INVALID_RESPONSE";
            case "undocumented-error" -> "UNDOCUMENTED_ERROR";
            case "problem-details-internal-detail" -> "INTERNAL_DETAIL";
            default -> "NONE";
        };
        URI endpoint = variant.startsWith("token-") ? environment.authorizationServerEndpoint() : environment.producerEndpoint();
        post(endpoint.resolve("/__fault/" + fault));
    }
    private void post(URI uri) throws Exception {
        var response = control.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) throw new IllegalStateException("Fault control failed");
    }
}
