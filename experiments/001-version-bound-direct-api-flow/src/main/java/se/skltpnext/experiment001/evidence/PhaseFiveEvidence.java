package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;

/** Independent oracle over closed, exported bytes. No access to the stimulus/decision code. */
public final class PhaseFiveEvidence {
    public static final List<String> CHANNELS = List.of(
            "telemetry/spans.jsonl", "telemetry/decisions.jsonl", "telemetry/dependencies.jsonl",
            "telemetry/metadata.jsonl", "telemetry/discovery.jsonl", "telemetry/transitions.jsonl",
            "telemetry/observations.jsonl", "audit/records.jsonl", "audit/observations.jsonl",
            "contract/validations.jsonl", "network/payload-call-ledger.jsonl",
            "errors/external.jsonl", "errors/harness.jsonl", "errors/http-response.txt",
            "phase-4/observations.jsonl", "results/source.json", "stimulus-result.json",
            "console/runner.log", "console/server.log");
    private final Path root;
    private final Path privateRoot;
    private final String run;
    public PhaseFiveEvidence(Path root, Path privateRoot, String run) {
        this.root = root; this.privateRoot = privateRoot; this.run = run;
    }

    public Map<String, String> evaluate() {
        Map<String, String> results = new TreeMap<>();
        for (String scenario : List.of("E001-OBS-001", "E001-OBS-002"))
            results.put(scenario + "/baseline", evaluateScenario(scenario));
        return results;
    }

    public String evaluateScenario(String scenario) {
        try {
            var catalog = JsonSupport.readResource("experiment-001/scenarios/catalog-phase-5-1.0.0.json");
            JsonSupport.validateResource("experiment-001/schemas/catalog-phase-5.schema.json", catalog, "OBS catalog");
            var expected = catalog.required("scenarios").required(scenario);
            Path directory = root.resolve("phase-5/" + scenario);
            try (var files = Files.list(directory)) {
                if (files.count() != expected.size()) return "inconclusive";
            }
            Set<String> stimuli = new HashSet<>(), decisions = new HashSet<>(), audits = new HashSet<>();
            boolean failed = false;
            for (int i = 0; i < expected.size(); i++) {
                Path capture = directory.resolve(Integer.toString(i + 1));
                var observation = read(capture.resolve("capture.json"));
                JsonSupport.validateResource("experiment-001/schemas/capture-phase-5.schema.json", observation, "OBS capture");
                if (!identity(observation, scenario, expected.get(i)) || !stimuli.add(observation.path("stimulusRef").asText()))
                    return "inconclusive";
                Path registry = privateRoot.resolve("observation-canaries/" + observation.required("stimulusRef").asText() + ".jsonl");
                Set<String> classes = new HashSet<>();
                for (var canary : rows(registry)) classes.add(canary.required("type").asText());
                if (!classes.containsAll(ObservationScanner.CLASSES)) return "inconclusive";
                Set<String> channels = new HashSet<>();
                boolean leaked = false;
                for (var channel : observation.required("channels")) {
                    String name = channel.required("channel").asText();
                    if (!CHANNELS.contains(name) || !channels.add(name)) return "inconclusive";
                    Path file = capture.resolve(name);
                    if (channel.required("hitCount").asLong() > 0 && !channel.path("exported").asBoolean()) {
                        // The failing raw bytes are quarantined under private state, never in the package.
                        Path raw = privateRoot.resolve("observation-quarantine/" + observation.path("stimulusRef").asText()).resolve(name);
                        if (!sameScan(raw, registry, channel)) return "inconclusive";
                        leaked = true;
                    } else {
                        if (!channel.path("exported").asBoolean() || !sameScan(file, registry, channel)) return "inconclusive";
                        leaked |= ObservationScanner.hits(ObservationScanner.scan(file, registry)) > 0;
                        validateChannel(file, name, expected.get(i));
                    }
                }
                if (!channels.equals(new HashSet<>(CHANNELS))) return "inconclusive";
                if (leaked) { failed = true; continue; }
                if (!observation.required("fixtureValid").asBoolean()) return "inconclusive";
                if (!matches(capture, scenario, expected.get(i), observation, decisions, audits)) failed = true;
            }
            return failed ? "fail" : "pass";
        } catch (Exception e) { return "inconclusive"; }
    }

    private boolean sameScan(Path file, Path registry, JsonNode channel) throws Exception {
        if (!Files.isRegularFile(file) || !JsonSupport.sha256(file).equals(channel.required("sha256").asText())) return false;
        var scan = ObservationScanner.scan(file, registry);
        return channel.required("findings").equals(JsonSupport.MAPPER.readTree(JsonSupport.MAPPER.writeValueAsBytes(scan)))
                && channel.required("hitCount").asLong() == ObservationScanner.hits(scan);
    }

    private boolean identity(JsonNode row, String scenario, JsonNode source) {
        return run.equals(row.path("runId").asText()) && scenario.equals(row.path("scenarioId").asText())
                && "baseline".equals(row.path("variantId").asText())
                && source.path("sourceScenarioId").equals(row.path("sourceScenarioId"))
                && source.path("sourceVariantId").equals(row.path("sourceVariantId"));
    }

    private boolean matches(Path capture, String scenario, JsonNode expected, JsonNode observation,
                            Set<String> decisionIds, Set<String> auditIds) throws Exception {
        String source = expected.path("sourceScenarioId").asText(), variant = expected.path("sourceVariantId").asText();
        var raw = rows(capture.resolve("telemetry/decisions.jsonl")).stream()
                .filter(row -> source.equals(row.path("scenarioId").asText()) && variant.equals(row.path("variantId").asText())).toList();
        if (raw.isEmpty()) return false;
        var terminal = raw.getLast();
        for (String field : List.of("checkpoint", "category", "reason"))
            if (!terminal.path(field).equals(expected.path(field))) return false;
        if (!terminal.path("component").equals(expected.path("actor"))) return false;
        if (!terminal.path("result").asText().equals(expected.path("httpStatus").asInt() == 200 ? "allow" : "deny")) return false;
        String external = Files.readString(capture.resolve("errors/http-response.txt"));
        if (expected.path("httpStatus").asInt() != 200 && !external.startsWith(expected.path("httpStatus").asInt() + "\n")) return false;
        if (!rows(capture.resolve("errors/harness.jsonl")).isEmpty()) return false;
        var summary = read(capture.resolve("stimulus-result.json"));
        if (!summary.path("fixtureValid").asBoolean() || !summary.path("stimulusRef").equals(observation.path("stimulusRef"))) return false;
        var sourceResult = read(capture.resolve("results/source.json"));
        if (!run.equals(sourceResult.path("runId").asText())) return false;
        if (sourceResult.has("scenarioId") && (!source.equals(sourceResult.path("scenarioId").asText())
                || !variant.equals(sourceResult.path("variantId").asText()) || !sourceResult.path("status").asText().equals("pass"))) return false;
        var decisionRows = rows(capture.resolve("telemetry/observations.jsonl"));
        var auditRows = rows(capture.resolve("audit/observations.jsonl"));
        var spans = rows(capture.resolve("telemetry/spans.jsonl"));
        if (decisionRows.isEmpty() || decisionRows.size() != auditRows.size()) return false;
        for (var decision : decisionRows) {
            if (!identity(decision, scenario, expected) || !decision.path("stimulusRef").equals(observation.path("stimulusRef"))
                    || !decisionIds.add(decision.path("decisionRef").asText()) || !auditIds.add(decision.path("auditRef").asText())) return false;
            var linked = auditRows.stream().filter(a -> a.path("auditRecordId").equals(decision.path("auditRef"))).toList();
            if (linked.size() != 1) return false;
            var audit = linked.getFirst();
            for (String field : List.of("runId", "scenarioId", "variantId", "stimulusRef", "sourceScenarioId", "sourceVariantId",
                    "actor", "checkpoint", "category", "result", "reason", "releaseId", "releaseVersion", "profileVersion", "policyVersion", "decisionRef"))
                if (!audit.path(field).equals(decision.path(field))) return false;
            if (spans.stream().filter(span -> span.path("traceId").equals(decision.path("traceId"))
                    && span.path("spanId").equals(decision.path("spanId"))
                    && span.path("component").equals(decision.path("actor"))
                    && run.equals(span.path("runId").asText()) && source.equals(span.path("scenarioId").asText())
                    && variant.equals(span.path("variantId").asText())).count() != 1) return false;
            if (raw.stream().noneMatch(r -> r.path("component").equals(decision.path("actor"))
                    && r.path("checkpoint").equals(decision.path("checkpoint")) && r.path("category").equals(decision.path("category"))
                    && r.path("result").equals(decision.path("result")) && r.path("reason").equals(decision.path("reason")))) return false;
        }
        // A complete terminal denial must be represented exactly once in both independent channels.
        if (scenario.equals("E001-OBS-002") && decisionRows.stream().filter(r -> r.path("checkpoint").equals(expected.path("checkpoint"))
                && r.path("category").equals(expected.path("category")) && r.path("result").asText().equals("deny")
                && r.path("policyVersion").equals(expected.path("policyVersion"))).count() != 1) return false;
        if (expected.path("category").asText().equals("authorization")) {
            int token = index(raw, "producer.token-validation"), sender = index(raw, "producer.sender-constraint"), auth = index(raw, "producer.authorization");
            if (token < 0 || sender <= token || auth <= sender || !raw.get(token).path("result").asText().equals("allow")
                    || !raw.get(sender).path("result").asText().equals("allow")) return false;
        }
        return true;
    }

    private static int index(List<JsonNode> rows, String checkpoint) {
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).path("checkpoint").asText().equals(checkpoint)) return i;
        return -1;
    }

    private void validateChannel(Path file, String name, JsonNode source) throws Exception {
        if (!name.endsWith("json") && !name.endsWith("jsonl")) return;
        String schema = switch (name) {
            case "telemetry/observations.jsonl" -> "decision-phase-5";
            case "audit/observations.jsonl" -> "audit-phase-5";
            case "audit/records.jsonl" -> "audit-event-phase-3";
            case "telemetry/metadata.jsonl" -> "metadata-event-phase-3";
            case "telemetry/discovery.jsonl" -> "discovery-event-phase-3";
            case "telemetry/transitions.jsonl" -> "transition-event-phase-3";
            case "phase-4/observations.jsonl" -> "observation-phase-4";
            case "stimulus-result.json" -> "stimulus-result-phase-5";
            case "results/source.json" -> switch (source.path("sourceScenarioId").asText()) {
                case "E001-FLOW-001" -> "scenario-result";
                case "E001-CON-002" -> "scenario-result-phase-4";
                case "E001-TOK-001", "E001-SEC-002", "E001-AUTHZ-001" -> "scenario-result-phase-2";
                default -> "stimulus-result-phase-5";
            };
            default -> "channel-" + name.replace('/', '-').replace(".jsonl", "").replace(".json", "") + "-phase-5";
        };
        var records = name.endsWith("jsonl") ? rows(file) : List.of(read(file));
        for (var row : records) JsonSupport.validateResource("experiment-001/schemas/" + schema + ".schema.json", row, "OBS channel");
    }

    public static JsonNode read(Path path) throws Exception { return JsonSupport.MAPPER.readTree(Files.readAllBytes(path)); }
    public static List<JsonNode> rows(Path path) throws Exception {
        List<JsonNode> rows = new ArrayList<>();
        for (String line : Files.readAllLines(path)) if (!line.isBlank()) rows.add(JsonSupport.MAPPER.readTree(line));
        return rows;
    }
}
