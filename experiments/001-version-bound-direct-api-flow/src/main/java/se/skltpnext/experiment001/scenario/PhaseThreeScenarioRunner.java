package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.consumer.Consumer;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.metadata.MutableExperimentClock;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class PhaseThreeScenarioRunner {
    private final Path root;
    private final String run;
    PhaseThreeScenarioRunner(Path root, String run) { this.root = root; this.run = run; }

    static void restoreMetadata(Path root, RuntimeEnvironment.EnvironmentInfo environment) {
        try (var paths = Files.walk(root.resolve("metadata"))) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot reset metadata", e); }
        var material = CryptoMaterial.load(root);
        MetadataStores.writeBaseline(root, environment.authorizationServerEndpoint(), environment.producerEndpoint(), material);
        var stores = new MetadataStores(root);
        var next = stores.nextRevision("service");
        var entry = (ObjectNode) next.required("entries").get(0);
        entry.put("endpointId", "PRODUCER-ENDPOINT-REV-2"); entry.put("endpointRevision", 2);
        entry.put("endpointUri", environment.secondProducerEndpoint().toString());
        MetadataStores.writeSigned(root, "service", next, material.metadataKey());
    }

    ScenarioEngine.ScenarioResult run(String scenario, String variant) {
        try (var telemetry = new TelemetryRecorder(root, run, scenario, variant, "consumer")) {
            var stores = new MetadataStores(root, "consumer", telemetry);
            var clock = new MutableExperimentClock(root);
            var consumer = new Consumer(root, run);
            if (scenario.equals("E001-LIFE-001")) return lifecycle(scenario, variant, stores, consumer, clock);
            if (scenario.equals("E001-META-002")) return staleMetadata(scenario, variant, stores, consumer, clock);
            if (variant.equals("iam-as-signing-key-revoked-after-bound")) return revokeSigningKey(scenario, variant, stores, consumer, clock);
            if (scenario.equals("E001-META-001")) return metadataFailure(scenario, variant, stores, consumer);
            if (scenario.equals("E001-DIS-003")) return discoveryFailure(scenario, variant, stores, consumer);
            Map<String, String> before = immutableInputs();
            var first = stores.discover();
            consumer.execute(scenario, variant, first);
            stores.activate("service", 2);
            var activated = clock.instant();
            clock.advance(Duration.ofMillis(1000));
            var second = stores.discover();
            Map<String, Object> transition = new java.util.LinkedHashMap<>();
            transition.put("runId", run); transition.put("scenarioId", scenario); transition.put("variantId", variant);
            transition.put("family", "service"); transition.put("oldRevision", 1); transition.put("newRevision", 2);
            transition.put("activatedAt", activated.toString());
            transition.put("observedAt", clock.instant().toString()); transition.put("elapsedMillis", Duration.between(activated, clock.instant()).toMillis());
            transition.put("boundMillis", 1000); transition.put("beforeDigests", before); transition.put("afterDigests", immutableInputs());
            JsonSupport.validate(JsonSupport.readResource("experiment-001/schemas/transition-event-phase-3.schema.json"),
                    JsonSupport.MAPPER.valueToTree(transition), "transition");
            JsonSupport.appendJsonLine(root.resolve("events/telemetry/transitions.jsonl"), transition);
            consumer.execute(scenario, variant, second);
            var calls = events(root, "network/payload-call-ledger.jsonl", scenario, variant).stream()
                    .filter(n -> n.path("receiver").asText().equals("producer-b")).toList();
            boolean pass = first.endpointRevision() == 1 && second.endpointRevision() == 2
                    && !first.producerEndpoint().equals(second.producerEndpoint())
                    && before.equals(immutableInputs())
                    && calls.size() == 2
                    && calls.get(0).path("listenerId").asText().equals(first.endpointId())
                    && calls.get(1).path("listenerId").asText().equals(second.endpointId())
                    && calls.stream().allMatch(n -> n.path("apiDataReceived").asBoolean());
            return new ScenarioEngine.ScenarioResult(scenario, variant, "allow", pass ? "pass" : "fail",
                    List.of("discovery.revision-1", "discovery.revision-2", "network.two-receivers", "immutable-inputs.equal"),
                    null, "pass", "pending-collection", null);
        }
    }

    private ScenarioEngine.ScenarioResult lifecycle(String scenario, String variant, MetadataStores stores,
                                                    Consumer consumer, MutableExperimentClock clock) {
        var discovery = stores.discover();
        var material = CryptoMaterial.load(root);
        boolean existing = variant.equals("inactive-A-existing-token-after-offboarding");
        boolean newRequest = variant.equals("inactive-A-token-request-after-offboarding");
        boolean unpublished = variant.equals("unpublished-service");
        Consumer.IssuedToken token = null;
        if (existing || newRequest) {
            token = consumer.obtainToken(scenario, variant, discovery, Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
            if (existing && consumer.callResource(scenario, variant, discovery, "DPoP", token.value(), material.dpopKey(), "consumer-a").status() != 200)
                throw new IllegalStateException("Lifecycle positive control failed");
        }
        String family = unpublished ? "service" : "membership";
        var next = stores.nextRevision(family);
        if (unpublished) next.put("status", "unpublished");
        else ((ObjectNode) next.required("members").get(existing || newRequest ? 0 : 1)).put("status", "inactive");
        MetadataStores.writeSigned(root, family, next, CryptoMaterial.metadataSigningKey(root, family));
        stores.activate(family, 2);
        var activated = clock.instant();
        clock.advance(Duration.ofMillis(1000));
        boolean technicalValid = false;
        if (token != null) {
            try {
                new se.skltpnext.experiment001.authorization.AccessTokenValidator(discovery.issuer(),
                        discovery.authorizationServerSigningPublicKey()).validate(token.value());
                technicalValid = true;
            } catch (Exception e) { throw new IllegalStateException("Existing token control invalid", e); }
        }
        String actual = "allow";
        String terminal;
        String reason;
        boolean contract = false;
        if (existing) {
            var response = consumer.callResource(scenario, variant, discovery, "DPoP", token.value(), material.dpopKey(), "consumer-a");
            actual = response.status() == 200 ? "allow" : "deny";
            contract = response.contractValidated() && response.status() == 403;
            terminal = "producer.authorization"; reason = "membership-inactive";
        } else if (newRequest) {
            try { consumer.obtainToken(scenario, variant, discovery, Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ); }
            catch (Consumer.TokenRequestDenied e) { if (e.status() == 403) actual = "deny"; }
            terminal = "authorization-server.membership"; reason = "consumer-inactive";
        } else {
            terminal = unpublished ? "discovery.resolved" : "membership.producer";
            reason = unpublished ? "missing-endpoint" : "producer-inactive";
            try { consumer.execute(scenario, variant, stores.discover()); }
            catch (MetadataStores.MetadataFailure e) { if (e.reason().equals(reason)) actual = "deny"; }
            if (unpublished) {
                // The accepted tombstone must survive loss of its source; cached rev 1 cannot reappear.
                stores.sourceAvailable("service", false);
                try { consumer.execute(scenario, variant, stores.discover()); actual = "allow"; }
                catch (MetadataStores.MetadataFailure e) { if (!e.reason().equals("missing-endpoint")) throw e; }
            }
        }
        recordTransition(scenario, variant, family, activated, clock.instant(), technicalValid);
        var decisions = events(root, "telemetry/decisions.jsonl", scenario, variant);
        var last = decisions.getLast();
        var calls = events(root, "network/payload-call-ledger.jsonl", scenario, variant);
        var audits = events(root, "audit/records.jsonl", scenario, variant);
        boolean pass = actual.equals("deny") && last.path("checkpoint").asText().equals(terminal)
                && last.path("result").asText().equals("deny") && last.path("reason").asText().equals(reason)
                && calls.size() == (existing ? 3 : newRequest ? 2 : 0)
                && (calls.isEmpty() || !calls.getLast().path("apiDataReceived").asBoolean())
                && (!existing || contract)
                && (!(existing || newRequest) || technicalValid);
        return new ScenarioEngine.ScenarioResult(scenario, variant, actual, pass ? "pass" : "fail",
                List.of(terminal, reason), audits.isEmpty() ? null : audits.getLast().path("auditRecordId").asText(),
                contract ? "pass" : "not-applicable", "pending-collection", null);
    }

    private ScenarioEngine.ScenarioResult staleMetadata(String scenario, String variant, MetadataStores stores,
                                                       Consumer consumer, MutableExperimentClock clock) {
        String family = variant.split("-", 2)[0];
        stores.discover();
        stores.sourceAvailable(family, false);
        long max = family.equals("service") ? 60000 : 30000;
        clock.advance(Duration.ofMillis(max));
        // Boundary is inclusive, refetching the same revision cannot reset its signed age.
        stores.readAndValidate(family);
        clock.advance(Duration.ofMillis(1));
        // Keep other families independently fresh so they cannot hide the target family's denial.
        for (String other : List.of("service", "membership", "iam")) if (!other.equals(family)) {
            var next = stores.nextRevision(other);
            MetadataStores.writeSigned(root, other, next, CryptoMaterial.metadataSigningKey(root, other));
            stores.activate(other, 2);
        }
        String actual = "allow";
        String observedFamily = "none";
        String reason = "unexpected-allow";
        try { consumer.execute(scenario, variant, stores.discover()); }
        catch (MetadataStores.MetadataFailure e) { actual = "deny"; observedFamily = e.family(); reason = e.reason(); }
        var last = events(root, "telemetry/metadata.jsonl", scenario, variant).getLast();
        boolean pass = actual.equals("deny") && observedFamily.equals(family) && reason.equals("stale")
                && last.path("ageMillis").asLong() == max + 1
                && last.path("maxStalenessMillis").asLong() == max
                && last.path("ttlMillis").asLong() == max / 2
                && events(root, "network/payload-call-ledger.jsonl", scenario, variant).isEmpty();
        return new ScenarioEngine.ScenarioResult(scenario, variant, actual, pass ? "pass" : "fail",
                List.of("metadata." + observedFamily, reason), null, "not-applicable", "pending-collection", null);
    }

    private ScenarioEngine.ScenarioResult revokeSigningKey(String scenario, String variant, MetadataStores stores,
                                                            Consumer consumer, MutableExperimentClock clock) {
        var discovery = stores.discover();
        var token = consumer.obtainToken(scenario, variant, discovery, Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
        var keys = CryptoMaterial.load(root);
        var initial = consumer.callResource(scenario, variant, discovery, "DPoP", token.value(), keys.dpopKey(), "consumer-a");
        if (initial.status() != 200) throw new IllegalStateException("Revocation positive control failed");
        var next = stores.nextRevision("iam");
        next.put("authorizationServerSigningKeyStatus", "revoked");
        MetadataStores.writeSigned(root, "iam", next, CryptoMaterial.metadataSigningKey(root, "iam"));
        stores.activate("iam", 2);
        var activated = clock.instant();
        clock.advance(Duration.ofMillis(1000));
        boolean cryptographicallyValid;
        try {
            new se.skltpnext.experiment001.authorization.AccessTokenValidator(discovery.issuer(),
                    discovery.authorizationServerSigningPublicKey()).validate(token.value());
            cryptographicallyValid = true;
        } catch (Exception e) { cryptographicallyValid = false; }
        var response = consumer.callResource(scenario, variant, discovery, "DPoP", token.value(), keys.dpopKey(), "consumer-a");
        recordTransition(scenario, variant, "iam", activated, clock.instant(), cryptographicallyValid);
        var decisions = events(root, "telemetry/decisions.jsonl", scenario, variant);
        var last = decisions.getLast();
        var calls = events(root, "network/payload-call-ledger.jsonl", scenario, variant);
        boolean pass = cryptographicallyValid && response.status() == 401 && response.contractValidated()
                && last.path("checkpoint").asText().equals("producer.token-validation")
                && last.path("result").asText().equals("deny") && last.path("reason").asText().equals("signing-key-revoked")
                && calls.size() == 3 && !calls.getLast().path("apiDataReceived").asBoolean();
        return new ScenarioEngine.ScenarioResult(scenario, variant, response.status() == 200 ? "allow" : "deny",
                pass ? "pass" : "fail", List.of("producer.token-validation", "signing-key-revoked"), null,
                "pass", "pending-collection", null);
    }

    private void recordTransition(String scenario, String variant, String family, java.time.Instant activated,
                                  java.time.Instant observed, boolean technicalTokenValid) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("runId", run); event.put("scenarioId", scenario); event.put("variantId", variant);
        event.put("family", family); event.put("oldRevision", 1); event.put("newRevision", 2);
        event.put("activatedAt", activated.toString()); event.put("observedAt", observed.toString());
        event.put("elapsedMillis", Duration.between(activated, observed).toMillis()); event.put("boundMillis", 1000);
        event.put("technicalTokenValid", technicalTokenValid);
        JsonSupport.validate(JsonSupport.readResource("experiment-001/schemas/transition-event-phase-3.schema.json"),
                JsonSupport.MAPPER.valueToTree(event), "transition");
        JsonSupport.appendJsonLine(root.resolve("events/telemetry/transitions.jsonl"), event);
    }

    private ScenarioEngine.ScenarioResult metadataFailure(String scenario, String variant,
                                                           MetadataStores stores, Consumer consumer) {
        var expected = JsonSupport.readResource("experiment-001/scenarios/metadata-faults-phase-3-1.0.0.json").required(variant);
        stores.discover();
        MetadataFaultFixtures.apply(root, stores, variant);
        String actual = "allow";
        String family = "none";
        String reason = "unexpected-allow";
        try { consumer.execute(scenario, variant, stores.discover()); }
        catch (MetadataStores.MetadataFailure e) { actual = "deny"; family = e.family(); reason = e.reason(); }
        var observations = events(root, "telemetry/metadata.jsonl", scenario, variant);
        var last = observations.getLast();
        boolean pass = actual.equals("deny") && expected.required("reason").asText().equals(reason)
                && expected.required("family").asText().equals(family)
                && last.path("family").asText().equals(family) && last.path("result").asText().equals("deny")
                && events(root, "network/payload-call-ledger.jsonl", scenario, variant).isEmpty();
        return new ScenarioEngine.ScenarioResult(scenario, variant, actual, pass ? "pass" : "fail",
                List.of("metadata." + family, reason), null, "not-applicable", "pending-collection", null);
    }

    private ScenarioEngine.ScenarioResult discoveryFailure(String scenario, String variant,
                                                            MetadataStores stores, Consumer consumer) {
        ObjectNode next = stores.nextRevision("service");
        var entries = (com.fasterxml.jackson.databind.node.ArrayNode) next.required("entries");
        if (variant.equals("missing-endpoint")) entries.removeAll();
        else entries.add(entries.get(0).deepCopy());
        MetadataStores.writeSigned(root, "service", next, CryptoMaterial.load(root).metadataKey());
        stores.activate("service", 2);
        String actual = "allow";
        String reason = "unexpected-allow";
        try { consumer.execute(scenario, variant, stores.discover()); }
        catch (MetadataStores.MetadataFailure e) { actual = "deny"; reason = e.reason(); }
        var observed = events(root, "telemetry/discovery.jsonl", scenario, variant);
        boolean pass = actual.equals("deny") && reason.equals(variant)
                && observed.size() == 1
                && observed.getFirst().path("candidateCount").asInt() == (variant.equals("missing-endpoint") ? 0 : 2)
                && observed.getFirst().path("endpointId").isNull()
                && events(root, "network/payload-call-ledger.jsonl", scenario, variant).isEmpty()
                && events(root, "telemetry/dependencies.jsonl", scenario, variant).stream()
                    .noneMatch(n -> List.of("authorization-server", "producer").contains(n.path("dependency").asText()));
        return new ScenarioEngine.ScenarioResult(scenario, variant, actual, pass ? "pass" : "fail",
                List.of("discovery.resolved", reason), null, "not-applicable", "pending-collection", null);
    }

    private Map<String, String> immutableInputs() {
        var values = new java.util.TreeMap<>(CryptoMaterial.load(root).publicFingerprints());
        values.putAll(CryptoMaterial.metadataPublicFingerprints(root));
        values.put("release", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/release/index-1.0.0.json")));
        values.put("contract", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/contracts/read-api-1.0.0.openapi.json")));
        try (var bytes = Consumer.class.getResourceAsStream("Consumer.class")) {
            values.put("consumerCode", JsonSupport.sha256(bytes.readAllBytes()));
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot fingerprint consumer", e); }
        return values;
    }

    static List<JsonNode> events(Path root, String channel, String scenario, String variant) {
        Path file = root.resolve("events/" + channel);
        if (!Files.isRegularFile(file)) return List.of();
        try {
            var nodes = new java.util.ArrayList<JsonNode>();
            for (String line : Files.readAllLines(file)) if (!line.isBlank()) {
                var n = JsonSupport.MAPPER.readTree(line);
                if (scenario.equals(n.path("scenarioId").asText()) && variant.equals(n.path("variantId").asText())) nodes.add(n);
            }
            return nodes;
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read scenario evidence", e); }
    }
}
