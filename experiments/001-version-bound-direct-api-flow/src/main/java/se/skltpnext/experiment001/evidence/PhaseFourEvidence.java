package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.contract.ContractValidators;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Recomputes the Phase 4 oracle from exported observations, without private fixtures or runner pass flags. */
public final class PhaseFourEvidence {
    private final Path root;
    private final String run;
    private final boolean runtime;
    public PhaseFourEvidence(Path root, String run) { this(root, run, false); }
    public PhaseFourEvidence(Path root, String run, boolean runtime) { this.root = root; this.run = run; this.runtime = runtime; }

    public static JsonNode catalog() {
        var catalog = JsonSupport.readResource("experiment-001/scenarios/catalog-phase-4-1.0.0.json");
        JsonSupport.validateResource("experiment-001/schemas/scenario-catalog-phase-4.schema.json", catalog, "Phase 4 catalog");
        var keys = new HashSet<String>();
        for (var entry : catalog.required("variants"))
            if (!keys.add(entry.path("scenarioId").asText() + "/" + entry.path("variantId").asText()))
                throw new IllegalStateException("Duplicate Phase 4 oracle");
        if (!keys.equals(ExperimentConfig.PHASE_4_VARIANTS)) throw new IllegalStateException("Phase 4 completeness mismatch");
        return catalog;
    }

    public Map<String, Boolean> evaluate() {
        Map<String, Boolean> results = new TreeMap<>();
        for (var entry : catalog().required("variants")) {
            String scenario = entry.path("scenarioId").asText(), variant = entry.path("variantId").asText();
            try { results.put(scenario + "/" + variant, matches(scenario, variant, true)); }
            catch (Exception e) { results.put(scenario + "/" + variant, false); }
        }
        return results;
    }

    public boolean timingDistinguishable(String scenario, String variant) throws Exception {
        String expectedDependency = "";
        String expectedReason = "";
        for (var entry : catalog().required("variants"))
            if (scenario.equals(entry.path("scenarioId").asText()) && variant.equals(entry.path("variantId").asText())) {
                expectedDependency = entry.path("dependency").asText(); expectedReason = entry.path("reason").asText();
            }
        for (var event : rows("phase-4/observations.jsonl", scenario, variant)) {
            if (event.path("kind").asText().equals("client-completion")) {
                long duration = event.path("durationMillis").asLong(-1);
                if (duration < 0 || duration > 350 || event.path("result").asText().equals("transport-error")) return false;
                if (event.path("result").asText().equals("timeout") && (duration < 300
                        || !event.path("dependency").asText().equals(expectedDependency) || !expectedReason.equals("timeout"))) return false;
            }
        }
        return true;
    }

    public boolean matches(String scenario, String variant, boolean finalized) throws Exception {
        JsonNode expected = null;
        for (var entry : catalog().required("variants"))
            if (scenario.equals(entry.path("scenarioId").asText()) && variant.equals(entry.path("variantId").asText())) expected = entry;
        if (expected == null) return false;
        var resultFile = root.resolve("results/" + scenario + "--" + variant + ".json");
        var result = JsonSupport.MAPPER.readTree(resultFile.toFile());
        JsonSupport.validateResource("experiment-001/schemas/scenario-result-phase-4.schema.json", result, "Phase 4 result");
        if (!run.equals(result.path("runId").asText()) || !scenario.equals(result.path("scenarioId").asText())
                || !variant.equals(result.path("variantId").asText()) || !result.path("actual").asText().equals("deny")
                || !result.path("expected").asText().equals("deny") || !timingDistinguishable(scenario, variant)) return false;
        var decisions = rows("telemetry/decisions.jsonl", scenario, variant);
        var terminal = decisions.getLast();
        for (String field : List.of("terminalCheckpoint", "reason")) {
            if (!result.path(field).equals(expected.path(field))) return false;
            if (!terminal.path(field.equals("terminalCheckpoint") ? "checkpoint" : field).equals(expected.path(field))) return false;
        }
        if (!terminal.path("result").asText().equals("deny")
                || !terminal.path("category").asText().equals(scenario.equals("E001-CON-002") ? "contract_validation" : "dependency_failure")) return false;
        if (!rows("errors/harness.jsonl", scenario, variant).isEmpty()) return false;
        long businesses = decisions.stream().filter(e -> e.path("checkpoint").asText().equals("producer.business-operation")).count();
        if (businesses != expected.path("businessOperations").asInt()) return false;
        // A deny cannot be followed by authorization, token issuance or business execution.
        boolean denied = false;
        for (var d : decisions) {
            String checkpoint = d.path("checkpoint").asText();
            if (denied && Set.of("producer.business-operation", "producer.authorization", "authorization-server.token-issuance").contains(checkpoint)) return false;
            denied |= d.path("result").asText().equals("deny");
        }
        var network = rows("network/payload-call-ledger.jsonl", scenario, variant);
        int as = expected.path("asCalls").asInt(), producer = expected.path("producerCalls").asInt();
        var receivers = new ArrayList<String>();
        for (int i = 0; i < as; i++) receivers.add("authorization-server");
        for (int i = 0; i < producer; i++) receivers.add("producer-b");
        if (!network.stream().map(n -> n.path("receiver").asText()).toList().equals(receivers)) return false;
        if (network.stream().filter(n -> n.path("apiDataReceived").asBoolean()).count() != businesses) return false;
        for (var n : network) {
            if (!n.path("sender").asText().equals("consumer-a")) return false;
            if (n.path("receiver").asText().equals("authorization-server") && n.path("apiDataReceived").asBoolean()) return false;
        }
        var contracts = rows("contract/validations.jsonl", scenario, variant);
        var expectedContracts = new ArrayList<String>(); expected.required("contracts").forEach(n -> expectedContracts.add(n.asText()));
        if (!contracts.stream().map(n -> n.path("role").asText() + "/" + n.path("phase").asText() + "/" + n.path("result").asText()).toList().equals(expectedContracts)) return false;
        for (var c : contracts) if (!c.path("contractId").asText().equals(ExperimentConfig.CONTRACT_ID)
                || !c.path("contractVersion").asText().equals(ExperimentConfig.CONTRACT_VERSION)
                || !c.path("validator").asText().equals(c.path("role").asText().equals("binding") ? "release-contract-binding-1.0.0" : "kappa-2.0.5")) return false;
        var observations = rows("phase-4/observations.jsonl", scenario, variant);
        for (var e : observations) JsonSupport.validateResource("experiment-001/schemas/observation-phase-4.schema.json", e, "Phase 4 event");
        var attempts = ofKind(observations, "client-attempt");
        var completions = ofKind(observations, "client-completion");
        var dependencies = rows("telemetry/dependencies.jsonl", scenario, variant).stream()
                .filter(e -> Set.of("authorization-server", "producer").contains(e.path("dependency").asText())).toList();
        var deps = receivers.stream().map(v -> v.equals("producer-b") ? "producer" : v).toList();
        for (var events : List.of(attempts, completions, dependencies)) {
            if (!events.stream().map(e -> e.path("dependency").asText()).toList().equals(deps)) return false;
            if (events.stream().anyMatch(e -> e.path("attempts").asInt() != 1)) return false;
        }
        for (int i = 0; i < completions.size(); i++) {
            var c = completions.get(i);
            if (c.path("timeoutMillis").asInt() != 300 || c.path("retryBudgetMillis").asInt() != 350
                    || !c.path("failureLocation").asText().equals("consumer.http." + (c.path("dependency").asText().equals("producer") ? "producer" : "token"))) return false;
            String outcome = !expected.path("dependency").isNull() && c.path("dependency").equals(expected.path("dependency"))
                    ? expected.path("reason").asText() : "success";
            if (!c.path("result").asText().equals(outcome)) return false;
            if (!outcome.equals("success") && !dependencies.get(i).path("result").asText().equals(outcome)) return false;
        }
        var responses = ofKind(observations, "client-response");
        if (expected.path("httpStatus").isNull()) { if (!responses.isEmpty()) return false; }
        else {
            if (responses.size() != 1 || !responses.getFirst().path("httpStatus").equals(expected.path("httpStatus"))
                    || !responses.getFirst().path("internalDetailAbsent").asBoolean()) return false;
            boolean valid = !Set.of("invalid-response", "undocumented-error").contains(variant);
            if (responses.getFirst().path("contractPassed").asBoolean() != valid
                    || responses.getFirst().path("problemTypeDocumented").asBoolean() == variant.equals("undocumented-error")) return false;
        }
        var audits = rows("audit/records.jsonl", scenario, variant);
        int expectedAudits = producer == 1 && !Set.of("invalid-request", "producer-unavailable").contains(variant) ? 1 : 0;
        if (audits.size() != expectedAudits) return false;
        if (audits.isEmpty()) { if (!result.path("auditRef").isNull()) return false; }
        else {
            var audit = audits.getFirst();
            JsonSupport.validateResource("experiment-001/schemas/audit-event-phase-3.schema.json", audit, "audit");
            if (!audit.path("auditRecordId").equals(result.path("auditRef")) || !audit.path("checkpoint").asText().equals("producer.authorization")) return false;
            if (rows("telemetry/spans.jsonl", scenario, variant).stream().anyMatch(s -> s.path("traceId").equals(result.path("auditRef")))) return false;
        }
        if (variant.equals("wrong-contract-version")) {
            var binding = ofKind(observations, "binding");
            if (binding.size() != 1 || !binding.getFirst().path("boundVersion").asText().equals("1.0.0")
                    || !binding.getFirst().path("offeredVersion").asText().equals("2.0.0")
                    || !binding.getFirst().path("contractSha256").asText().equals(JsonSupport.sha256(JsonSupport.readResourceBytes(ContractValidators.CONTRACT_RESOURCE)))) return false;
        }
        if (!finalized) return true;
        var spans = rows("telemetry/spans.jsonl", scenario, variant);
        if (producer == 0 && !spans.isEmpty()) return false;
        if (producer == 1) {
            var clientSpans = spans.stream().filter(e -> e.path("component").asText().equals("consumer")).toList();
            var serverSpans = spans.stream().filter(e -> e.path("component").asText().equals("producer")).toList();
            if (spans.size() != 2 || clientSpans.size() != 1 || serverSpans.size() != 1
                    || !clientSpans.getFirst().path("status").asText().equals("error")
                    || !serverSpans.getFirst().path("traceId").equals(clientSpans.getFirst().path("traceId"))
                    || !serverSpans.getFirst().path("parentSpanId").equals(clientSpans.getFirst().path("spanId"))) return false;
        }
        var finalEvents = ofKind(observations, "finalized"); var drained = ofKind(observations, "drained");
        if (finalEvents.size() != 1 || drained.size() != 1) return false;
        var fin = finalEvents.getFirst(); var drain = drained.getFirst(); String digest = JsonSupport.sha256(resultFile);
        if (!fin.path("beforeSha256").asText().equals(digest) || !drain.path("beforeSha256").asText().equals(digest)
                || !drain.path("afterSha256").asText().equals(digest) || drain.path("scenarioDurationMillis").asLong(5001) > 5000) return false;
        var arrivals = ofKind(observations, "server-arrival"); var ends = ofKind(observations, "server-completion");
        if (!expected.path("fault").asText().equals("NONE")) {
            if (arrivals.size() != 1 || ends.size() != 1) return false;
            for (var e : List.of(arrivals.getFirst(), ends.getFirst()))
                if (!e.path("fault").equals(expected.path("fault"))
                        || !e.path("dependency").asText().equals(expected.path("dependency").isNull() ? "producer" : expected.path("dependency").asText())) return false;
            if (ends.getFirst().path("atEpochMillis").asLong() > drain.path("atEpochMillis").asLong()) return false;
            if (expected.path("fault").asText().equals("SLOW")) {
                var end = ends.getFirst();
                if (end.path("durationMillis").asLong() < 600
                        || end.path("atEpochMillis").asLong() <= fin.path("atEpochMillis").asLong()
                        || !Set.of("late-response-sent", "late-response-disconnected").contains(end.path("result").asText())) return false;
                for (var e : ofKind(observations, "business-operation"))
                    if (e.path("atEpochMillis").asLong() >= completions.getLast().path("atEpochMillis").asLong()) return false;
            } else if (expected.path("fault").asText().equals("UNAVAILABLE") && !ends.getFirst().path("result").asText().equals("unavailable")) return false;
        } else if (!arrivals.isEmpty() || !ends.isEmpty()) return false;
        return ofKind(observations, "business-operation").size() == businesses;
    }

    private static List<JsonNode> ofKind(List<JsonNode> rows, String kind) {
        return rows.stream().filter(e -> e.path("kind").asText().equals(kind)).toList();
    }
    public List<JsonNode> rows(String channel, String scenario, String variant) throws Exception {
        Path file = root.resolve((runtime ? "events/" : "") + channel);
        if (!Files.isRegularFile(file)) return List.of();
        var values = new ArrayList<JsonNode>();
        for (String line : Files.readAllLines(file)) if (!line.isBlank()) {
            var value = JsonSupport.MAPPER.readTree(line);
            if (scenario.equals(value.path("scenarioId").asText()) && variant.equals(value.path("variantId").asText())) {
                if (!run.equals(value.path("runId").asText())) throw new IllegalArgumentException("Wrong observation run");
                values.add(value);
            }
        }
        return values;
    }
}
