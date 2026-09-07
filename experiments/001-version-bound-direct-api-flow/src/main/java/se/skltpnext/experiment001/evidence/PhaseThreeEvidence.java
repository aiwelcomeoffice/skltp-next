package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.ExperimentConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Re-evaluates the frozen Phase 3 oracle using exported observations, without fixture/runtime access. */
public final class PhaseThreeEvidence {
    private final Path root;
    private final String run;
    private final Map<String, List<JsonNode>> channels = new java.util.HashMap<>();
    public PhaseThreeEvidence(Path evidenceRoot, String run) { this.root = evidenceRoot; this.run = run; }

    public Map<String, Boolean> evaluate() {
        var catalog = JsonSupport.readResource("experiment-001/scenarios/catalog-phase-3-1.0.0.json");
        JsonSupport.validate(JsonSupport.readResource("experiment-001/schemas/scenario-catalog-phase-3.schema.json"), catalog, "Phase 3 catalog");
        Map<String, Boolean> checks = new java.util.TreeMap<>();
        for (JsonNode expected : catalog.required("variants")) {
            String scenario = expected.required("scenarioId").asText();
            String variant = expected.required("variantId").asText();
            String key = scenario + "/" + variant;
            if (checks.containsKey(key)) throw new IllegalStateException("Duplicate catalog oracle");
            try { checks.put(key, matches(scenario, variant, expected)); }
            catch (Exception e) { checks.put(key, false); }
        }
        if (!checks.keySet().equals(ExperimentConfig.PHASE_3_VARIANTS)) throw new IllegalStateException("Catalog/completeness disagreement");
        return checks;
    }

    private boolean matches(String scenario, String variant, JsonNode expected) throws Exception {
        JsonNode result = JsonSupport.MAPPER.readTree(root.resolve("results/" + scenario + "--" + variant + ".json").toFile());
        if (!run.equals(result.path("runId").asText()) || !scenario.equals(result.path("scenarioId").asText())
                || !variant.equals(result.path("variantId").asText())
                || !expected.path("expected").equals(result.path("actual"))
                || !expected.path("expected").equals(result.path("expected"))) return false;
        List<JsonNode> decisions = rows("telemetry/decisions.jsonl", scenario, variant);
        List<JsonNode> network = rows("network/payload-call-ledger.jsonl", scenario, variant);
        List<JsonNode> dependencies = rows("telemetry/dependencies.jsonl", scenario, variant);
        List<JsonNode> metadata = rows("telemetry/metadata.jsonl", scenario, variant);
        List<JsonNode> discoveries = rows("telemetry/discovery.jsonl", scenario, variant);
        List<JsonNode> transitions = rows("telemetry/transitions.jsonl", scenario, variant);
        List<JsonNode> contracts = rows("contract/validations.jsonl", scenario, variant);
        List<JsonNode> audits = rows("audit/records.jsonl", scenario, variant);
        if (!rows("errors/harness.jsonl", scenario, variant).isEmpty() || decisions.isEmpty()) return false;
        JsonNode terminal = decisions.getLast();
        if (!terminal.path("checkpoint").equals(expected.path("terminalCheckpoint"))
                || !terminal.path("result").equals(expected.path("expected"))
                || !terminal.path("reason").equals(expected.path("reason"))) return false;
        List<String> expectedReceivers = new ArrayList<>();
        expected.required("receivers").forEach(n -> expectedReceivers.add(n.asText()));
        if (!network.stream().map(n -> n.path("receiver").asText()).toList().equals(expectedReceivers)) return false;
        if (network.stream().filter(n -> n.path("apiDataReceived").asBoolean()).count() != expected.path("payloadReceivers").asInt()) return false;
        if (network.stream().anyMatch(n -> n.path("apiDataReceived").asBoolean() && !n.path("receiver").asText().equals("producer-b"))) return false;
        long tokenCalls = expectedReceivers.stream().filter("authorization-server"::equals).count();
        long producerCalls = expectedReceivers.stream().filter("producer-b"::equals).count();
        if (dependencies.stream().filter(n -> n.path("dependency").asText().equals("authorization-server")).count() != tokenCalls
                || dependencies.stream().filter(n -> n.path("dependency").asText().equals("producer")).count() != producerCalls
                || dependencies.stream().anyMatch(n -> n.path("attempts").asInt() != 1)) return false;
        if (producerCalls == 0 && (!contracts.isEmpty() || !rows("telemetry/spans.jsonl", scenario, variant).isEmpty())) return false;
        if (producerCalls > 0) {
            for (String role : List.of("provider", "consumer")) for (String phase : List.of("request", "response")) {
                if (contracts.stream().filter(n -> role.equals(n.path("role").asText()) && phase.equals(n.path("phase").asText())
                        && "pass".equals(n.path("result").asText())).count() != producerCalls) return false;
            }
        }
        for (JsonNode event : metadata) validateEvent("metadata", event);
        for (JsonNode event : discoveries) validateEvent("discovery", event);
        for (JsonNode event : transitions) validateEvent("transition", event);
        for (JsonNode event : audits) validateEvent("audit", event);
        if (!result.path("auditRef").isNull() && audits.stream().noneMatch(n -> n.path("auditRecordId").equals(result.path("auditRef")))) return false;
        if (scenario.equals("E001-REL-001")) return metadata.isEmpty() && discoveries.isEmpty() && transitions.isEmpty()
                && dependencies.isEmpty() && audits.isEmpty();
        if (metadata.isEmpty()) return false;
        for (JsonNode event : metadata) {
            long cacheAge = Duration.between(Instant.parse(event.path("fetchedAt").asText()),
                    Instant.parse(event.path("observedAt").asText())).toMillis();
            if (cacheAge < 0 || cacheAge != event.path("cacheAgeMillis").asLong()) return false;
            String family = event.path("family").asText();
            long ttl = family.equals("service") ? 30000 : 15000;
            if (event.path("ttlMillis").asLong() != ttl || event.path("maxStalenessMillis").asLong() != ttl * 2) return false;
            if ("allow".equals(event.path("result").asText()) && event.path("ageMillis").asLong() > ttl * 2) return false;
            if (!event.path("authorityRef").asText().equals("metadata-" + family)) return false;
        }
        if (scenario.equals("E001-DIS-003")) {
            JsonNode lookup = discoveries.getLast();
            return lookup.path("candidateCount").asInt() == (variant.equals("missing-endpoint") ? 0 : 2)
                    && lookup.path("endpointId").isNull() && lookup.path("result").asText().equals("deny");
        }
        if (scenario.equals("E001-META-002") || (scenario.equals("E001-META-001") && !variant.contains("revoked-after-bound"))) {
            JsonNode last = metadata.getLast();
            if (!last.path("family").equals(expected.path("family")) || !last.path("reason").equals(expected.path("reason"))
                    || !last.path("result").asText().equals("deny")) return false;
            if (variant.endsWith("-rollback") && metadata.stream().noneMatch(n -> n.path("family").equals(expected.path("family"))
                    && n.path("revision").asInt() == 2 && n.path("result").asText().equals("allow"))) return false;
            if (scenario.equals("E001-META-002")) {
                long max = last.path("maxStalenessMillis").asLong();
                return last.path("ageMillis").asLong() == max + 1 && last.path("cacheState").asText().equals("source-unavailable")
                        && metadata.stream().anyMatch(n -> n.path("family").equals(expected.path("family"))
                            && n.path("ageMillis").asLong() == max && n.path("result").asText().equals("allow"));
            }
            return true;
        }
        if (transitions.size() != 1) return false;
        JsonNode transition = transitions.getFirst();
        long elapsed = Duration.between(Instant.parse(transition.path("activatedAt").asText()), Instant.parse(transition.path("observedAt").asText())).toMillis();
        if (elapsed < 0 || elapsed > 1000 || elapsed != transition.path("elapsedMillis").asLong()
                || transition.path("boundMillis").asLong() != 1000
                || transition.path("oldRevision").asInt() != 1 || transition.path("newRevision").asInt() != 2) return false;
        boolean updatedFamilyObserved = metadata.stream().anyMatch(n -> n.path("family").equals(expected.path("family"))
                && n.path("revision").asInt() == 2 && n.path("result").asText().equals("allow")
                && n.path("propagationMillis").asLong() <= 1000 && n.path("cacheState").asText().equals("invalidated"));
        if (!updatedFamilyObserved) return false;
        if (scenario.equals("E001-DIS-002")) {
            return discoveries.size() == 2 && discoveries.get(0).path("endpointRevision").asInt() == 1
                    && discoveries.get(1).path("endpointRevision").asInt() == 2
                    && discoveries.get(0).path("lookupRef").equals(discoveries.get(1).path("lookupRef"))
                    && network.get(1).path("listenerId").equals(discoveries.get(0).path("endpointId"))
                    && network.get(3).path("listenerId").equals(discoveries.get(1).path("endpointId"))
                    && transition.has("beforeDigests") && transition.path("beforeDigests").size() >= 7
                    && transition.path("beforeDigests").equals(transition.path("afterDigests"))
                    && audits.size() == 2;
        }
        if (variant.equals("unpublished-service")) return discoveries.size() == 3
                && discoveries.subList(1, 3).stream().allMatch(n -> n.path("candidateCount").asInt() == 0 && n.path("endpointId").isNull());
        if (variant.equals("inactive-B-before-token")) return true;
        if (!transition.path("technicalTokenValid").asBoolean()) return false;
        // Slice after the successful issuance/control operation. No later security checkpoint can follow the terminal deny.
        if (variant.equals("inactive-A-token-request-after-offboarding")) {
            return decisions.stream().filter(n -> n.path("checkpoint").asText().equals("authorization-server.token-issuance")).count() == 1
                    && decisions.stream().filter(n -> n.path("checkpoint").asText().equals("authorization-server.client-authentication")
                        && n.path("result").asText().equals("allow")).count() == 2
                    && audits.stream().anyMatch(n -> n.path("checkpoint").asText().equals("authorization-server.membership")
                        && n.path("decidingParty").asText().equals("authorization-server") && n.path("result").asText().equals("deny"));
        }
        List<JsonNode> producer = decisions.stream().filter(n -> n.path("component").asText().equals("producer")
                && Set.of("token_validation", "sender_constraint", "authorization", "business_operation").contains(n.path("category").asText())).toList();
        int lastToken = -1;
        for (int i = 0; i < producer.size(); i++) if (producer.get(i).path("checkpoint").asText().equals("producer.token-validation")) lastToken = i;
        if (lastToken < 0) return false;
        var after = producer.subList(lastToken, producer.size());
        if (variant.contains("revoked-after-bound")) return after.size() == 1 && after.getFirst().path("result").asText().equals("deny");
        return after.stream().map(n -> n.path("result").asText()).toList().equals(List.of("allow", "allow", "deny"))
                && audits.getLast().path("result").asText().equals("deny");
    }

    private static void validateEvent(String kind, JsonNode event) {
        JsonSupport.validateResource("experiment-001/schemas/" + kind + "-event-phase-3.schema.json", event, "exported event");
    }
    private List<JsonNode> rows(String channel, String scenario, String variant) throws Exception {
        if (!channels.containsKey(channel)) {
            List<JsonNode> all = new ArrayList<>();
            for (String line : Files.readAllLines(root.resolve(channel))) if (!line.isBlank()) all.add(JsonSupport.MAPPER.readTree(line));
            channels.put(channel, all);
        }
        List<JsonNode> found = new ArrayList<>();
        for (JsonNode n : channels.get(channel)) {
            if (scenario.equals(n.path("scenarioId").asText()) && variant.equals(n.path("variantId").asText())) {
                if (!run.equals(n.path("runId").asText())) throw new IllegalArgumentException("Cross-run evidence");
                found.add(n);
            }
        }
        return found;
    }
}
