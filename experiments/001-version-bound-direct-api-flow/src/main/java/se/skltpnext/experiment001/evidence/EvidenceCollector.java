package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import se.skltpnext.experiment001.ExperimentConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class EvidenceCollector {
    private static final List<String> PHASE_1_RESULTS = List.of(
            "E001-REL-001--valid.json",
            "E001-DIS-001--baseline.json",
            "E001-FLOW-001--baseline.json",
            "E001-CON-001--baseline.json");
    private static final List<String> PHASE_2_RESULTS = List.of(
            "E001-FLOW-002--baseline.json",
            "E001-SEC-001--baseline.json",
            "E001-SEC-002--baseline.json",
            "E001-AUTHZ-001--insufficient-scope.json",
            "E001-AUTHZ-001--local-policy-deny.json",
            "E001-TOK-001--missing.json",
            "E001-TOK-001--wrong-issuer.json",
            "E001-TOK-001--wrong-audience.json",
            "E001-TOK-001--bad-signature.json",
            "E001-TOK-001--disallowed-algorithm.json",
            "E001-TOK-001--wrong-type.json",
            "E001-TOK-001--expired.json",
            "E001-TOK-001--not-yet-valid.json",
            "E001-TOK-001--missing-required-claim.json",
            "E001-TOK-001--wrong-client-id.json",
            "E001-TOK-001--wrong-sub.json");
    private static final List<String> PHASE_3_RESULTS = ExperimentConfig.PHASE_3_VARIANTS.stream()
            .sorted().map(key -> key.replace("/", "--") + ".json").toList();
    private static final List<String> PHASE_4_RESULTS = ExperimentConfig.PHASE_4_VARIANTS.stream()
            .sorted().map(key -> key.replace("/", "--") + ".json").toList();
    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "access_token", "client_assertion", "dpop_proof", "private_key",
            "raw_claims", "api_payload", "authorization_header");
    private final Path runtimeRoot;
    private final Path evidenceRoot;
    private final String runId;

    public EvidenceCollector(Path runtimeRoot, Path evidenceRoot, String runId) {
        this.runtimeRoot = runtimeRoot;
        this.evidenceRoot = evidenceRoot;
        this.runId = runId;
    }

    public CollectionResult collect() {
        try {
            int phase = highestPresentPhase();
            List<String> expectedResults = expectedResults(phase);
            if (Files.exists(evidenceRoot)) {
                deleteTree(evidenceRoot);
            }
            Files.createDirectories(evidenceRoot);
            copyResults(expectedResults);
            copyOrCreate("events/telemetry/spans.jsonl", "telemetry/spans.jsonl");
            if (phase >= 3) {
                JsonSupport.writeJson(evidenceRoot.resolve("metadata-authority-fingerprints.json"),
                        se.skltpnext.experiment001.authorization.CryptoMaterial.metadataPublicFingerprints(runtimeRoot));
                copyRequired("console.log", "console/captured.log");
                copyRequired("events/telemetry/metadata.jsonl", "telemetry/metadata.jsonl");
                copyRequired("events/telemetry/discovery.jsonl", "telemetry/discovery.jsonl");
                copyRequired("events/telemetry/transitions.jsonl", "telemetry/transitions.jsonl");
            }
            if (phase >= 4) copyRequired("events/phase-4/observations.jsonl", "phase-4/observations.jsonl");
            copyOrCreate("events/telemetry/decisions.jsonl", "telemetry/decisions.jsonl");
            copyOrCreate("events/telemetry/dependencies.jsonl", "telemetry/dependencies.jsonl");
            copyOrCreate("events/audit/records.jsonl", "audit/records.jsonl");
            copyOrCreate("events/contract/validations.jsonl", "contract/validations.jsonl");
            copyOrCreate("events/network/payload-call-ledger.jsonl", "network/payload-call-ledger.jsonl");
            copyOrCreate("events/errors/external.jsonl", "errors/external.jsonl");
            copyOrCreate("events/errors/harness.jsonl", "errors/harness.jsonl");
            copyRequired("validation/tool-gates.json", "validation/tool-gates.json");
            Files.copy(runtimeRoot.resolve("public-trust/tls-fingerprints.json"),
                    evidenceRoot.resolve("tls-fingerprints.json"), StandardCopyOption.REPLACE_EXISTING);
            JsonSupport.writeJson(evidenceRoot.resolve("jwk-fingerprints.json"),
                    se.skltpnext.experiment001.authorization.CryptoMaterial.load(runtimeRoot)
                            .publicFingerprints());
            JsonSupport.writeJson(evidenceRoot.resolve("parameters.json"),
                    JsonSupport.readResource("experiment-001/profiles/parameters-1.0.0.json"));
            JsonSupport.writeJson(evidenceRoot.resolve("artifact-digests.json"), artifactDigests());
            JsonSupport.writeJson(evidenceRoot.resolve("versions.json"), versions());

            Map<String, Boolean> phaseThree = phase >= 3 ? new PhaseThreeEvidence(evidenceRoot, runId).evaluate() : Map.of();
            boolean observationsPass = phaseThree.values().stream().allMatch(Boolean::booleanValue);
            if (phase >= 3) JsonSupport.writeJson(evidenceRoot.resolve("validation/phase-3.json"), phaseThree);
            Map<String, Boolean> phaseFour = phase >= 4 ? new PhaseFourEvidence(evidenceRoot, runId).evaluate() : Map.of();
            observationsPass &= phaseFour.values().stream().allMatch(Boolean::booleanValue);
            if (phase >= 4) JsonSupport.writeJson(evidenceRoot.resolve("validation/phase-4.json"), phaseFour);
            LeakageResult leakage = scan(false);
            JsonSupport.writeJson(evidenceRoot.resolve("leakage/report.json"), leakage.toMap());
            JsonSupport.writeJson(evidenceRoot.resolve("completeness.json"),
                    completeness(leakage, phase, expectedResults));

            boolean resultsPass = expectedResults.stream().allMatch(name ->
                    "pass".equals(read(evidenceRoot.resolve("results").resolve(name))
                            .required("status").textValue()));
            boolean gatesPass = "pass".equals(read(evidenceRoot.resolve("validation/tool-gates.json"))
                    .required("status").textValue());
            boolean directLedgerPass = directLedgerPass(phase);
            String status = resultsPass && gatesPass && observationsPass && leakage.passed() && directLedgerPass
                    ? "pass" : "inconclusive";

            if (phase >= 4) {
                boolean failed = PHASE_4_RESULTS.stream().anyMatch(name -> "fail".equals(
                        read(evidenceRoot.resolve("results").resolve(name)).path("status").asText()));
                if (failed) status = "fail";
                JsonSupport.writeJson(evidenceRoot.resolve("classification.json"), Map.of(
                        "scope", "phase-1-through-4", "phaseFourResult", failed ? "falsified" : status.equals("pass") ? "verified" : "inconclusive",
                        "experiment001", "not-classified", "observationsRef", "validation/phase-4.json"));
            } else if (phase >= 3) JsonSupport.writeJson(evidenceRoot.resolve("classification.json"), Map.of(
                    "scope", "phase-1-through-3", "phaseThreeResult", status.equals("pass") ? "verified" : "inconclusive",
                    "experiment001", "not-classified", "observationsRef", "validation/phase-3.json"));
            List<Map<String, String>> fileEntries = evidenceFiles().stream()
                    .filter(path -> !path.equals(evidenceRoot.resolve("manifest.json")))
                    .filter(path -> !path.equals(evidenceRoot.resolve("SHA256SUMS")))
                    .map(path -> Map.of(
                            "path", evidenceRoot.relativize(path).toString().replace('\\', '/'),
                            "sha256", JsonSupport.sha256(path)))
                    .toList();
            ObjectNode manifest = JsonSupport.MAPPER.createObjectNode();
            manifest.put("schemaVersion", schemaVersionForPhase(phase));
            manifest.put("runId", runId);
            manifest.put("phase", "phase-" + phase);
            manifest.put("status", status);
            manifest.put("sourceCommit", git("rev-parse", "HEAD").trim());
            manifest.put("gitStatusClass", git("status", "--porcelain").isBlank()
                    ? "clean" : "phase-" + phase + "-working-tree");
            ArrayNode files = manifest.putArray("files");
            fileEntries.forEach(entry -> {
                ObjectNode file = files.addObject();
                file.put("path", entry.get("path"));
                file.put("sha256", entry.get("sha256"));
            });
            JsonSupport.validate(JsonSupport.readResource(manifestSchemaForPhase(phase)),
                    manifest, "evidence manifest");
            JsonSupport.writeJson(evidenceRoot.resolve("manifest.json"), manifest);
            writeChecksums();
            return new CollectionResult(status, leakage.passed(), directLedgerPass, evidenceRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot collect evidence package", e);
        }
    }

    public ValidationResult validate() {
        try { return validatePackage(); }
        catch (Exception e) { return new ValidationResult("inconclusive", false, false, false); }
    }

    private ValidationResult validatePackage() {
        JsonNode manifest = read(evidenceRoot.resolve("manifest.json"));
        int phase = phaseFromManifest(manifest);
        if (!runId.equals(manifest.path("runId").asText())) return new ValidationResult("inconclusive", false, false, false);
        JsonSupport.validate(JsonSupport.readResource(manifestSchemaForPhase(phase)),
                manifest, "evidence manifest");
        for (String name : expectedResults(phase)) {
            JsonNode result = read(evidenceRoot.resolve("results").resolve(name));
            JsonSupport.validate(JsonSupport.readResource(resultSchemaFor(name)),
                    result, "scenario result " + name);
            if (!runId.equals(result.path("runId").asText())
                    || !name.equals(result.path("scenarioId").asText() + "--" + result.path("variantId").asText() + ".json")
                    || !"pass".equals(result.required("status").textValue())) {
                return new ValidationResult("inconclusive", false, false, false);
            }
        }
        boolean checksums = verifyChecksums();
        boolean manifestEntries = verifyManifestEntries(manifest);
        LeakageResult leakage = scan(true);
        boolean ledger = directLedgerPass(phase);
        boolean noPrivate = evidenceFiles().stream().noneMatch(path ->
                evidenceRoot.relativize(path).toString().replace('\\', '/').matches("(?i)(.*[/])?private([/].*)?")
                        || path.getFileName().toString().matches("(?i).*(\\.p12|\\.jwk(?:\\.json)?|\\.pem|\\.key|keystore|passwords?)"));
        boolean observations = phase < 3 || new PhaseThreeEvidence(evidenceRoot, runId).evaluate().values().stream().allMatch(Boolean::booleanValue);
        observations &= phase < 4 || new PhaseFourEvidence(evidenceRoot, runId).evaluate().values().stream().allMatch(Boolean::booleanValue);
        if (phase >= 4) {
            var classification = read(evidenceRoot.resolve("classification.json"));
            observations &= "verified".equals(classification.path("phaseFourResult").asText())
                    && "not-classified".equals(classification.path("experiment001").asText())
                    && "phase-1-through-4".equals(classification.path("scope").asText())
                    && read(evidenceRoot.resolve("validation/phase-4.json")).equals(JsonSupport.MAPPER.valueToTree(
                            new PhaseFourEvidence(evidenceRoot, runId).evaluate()));
        }
        boolean complete = completenessMatches(phase);
        boolean passed = observations && complete && checksums && manifestEntries && leakage.passed() && ledger && noPrivate
                && "pass".equals(manifest.required("status").textValue());
        return new ValidationResult(passed ? "pass" : "inconclusive",
                checksums, leakage.passed(), ledger);
    }

    private void copyResults(List<String> expectedResults) throws IOException {
        Path source = runtimeRoot.resolve("results");
        for (String name : expectedResults) {
            if (!Files.isRegularFile(source.resolve(name))) {
                throw new IllegalStateException("Missing required scenario result " + name);
            }
            Path target = evidenceRoot.resolve("results").resolve(name);
            Files.createDirectories(target.getParent());
            Files.copy(source.resolve(name), target, StandardCopyOption.REPLACE_EXISTING);
            JsonSupport.validateResource(resultSchemaFor(name), read(target), "collected result");
        }
    }

    private void copyRequired(String sourceRelative, String targetRelative) throws IOException {
        Path source = runtimeRoot.resolve(sourceRelative);
        if (!Files.isRegularFile(source)) {
            throw new IllegalStateException("Missing required evidence source " + sourceRelative);
        }
        Path target = evidenceRoot.resolve(targetRelative);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyOrCreate(String sourceRelative, String targetRelative) throws IOException {
        Path source = runtimeRoot.resolve(sourceRelative);
        Path target = evidenceRoot.resolve(targetRelative);
        Files.createDirectories(target.getParent());
        if (Files.isRegularFile(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.writeString(target, "", StandardCharsets.UTF_8);
        }
    }

    private Map<String, Object> versions() {
        Map<String, Object> versions = new LinkedHashMap<>();
        versions.put("jdkRuntime", System.getProperty("java.runtime.version"));
        versions.put("jdkVendor", System.getProperty("java.vendor"));
        versions.put("jdkDistributionSha256LinuxX64",
                "e58fcdcd637b25c03ca84cbbcefc70d11efb8f4b4cbd05decc9f661769d77f94");
        versions.put("maven", "3.9.16");
        versions.put("mavenWrapper", "3.3.4");
        versions.put("plugins", Map.of(
                "clean", "3.5.0", "resources", "3.5.0", "compiler", "3.15.0",
                "jar", "3.5.0", "assembly", "3.8.0", "enforcer", "3.6.2",
                "surefire", "3.5.6"));
        versions.put("os", Map.of(
                "name", System.getProperty("os.name"),
                "version", System.getProperty("os.version"),
                "arch", System.getProperty("os.arch")));
        versions.put("resolvedRuntimeArtifacts", resolvedArtifacts());
        return versions;
    }

    private Map<String, Object> artifactDigests() {
        JsonNode releaseIndex = JsonSupport.readResource("experiment-001/release/index-1.0.0.json");
        Map<String, String> schemas = new TreeMap<>();
        for (String schema : List.of(
                "scenario-result-phase-4.schema.json", "evidence-manifest-phase-4.schema.json",
                "scenario-catalog-phase-4.schema.json", "observation-phase-4.schema.json",
                "release-index.schema.json",
                "scenario-result.schema.json",
                "scenario-result-phase-2.schema.json",
                "scenario-result-phase-3-1.1.0.schema.json",
                "evidence-manifest.schema.json",
                "evidence-manifest-phase-2.schema.json",
                "evidence-manifest-phase-3-1.1.0.schema.json",
                "scenario-catalog-phase-2.schema.json",
                "security-errors-phase-2.schema.json",
                "service-metadata.schema.json",
                "membership-metadata.schema.json",
                "iam-metadata.schema.json",
                "service-metadata-phase-3.schema.json", "membership-metadata-phase-3.schema.json", "iam-metadata-phase-3.schema.json",
                "metadata-event-phase-3.schema.json", "discovery-event-phase-3.schema.json", "transition-event-phase-3.schema.json",
                "scenario-catalog-phase-3.schema.json", "audit-event-phase-3.schema.json")) {
            schemas.put(schema, JsonSupport.sha256(JsonSupport.readResourceBytes(
                    "experiment-001/schemas/" + schema)));
        }
        return Map.of(
                "releaseIndexSha256", JsonSupport.sha256(JsonSupport.readResourceBytes(
                        "experiment-001/release/index-1.0.0.json")),
                "releaseReferences", releaseIndex.required("references"),
                "schemas", schemas,
                "phaseFourScenarioCatalogSha256", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/scenarios/catalog-phase-4-1.0.0.json")),
                "phaseFourContractFaultsSha256", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/scenarios/contract-faults-phase-4-1.0.0.json")),
                "phaseThreeScenarioCatalogSha256", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/scenarios/catalog-phase-3-1.0.0.json")),
                "phaseThreeMetadataFaultsSha256", JsonSupport.sha256(JsonSupport.readResourceBytes("experiment-001/scenarios/metadata-faults-phase-3-1.0.0.json")),
                "phaseTwoScenarioCatalogSha256", JsonSupport.sha256(JsonSupport.readResourceBytes(
                        "experiment-001/scenarios/catalog-phase-2-1.0.0.json")),
                "phaseTwoSecurityErrorsSha256", JsonSupport.sha256(JsonSupport.readResourceBytes(
                        "experiment-001/profiles/security-errors-phase-2-1.0.0.json")));
    }

    private List<String> resolvedArtifacts() {
        try {
            URI location = EvidenceCollector.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jar = Path.of(location);
            if (!Files.isRegularFile(jar)) {
                return List.of("classes-directory; use packaged CLI for final evidence");
            }
            Set<String> values = new java.util.TreeSet<>();
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.getName().startsWith("META-INF/maven/")
                            || !entry.getName().endsWith("/pom.properties")) {
                        continue;
                    }
                    Properties properties = new Properties();
                    try (InputStream input = zip.getInputStream(entry)) {
                        properties.load(input);
                    }
                    values.add(properties.getProperty("groupId") + ":"
                            + properties.getProperty("artifactId") + ":"
                            + properties.getProperty("version"));
                }
            }
            return List.copyOf(values);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enumerate packaged dependency versions", e);
        }
    }

    private LeakageResult scan(boolean includeFinalPackage) {
        List<Canary> canaries = loadCanaries();
        List<Path> channels = new ArrayList<>(evidenceFiles());
        Path console = runtimeRoot.resolve("console.log");
        if (Files.isRegularFile(console)) {
            channels.add(console);
        }
        long hits = 0;
        List<Map<String, Object>> scanned = new ArrayList<>();
        for (Path channel : channels) {
            if (!Files.isRegularFile(channel)
                    || channel.equals(evidenceRoot.resolve("leakage/report.json"))
                    || channel.equals(evidenceRoot.resolve("SHA256SUMS"))) {
                continue;
            }
            try {
                byte[] bytes = Files.readAllBytes(channel);
                String text = new String(bytes, StandardCharsets.UTF_8);
                long channelHits = leakageHits(text,
                        canaries.stream().map(Canary::value).toList(), looksLikeJson(channel));
                hits += channelHits;
                scanned.add(Map.of(
                        "channel", channel.startsWith(evidenceRoot)
                                ? evidenceRoot.relativize(channel).toString().replace('\\', '/')
                                : "captured-console",
                        "sha256", JsonSupport.sha256(bytes),
                        "hitCount", channelHits));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot scan evidence channel", e);
            }
        }
        Set<String> classes = new java.util.TreeSet<>();
        canaries.forEach(canary -> classes.add(canary.type()));
        boolean complete = classes.containsAll(Set.of(
                "access_token", "client_assertion", "dpop_proof", "private_key",
                "sensitive_claim", "api_payload"));
        return new LeakageResult(hits == 0 && complete, hits, classes, scanned);
    }

    static long leakageHits(String text, List<String> canaryValues, boolean jsonChannel) {
        long hits = canaryValues.stream().filter(text::contains).count();
        return jsonChannel ? hits + forbiddenFieldHits(text) : hits;
    }

    private static long forbiddenFieldHits(String text) {
        long hits = 0;
        for (String field : FORBIDDEN_FIELD_NAMES) {
            if (text.matches("(?s).*\\\"" + field + "\\\"\\s*:.*")) {
                hits++;
            }
        }
        return hits;
    }

    private List<Canary> loadCanaries() {
        Path path = runtimeRoot.resolve("private/canaries.jsonl");
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            List<Canary> result = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    JsonNode node = JsonSupport.MAPPER.readTree(line);
                    result.add(new Canary(node.required("type").textValue(),
                            node.required("value").textValue()));
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load private leakage canaries", e);
        }
    }

    private boolean directLedgerPass(int phase) {
        Path ledger = evidenceRoot.resolve("network/payload-call-ledger.jsonl");
        try {
            List<JsonNode> nodes = new ArrayList<>();
            for (String line : Files.readAllLines(ledger, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    nodes.add(JsonSupport.MAPPER.readTree(line));
                }
            }
            for (String scenario : List.of("E001-FLOW-001", "E001-CON-001")) {
                List<JsonNode> selected = nodes.stream()
                        .filter(node -> scenario.equals(node.path("scenarioId").asText()))
                        .toList();
                long apiReceivers = selected.stream()
                        .filter(node -> node.required("apiDataReceived").booleanValue()).count();
                boolean producerOnly = selected.stream()
                        .filter(node -> node.required("apiDataReceived").booleanValue())
                        .allMatch(node -> "producer-b".equals(node.required("receiver").textValue()));
                boolean asNoApi = selected.stream().anyMatch(node ->
                        "authorization-server".equals(node.required("receiver").textValue())
                                && !node.required("apiDataReceived").booleanValue());
                if (apiReceivers != 1 || !producerOnly || !asNoApi) {
                    return false;
                }
            }
            if (phase >= 2) {
                for (String key : ExperimentConfig.PHASE_2_VARIANTS) {
                    String[] parts = key.split("/", 2);
                    List<JsonNode> selected = nodes.stream()
                            .filter(node -> parts[0].equals(node.path("scenarioId").asText())
                                    && parts[1].equals(node.path("variantId").asText())
                                    && "producer-b".equals(node.path("receiver").asText()))
                            .toList();
                    boolean expectedBusiness = Set.of(
                            "E001-FLOW-002/baseline", "E001-SEC-001/baseline").contains(key);
                    if (selected.size() != 1
                            || selected.getFirst().required("apiDataReceived").booleanValue()
                            != expectedBusiness) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, Object> completeness(LeakageResult leakage, int phase,
                                             List<String> expectedResults) {
        List<String> expectedVariants = expectedVariantsForPhase(phase);
        return Map.of(
                "phase", "phase-" + phase,
                "expectedVariants", expectedVariants,
                "observedResultFiles", expectedResults,
                "toolGates", List.of("runtime", "nimbus-dpop", "swagger-parser", "kappa-jackson"),
                "channels", List.of("telemetry", "audit", "contract", "dependency",
                        "network", "external-errors", "harness-errors", "results", "captured-console", "metadata", "discovery", "transitions"),
                "leakageCanaryClasses", leakage.canaryClasses(),
                "complete", leakage.passed() && (phase < 4 || new PhaseFourEvidence(evidenceRoot, runId).evaluate().values().stream().allMatch(Boolean::booleanValue)) && (phase < 3 || new PhaseThreeEvidence(evidenceRoot, runId).evaluate().values().stream().allMatch(Boolean::booleanValue)));
    }

    private int highestPresentPhase() {
        Path results = runtimeRoot.resolve("results");
        if (PHASE_4_RESULTS.stream().anyMatch(name -> Files.isRegularFile(results.resolve(name)))) return 4;
        if (PHASE_3_RESULTS.stream().anyMatch(name -> Files.isRegularFile(results.resolve(name)))) {
            return 3;
        }
        if (PHASE_2_RESULTS.stream().anyMatch(name -> Files.isRegularFile(results.resolve(name)))) {
            return 2;
        }
        return 1;
    }

    private static int phaseFromManifest(JsonNode manifest) {
        String phase = manifest.path("phase").asText();
        return switch (phase) {
            case "phase-1" -> 1;
            case "phase-2" -> 2;
            case "phase-3" -> 3;
            case "phase-4" -> 4;
            default -> throw new IllegalStateException("Unknown evidence phase: " + phase);
        };
    }

    private static String schemaVersionForPhase(int phase) {
        return switch (phase) {
            case 1 -> "1.0.0";
            case 2 -> "2.0.0";
            case 3 -> "3.1.0";
            case 4 -> "4.0.0";
            default -> throw new IllegalArgumentException("Unknown phase " + phase);
        };
    }

    private static String manifestSchemaForPhase(int phase) {
        return switch (phase) {
            case 1 -> "experiment-001/schemas/evidence-manifest.schema.json";
            case 2 -> "experiment-001/schemas/evidence-manifest-phase-2.schema.json";
            case 3 -> "experiment-001/schemas/evidence-manifest-phase-3-1.1.0.schema.json";
            case 4 -> "experiment-001/schemas/evidence-manifest-phase-4.schema.json";
            default -> throw new IllegalArgumentException("Unknown phase " + phase);
        };
    }

    private static String resultSchemaFor(String resultFileName) {
        if (PHASE_4_RESULTS.contains(resultFileName)) return "experiment-001/schemas/scenario-result-phase-4.schema.json";
        if (PHASE_3_RESULTS.contains(resultFileName)) {
            return "experiment-001/schemas/scenario-result-phase-3-1.1.0.schema.json";
        }
        if (PHASE_2_RESULTS.contains(resultFileName)) {
            return "experiment-001/schemas/scenario-result-phase-2.schema.json";
        }
        return "experiment-001/schemas/scenario-result.schema.json";
    }

    private static List<String> expectedVariantsForPhase(int phase) {
        java.util.stream.Stream<String> combined = switch (phase) {
            case 1 -> ExperimentConfig.PHASE_1_VARIANTS.stream();
            case 2 -> java.util.stream.Stream.concat(
                    ExperimentConfig.PHASE_1_VARIANTS.stream(), ExperimentConfig.PHASE_2_VARIANTS.stream());
            case 4 -> ExperimentConfig.IMPLEMENTED_VARIANTS.stream();
            case 3 -> java.util.stream.Stream.of(ExperimentConfig.PHASE_1_VARIANTS,
                            ExperimentConfig.PHASE_2_VARIANTS, ExperimentConfig.PHASE_3_VARIANTS)
                    .flatMap(Set::stream);
            default -> throw new IllegalArgumentException("Unknown phase " + phase);
        };
        return combined.sorted().toList();
    }

    private static List<String> expectedResults(int phase) {
        List<String> combined = new ArrayList<>(PHASE_1_RESULTS);
        if (phase >= 2) {
            combined.addAll(PHASE_2_RESULTS);
        }
        if (phase >= 3) {
            combined.addAll(PHASE_3_RESULTS);
        }
        if (phase >= 4) combined.addAll(PHASE_4_RESULTS);
        return List.copyOf(combined);
    }

    private boolean completenessMatches(int phase) {
        try {
            var report = read(evidenceRoot.resolve("completeness.json"));
            Set<String> actual;
            try (var files = Files.list(evidenceRoot.resolve("results"))) {
                actual = files.map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
            }
            return actual.equals(new HashSet<>(expectedResults(phase))) && report.path("complete").asBoolean()
                    && report.path("expectedVariants").equals(JsonSupport.MAPPER.valueToTree(expectedVariantsForPhase(phase)))
                    && report.path("observedResultFiles").equals(JsonSupport.MAPPER.valueToTree(expectedResults(phase)));
        } catch (Exception e) { return false; }
    }

    private void writeChecksums() throws IOException {
        StringBuilder sums = new StringBuilder();
        for (Path path : evidenceFiles()) {
            if (!path.equals(evidenceRoot.resolve("SHA256SUMS"))) {
                sums.append(JsonSupport.sha256(path)).append("  ")
                        .append(evidenceRoot.relativize(path).toString().replace('\\', '/'))
                        .append('\n');
            }
        }
        Files.writeString(evidenceRoot.resolve("SHA256SUMS"), sums.toString(), StandardCharsets.UTF_8);
    }

    private boolean verifyChecksums() {
        try {
            Set<String> listed = new HashSet<>();
            Set<String> expected = evidenceFiles().stream().filter(p -> !p.getFileName().toString().equals("SHA256SUMS"))
                    .map(p -> evidenceRoot.relativize(p).toString().replace('\\', '/')).collect(java.util.stream.Collectors.toSet());
            for (String line : Files.readAllLines(evidenceRoot.resolve("SHA256SUMS"), StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("  ", 2);
                if (!listed.add(parts[1])) return false;
                Path path = evidenceRoot.resolve(parts[1]).normalize();
                if (!path.startsWith(evidenceRoot) || !parts[0].equals(JsonSupport.sha256(path))) {
                    return false;
                }
            }
            return listed.equals(expected);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyManifestEntries(JsonNode manifest) {
        try {
            Set<String> expected = new HashSet<>();
            for (Path path : evidenceFiles()) {
                if (!path.equals(evidenceRoot.resolve("manifest.json"))
                        && !path.equals(evidenceRoot.resolve("SHA256SUMS"))) {
                    expected.add(evidenceRoot.relativize(path).toString().replace('\\', '/'));
                }
            }
            Set<String> observed = new HashSet<>();
            for (JsonNode entry : manifest.required("files")) {
                String relative = entry.required("path").textValue();
                Path path = evidenceRoot.resolve(relative).normalize();
                if (!path.startsWith(evidenceRoot) || !Files.isRegularFile(path)
                        || !entry.required("sha256").textValue().equals(JsonSupport.sha256(path))) {
                    return false;
                }
                if (!observed.add(relative)) return false;
            }
            return observed.equals(expected);
        } catch (Exception e) {
            return false;
        }
    }

    private List<Path> evidenceFiles() {
        if (!Files.exists(evidenceRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(evidenceRoot)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean looksLikeJson(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") || name.endsWith(".jsonl");
    }

    private static JsonNode read(Path path) {
        try {
            return JsonSupport.MAPPER.readTree(path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read evidence file " + path, e);
        }
    }

    private static String git(String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IllegalStateException("Git evidence query failed");
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot query Git evidence", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Git evidence query", e);
        }
    }

    public static void deleteTree(Path target) throws IOException {
        if (!target.toString().contains("target/experiment-001")) {
            throw new IllegalArgumentException("Refusing to delete outside generated experiment state");
        }
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private record Canary(String type, String value) {
    }

    private record LeakageResult(boolean passed, long hitCount, Set<String> canaryClasses,
                                 List<Map<String, Object>> scannedFiles) {
        private Map<String, Object> toMap() {
            return Map.of(
                    "schemaVersion", "1.0.0",
                    "status", passed ? "pass" : "inconclusive",
                    "hitCount", hitCount,
                    "canaryClasses", canaryClasses,
                    "scannedFiles", scannedFiles);
        }
    }

    public record CollectionResult(String status, boolean leakagePass,
                                   boolean directLedgerPass, Path evidencePath) {
    }

    public record ValidationResult(String status, boolean checksumsPass,
                                   boolean leakagePass, boolean directLedgerPass) {
        public boolean passed() {
            return "pass".equals(status);
        }
    }
}
