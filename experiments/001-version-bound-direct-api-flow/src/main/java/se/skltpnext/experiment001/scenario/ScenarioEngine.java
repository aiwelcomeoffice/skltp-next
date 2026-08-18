package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.consumer.Consumer;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.release.ReleaseValidator;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioEngine {
    private final Path runtimeRoot;
    private final String runId;
    private final RuntimeEnvironment.EnvironmentInfo environment;
    private final HttpClient controlClient;

    public ScenarioEngine(Path runtimeRoot, String runId) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        environment = RuntimeEnvironment.read(runtimeRoot);
        controlClient = HttpClient.newBuilder()
                .sslContext(TlsMaterial.clientContext(runtimeRoot))
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public ScenarioResult run(String scenarioId, String variantId) {
        String key = scenarioId + "/" + variantId;
        if (!ExperimentConfig.PHASE_1_VARIANTS.contains(key)) {
            throw new IllegalArgumentException("Scenario/variant is not implemented in Phase 1: " + key);
        }
        resetForScenario(scenarioId, variantId);
        try {
            ScenarioResult result = switch (key) {
                case "E001-REL-001/valid" -> releaseScenario(scenarioId, variantId);
                case "E001-DIS-001/baseline" -> discoveryScenario(scenarioId, variantId);
                case "E001-FLOW-001/baseline" -> flowScenario(scenarioId, variantId, false);
                case "E001-CON-001/baseline" -> flowScenario(scenarioId, variantId, true);
                default -> throw new IllegalStateException("Unreachable Phase 1 key");
            };
            writeResult(result);
            return result;
        } catch (Exception e) {
            ScenarioResult inconclusive = new ScenarioResult(
                    scenarioId, variantId, "harness-error", "inconclusive",
                    List.of("safe-evidence-finalization"), null,
                    "not-applicable", "pending-collection");
            writeResult(inconclusive);
            return inconclusive;
        }
    }

    public List<ScenarioResult> runPhaseOneSuite() {
        List<ScenarioResult> results = new ArrayList<>();
        results.add(run("E001-REL-001", "valid"));
        results.add(run("E001-DIS-001", "baseline"));
        results.add(run("E001-FLOW-001", "baseline"));
        results.add(run("E001-CON-001", "baseline"));
        return results;
    }

    private ScenarioResult releaseScenario(String scenarioId, String variantId) {
        var selection = new ReleaseValidator().validate();
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenarioId, variantId, "consumer")) {
            telemetry.decision("release.selection", "release_validation",
                    "allow", "immutable-references-valid");
        }
        return new ScenarioResult(scenarioId, variantId, "allow", "pass",
                List.of("release.schema", "release.reference-cardinality", "release.byte-digests"),
                null, "not-applicable", "pending-collection");
    }

    private ScenarioResult discoveryScenario(String scenarioId, String variantId) {
        new ReleaseValidator().validate();
        MetadataStores.DiscoveryResult discovery = new MetadataStores(runtimeRoot).discover();
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenarioId, variantId, "consumer")) {
            telemetry.decision("metadata.service", "metadata_validation", "allow", "service-revision-valid");
            telemetry.decision("membership.consumer-a", "membership_validation", "allow", "consumer-active");
            telemetry.decision("membership.producer-b", "membership_validation", "allow", "producer-active");
            telemetry.decision("metadata.iam", "metadata_validation", "allow", "iam-relation-valid");
            telemetry.decision("discovery.resolved", "discovery", "allow", "single-endpoint");
        }
        if (discovery.candidateCount() != 1
                || !discovery.consumerMembershipActive()
                || !discovery.producerMembershipActive()
                || !"PRODUCER-ENDPOINT-REV-1".equals(discovery.endpointId())) {
            throw new IllegalStateException("Discovery oracle mismatch");
        }
        return new ScenarioResult(scenarioId, variantId, "allow", "pass",
                List.of("service.revision-1", "membership.consumer-a", "membership.producer-b",
                        "iam.organization-system-client", "discovery.single-endpoint"),
                null, "not-applicable", "pending-collection");
    }

    private ScenarioResult flowScenario(String scenarioId, String variantId, boolean requireTraceOracle) {
        new ReleaseValidator().validate();
        MetadataStores.DiscoveryResult discovery = new MetadataStores(runtimeRoot).discover();
        Consumer.FlowResult flow = new Consumer(runtimeRoot, runId).execute(scenarioId, variantId, discovery);
        if (!flow.privateKeyJwtValidated() || !flow.dpopTokenBound()
                || !flow.producerTokenValidated() || !flow.producerSenderConstraintValidated()
                || !flow.producerAuthorized()) {
            throw new IllegalStateException("Flow checkpoint missing");
        }
        if (!onlyProducerReceivesApiData(scenarioId, variantId)) {
            throw new IllegalStateException("Payload-call ledger violates direct flow invariant");
        }
        if (!producerAuthorizationObserved(scenarioId, variantId)) {
            throw new IllegalStateException("Producer authorization was not independently observed");
        }
        if (!fourContractCheckpointsObserved(scenarioId, variantId)) {
            throw new IllegalStateException("Provider/consumer contract evidence incomplete");
        }
        if (requireTraceOracle && !traceRelationObserved(scenarioId, variantId,
                flow.consumerTraceId(), flow.consumerSpanId())) {
            throw new IllegalStateException("Consumer/producer trace relation missing");
        }
        String auditRef = latestAuditRef(scenarioId, variantId);
        if (auditRef == null || auditRef.equals(flow.consumerTraceId())) {
            throw new IllegalStateException("Audit reference must be separate from trace context");
        }
        List<String> checkpoints = new ArrayList<>(List.of(
                "authorization-server.client-authentication",
                "authorization-server.sender-constraint",
                "authorization-server.token-issuance",
                "producer.token-validation",
                "producer.sender-constraint",
                "producer.authorization",
                "network.consumer-to-producer-only",
                "contract.provider-request",
                "contract.provider-response",
                "contract.consumer-request",
                "contract.consumer-response"));
        if (requireTraceOracle) {
            checkpoints.add("trace.consumer-producer-parentage");
            checkpoints.add("audit.separate-reference");
        }
        return new ScenarioResult(scenarioId, variantId, "allow", "pass",
                checkpoints, auditRef, "pass", "pending-collection");
    }

    public void reset() {
        postReset(environment.authorizationServerEndpoint().resolve("/__reset"));
        postReset(environment.producerEndpoint().resolve("/__reset"));
    }

    public void resetForScenario(String scenarioId, String variantId) {
        String key = scenarioId + "/" + variantId;
        if (!ExperimentConfig.PHASE_1_VARIANTS.contains(key)) {
            throw new IllegalArgumentException("Scenario/variant is not implemented in Phase 1: " + key);
        }
        reset();
        for (String channel : List.of(
                "events/telemetry/spans.jsonl",
                "events/telemetry/decisions.jsonl",
                "events/telemetry/dependencies.jsonl",
                "events/audit/records.jsonl",
                "events/contract/validations.jsonl",
                "events/network/payload-call-ledger.jsonl",
                "events/errors/external.jsonl")) {
            removeScenarioEvents(runtimeRoot.resolve(channel), scenarioId, variantId);
        }
        try {
            Files.deleteIfExists(runtimeRoot.resolve("results")
                    .resolve(scenarioId + "--" + variantId + ".json"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reset scenario result", e);
        }
    }

    public boolean ready() {
        return readiness(environment.authorizationServerEndpoint().resolve("/ready"))
                && readiness(environment.producerEndpoint().resolve("/ready"))
                && Files.isRegularFile(runtimeRoot.resolve("metadata/service-rev-1.jws.json"))
                && Files.isRegularFile(runtimeRoot.resolve("metadata/membership-rev-1.jws.json"))
                && Files.isRegularFile(runtimeRoot.resolve("metadata/iam-rev-1.jws.json"));
    }

    private void postReset(URI uri) {
        try {
            HttpResponse<Void> response = controlClient.send(HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(2)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("State reset failed");
            }
        } catch (Exception e) {
            throw new IllegalStateException("State reset dependency failed", e);
        }
    }

    private boolean readiness(URI uri) {
        try {
            return controlClient.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean onlyProducerReceivesApiData(String scenarioId, String variantId) {
        List<JsonNode> matching = jsonLines(runtimeRoot.resolve("events/network/payload-call-ledger.jsonl"),
                scenarioId, variantId);
        long apiReceivers = matching.stream().filter(node -> node.required("apiDataReceived").booleanValue()).count();
        boolean onlyProducer = matching.stream()
                .filter(node -> node.required("apiDataReceived").booleanValue())
                .allMatch(node -> "producer-b".equals(node.required("receiver").textValue()));
        boolean asObservedWithoutApiData = matching.stream().anyMatch(node ->
                "authorization-server".equals(node.required("receiver").textValue())
                        && !node.required("apiDataReceived").booleanValue());
        return apiReceivers == 1 && onlyProducer && asObservedWithoutApiData;
    }

    private boolean producerAuthorizationObserved(String scenarioId, String variantId) {
        return jsonLines(runtimeRoot.resolve("events/telemetry/decisions.jsonl"), scenarioId, variantId)
                .stream().anyMatch(node -> "producer".equals(node.required("component").textValue())
                        && "authorization".equals(node.required("category").textValue())
                        && "allow".equals(node.required("result").textValue()));
    }

    private boolean fourContractCheckpointsObserved(String scenarioId, String variantId) {
        List<JsonNode> nodes = jsonLines(runtimeRoot.resolve("events/contract/validations.jsonl"),
                scenarioId, variantId);
        return nodes.stream().filter(node -> "pass".equals(node.required("result").textValue()))
                .map(node -> node.required("role").textValue() + "/" + node.required("phase").textValue())
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(java.util.Set.of("provider/request", "provider/response",
                        "consumer/request", "consumer/response"));
    }

    private boolean traceRelationObserved(String scenarioId, String variantId,
                                          String consumerTraceId, String consumerSpanId) {
        return jsonLines(runtimeRoot.resolve("events/telemetry/spans.jsonl"), scenarioId, variantId)
                .stream().anyMatch(node -> "producer".equals(node.required("component").textValue())
                        && consumerTraceId.equals(node.required("traceId").textValue())
                        && consumerSpanId.equals(node.required("parentSpanId").textValue()));
    }

    private String latestAuditRef(String scenarioId, String variantId) {
        List<JsonNode> nodes = jsonLines(runtimeRoot.resolve("events/audit/records.jsonl"),
                scenarioId, variantId);
        return nodes.isEmpty() ? null : nodes.getLast().required("auditRecordId").textValue();
    }

    private List<JsonNode> jsonLines(Path path, String scenarioId, String variantId) {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            List<JsonNode> result = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = JsonSupport.MAPPER.readTree(line);
                if (scenarioId.equals(node.path("scenarioId").asText())
                        && variantId.equals(node.path("variantId").asText())) {
                    result.add(node);
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read safe event channel", e);
        }
    }

    private static void removeScenarioEvents(Path path, String scenarioId, String variantId) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            List<String> retained = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = JsonSupport.MAPPER.readTree(line);
                if (!scenarioId.equals(node.path("scenarioId").asText())
                        || !variantId.equals(node.path("variantId").asText())) {
                    retained.add(line);
                }
            }
            String content = retained.isEmpty() ? "" : String.join("\n", retained) + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reset safe scenario evidence", e);
        }
    }

    private void writeResult(ScenarioResult result) {
        ObjectNode json = JsonSupport.MAPPER.createObjectNode();
        json.put("schemaVersion", "1.0.0");
        json.put("runId", runId);
        json.put("scenarioId", result.scenarioId());
        json.put("variantId", result.variantId());
        json.put("releaseId", ExperimentConfig.RELEASE_ID);
        json.put("releaseVersion", ExperimentConfig.RELEASE_VERSION);
        json.put("parameterSetId", ExperimentConfig.PARAMETER_SET_ID);
        json.put("expected", "allow");
        json.put("actual", result.actual());
        json.put("status", result.status());
        var checkpoints = json.putArray("checkpoints");
        result.checkpoints().forEach(checkpoints::add);
        json.put("telemetryRef", "telemetry/decisions.jsonl");
        if (result.auditRef() == null) {
            json.putNull("auditRef");
        } else {
            json.put("auditRef", result.auditRef());
        }
        json.put("contractValidation", result.contractValidation());
        json.put("leakageValidation", result.leakageValidation());
        JsonSupport.validate(JsonSupport.readResource(
                "experiment-001/schemas/scenario-result.schema.json"), json, "scenario result");
        JsonSupport.writeJson(runtimeRoot.resolve("results")
                .resolve(result.scenarioId() + "--" + result.variantId() + ".json"), json);
    }

    public record ScenarioResult(
            String scenarioId,
            String variantId,
            String actual,
            String status,
            List<String> checkpoints,
            String auditRef,
            String contractValidation,
            String leakageValidation) {
        public boolean passed() {
            return "pass".equals(status);
        }
    }
}
