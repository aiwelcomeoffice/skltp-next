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
    private static final List<String> EXPECTED_RESULTS = List.of(
            "E001-REL-001--valid.json",
            "E001-DIS-001--baseline.json",
            "E001-FLOW-001--baseline.json",
            "E001-CON-001--baseline.json");
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
            if (Files.exists(evidenceRoot)) {
                deleteTree(evidenceRoot);
            }
            Files.createDirectories(evidenceRoot);
            copyResults();
            copyOrCreate("events/telemetry/spans.jsonl", "telemetry/spans.jsonl");
            copyOrCreate("events/telemetry/decisions.jsonl", "telemetry/decisions.jsonl");
            copyOrCreate("events/telemetry/dependencies.jsonl", "telemetry/dependencies.jsonl");
            copyOrCreate("events/audit/records.jsonl", "audit/records.jsonl");
            copyOrCreate("events/contract/validations.jsonl", "contract/validations.jsonl");
            copyOrCreate("events/network/payload-call-ledger.jsonl", "network/payload-call-ledger.jsonl");
            copyOrCreate("events/errors/external.jsonl", "errors/external.jsonl");
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

            LeakageResult leakage = scan(false);
            JsonSupport.writeJson(evidenceRoot.resolve("leakage/report.json"), leakage.toMap());
            JsonSupport.writeJson(evidenceRoot.resolve("completeness.json"), completeness(leakage));

            boolean resultsPass = EXPECTED_RESULTS.stream().allMatch(name ->
                    "pass".equals(read(evidenceRoot.resolve("results").resolve(name))
                            .required("status").textValue()));
            boolean gatesPass = "pass".equals(read(evidenceRoot.resolve("validation/tool-gates.json"))
                    .required("status").textValue());
            boolean directLedgerPass = directLedgerPass();
            String status = resultsPass && gatesPass && leakage.passed() && directLedgerPass
                    ? "pass" : "inconclusive";

            List<Map<String, String>> fileEntries = evidenceFiles().stream()
                    .filter(path -> !path.equals(evidenceRoot.resolve("manifest.json")))
                    .filter(path -> !path.equals(evidenceRoot.resolve("SHA256SUMS")))
                    .map(path -> Map.of(
                            "path", evidenceRoot.relativize(path).toString().replace('\\', '/'),
                            "sha256", JsonSupport.sha256(path)))
                    .toList();
            ObjectNode manifest = JsonSupport.MAPPER.createObjectNode();
            manifest.put("schemaVersion", "1.0.0");
            manifest.put("runId", runId);
            manifest.put("phase", "phase-1");
            manifest.put("status", status);
            manifest.put("sourceCommit", git("rev-parse", "HEAD").trim());
            manifest.put("gitStatusClass", git("status", "--porcelain").isBlank()
                    ? "clean" : "phase-1-working-tree");
            ArrayNode files = manifest.putArray("files");
            fileEntries.forEach(entry -> {
                ObjectNode file = files.addObject();
                file.put("path", entry.get("path"));
                file.put("sha256", entry.get("sha256"));
            });
            JsonSupport.validate(JsonSupport.readResource(
                    "experiment-001/schemas/evidence-manifest.schema.json"), manifest, "evidence manifest");
            JsonSupport.writeJson(evidenceRoot.resolve("manifest.json"), manifest);
            writeChecksums();
            return new CollectionResult(status, leakage.passed(), directLedgerPass, evidenceRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot collect evidence package", e);
        }
    }

    public ValidationResult validate() {
        JsonNode manifest = read(evidenceRoot.resolve("manifest.json"));
        JsonSupport.validate(JsonSupport.readResource(
                "experiment-001/schemas/evidence-manifest.schema.json"), manifest, "evidence manifest");
        for (String name : EXPECTED_RESULTS) {
            JsonNode result = read(evidenceRoot.resolve("results").resolve(name));
            JsonSupport.validate(JsonSupport.readResource(
                    "experiment-001/schemas/scenario-result.schema.json"), result, "scenario result " + name);
            if (!"pass".equals(result.required("status").textValue())) {
                return new ValidationResult("inconclusive", false, false, false);
            }
        }
        boolean checksums = verifyChecksums();
        boolean manifestEntries = verifyManifestEntries(manifest);
        LeakageResult leakage = scan(true);
        boolean ledger = directLedgerPass();
        boolean noPrivate = evidenceFiles().stream().noneMatch(path ->
                path.getFileName().toString().matches("(?i).*(key|keystore|password|token|assertion|proof).*"));
        boolean passed = checksums && manifestEntries && leakage.passed() && ledger && noPrivate
                && "pass".equals(manifest.required("status").textValue());
        return new ValidationResult(passed ? "pass" : "inconclusive",
                checksums, leakage.passed(), ledger);
    }

    private void copyResults() throws IOException {
        Path source = runtimeRoot.resolve("results");
        for (String name : EXPECTED_RESULTS) {
            if (!Files.isRegularFile(source.resolve(name))) {
                throw new IllegalStateException("Missing Phase 1 result " + name);
            }
            Path target = evidenceRoot.resolve("results").resolve(name);
            Files.createDirectories(target.getParent());
            Files.copy(source.resolve(name), target, StandardCopyOption.REPLACE_EXISTING);
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
                "release-index.schema.json",
                "scenario-result.schema.json",
                "evidence-manifest.schema.json",
                "service-metadata.schema.json",
                "membership-metadata.schema.json",
                "iam-metadata.schema.json")) {
            schemas.put(schema, JsonSupport.sha256(JsonSupport.readResourceBytes(
                    "experiment-001/schemas/" + schema)));
        }
        return Map.of(
                "releaseIndexSha256", JsonSupport.sha256(JsonSupport.readResourceBytes(
                        "experiment-001/release/index-1.0.0.json")),
                "releaseReferences", releaseIndex.required("references"),
                "schemas", schemas);
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

    private boolean directLedgerPass() {
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
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, Object> completeness(LeakageResult leakage) {
        return Map.of(
                "phase", "phase-1",
                "expectedVariants", List.of(
                        "E001-REL-001/valid", "E001-DIS-001/baseline",
                        "E001-FLOW-001/baseline", "E001-CON-001/baseline"),
                "observedResultFiles", EXPECTED_RESULTS,
                "toolGates", List.of("runtime", "nimbus-dpop", "swagger-parser", "kappa-jackson"),
                "channels", List.of("telemetry", "audit", "contract", "dependency",
                        "network", "external-errors", "results", "captured-console"),
                "leakageCanaryClasses", leakage.canaryClasses(),
                "complete", leakage.passed());
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
            for (String line : Files.readAllLines(evidenceRoot.resolve("SHA256SUMS"), StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("  ", 2);
                Path path = evidenceRoot.resolve(parts[1]).normalize();
                if (!path.startsWith(evidenceRoot) || !parts[0].equals(JsonSupport.sha256(path))) {
                    return false;
                }
            }
            return true;
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
                observed.add(relative);
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
