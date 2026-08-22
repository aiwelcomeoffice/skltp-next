package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.AccessTokenFixtures;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.consumer.Consumer;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class PhaseTwoScenarioRunner {
    private static final String CONSUMER_ACTOR = "consumer-a";
    private static final String ATTACKER_ACTOR = "logical-attacker";

    private final Path runtimeRoot;
    private final String runId;
    private final RuntimeEnvironment.EnvironmentInfo environment;
    private final PhaseTwoOracles oracles = new PhaseTwoOracles();
    private final MetadataStores metadataStores;
    private final Consumer consumer;
    private final CryptoMaterial material;
    private final HttpClient controlClient;

    PhaseTwoScenarioRunner(Path runtimeRoot, String runId) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        environment = RuntimeEnvironment.read(runtimeRoot);
        metadataStores = new MetadataStores(runtimeRoot);
        consumer = new Consumer(runtimeRoot, runId);
        material = CryptoMaterial.load(runtimeRoot);
        controlClient = HttpClient.newBuilder()
                .sslContext(TlsMaterial.clientContext(runtimeRoot))
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    ScenarioEngine.ScenarioResult run(String scenarioId, String variantId) {
        PhaseTwoOracles.ExpectedOutcome expected = oracles.expected(scenarioId, variantId);
        MetadataStores.DiscoveryResult discovery = metadataStores.discover();
        Consumer.ResourceResponse response;
        Instant tokenObtainedAt = null;
        String actor = CONSUMER_ACTOR;
        boolean tokenPresented = !("E001-TOK-001".equals(scenarioId)
                && "missing".equals(variantId));

        switch (scenarioId) {
            case "E001-FLOW-002" -> {
                Consumer.IssuedToken token = consumer.obtainToken(scenarioId, variantId, discovery,
                        Consumer.TokenKind.BEARER, ExperimentConfig.SCOPE_READ);
                tokenObtainedAt = token.obtainedAt();
                response = consumer.callResource(scenarioId, variantId, discovery,
                        "Bearer", token.value(), null, actor);
            }
            case "E001-SEC-001" -> {
                Consumer.IssuedToken token = consumer.obtainToken(scenarioId, variantId, discovery,
                        Consumer.TokenKind.BEARER, ExperimentConfig.SCOPE_READ);
                tokenObtainedAt = token.obtainedAt();
                actor = ATTACKER_ACTOR;
                response = consumer.callResource(scenarioId, variantId, discovery,
                        "Bearer", token.value(), null, actor);
            }
            case "E001-SEC-002" -> {
                Consumer.IssuedToken token = consumer.obtainToken(scenarioId, variantId, discovery,
                        Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
                tokenObtainedAt = token.obtainedAt();
                actor = ATTACKER_ACTOR;
                response = consumer.callResource(scenarioId, variantId, discovery,
                        "DPoP", token.value(), attackerKey(), actor);
            }
            case "E001-AUTHZ-001" -> {
                if ("insufficient-scope".equals(variantId)) {
                    Consumer.IssuedToken token = consumer.obtainToken(scenarioId, variantId, discovery,
                            Consumer.TokenKind.BEARER, ExperimentConfig.SCOPE_INSUFFICIENT);
                    tokenObtainedAt = token.obtainedAt();
                    response = consumer.callResource(scenarioId, variantId, discovery,
                            "Bearer", token.value(), null, actor);
                } else {
                    configureLocalPolicyDeny();
                    Consumer.IssuedToken token = consumer.obtainToken(scenarioId, variantId, discovery,
                            Consumer.TokenKind.DPOP, ExperimentConfig.SCOPE_READ);
                    tokenObtainedAt = token.obtainedAt();
                    response = consumer.callResource(scenarioId, variantId, discovery,
                            "DPoP", token.value(), material.dpopKey(), actor);
                }
            }
            case "E001-TOK-001" -> {
                String token = AccessTokenFixtures.create(
                        runtimeRoot, discovery.issuer(), variantId);
                response = consumer.callResource(scenarioId, variantId, discovery,
                        "Bearer", token, null, actor);
            }
            default -> throw new IllegalArgumentException("Not a Phase 2 scenario");
        }

        List<JsonNode> decisions = decisionEvents(scenarioId, variantId);
        List<String> checkpoints = decisions.stream()
                .map(node -> node.required("checkpoint").textValue())
                .distinct()
                .toList();
        boolean businessExecuted = checkpoints.contains("producer.business-operation");
        String actualDecision = response.status() == 200 ? "allow" : "deny";
        String actualTerminal = actualTerminal(checkpoints);
        String senderConstraint = senderConstraint(decisions, expected.securityClass());
        String auditRef = latestAuditRef(scenarioId, variantId);
        long tokenAgeSeconds = tokenObtainedAt == null ? -1
                : Math.max(0, Duration.between(tokenObtainedAt, Instant.now()).toSeconds());

        boolean matches = expected.decision().equals(actualDecision)
                && expected.httpStatus() == response.status()
                && equalNullable(expected.wwwAuthenticate(), response.wwwAuthenticate())
                && equalNullable(expected.problemType(), response.problemType())
                && expected.terminalCheckpoint().equals(actualTerminal)
                && expected.businessOperationExecuted() == businessExecuted
                && response.contractValidated()
                && checkpointOrderMatches(decisions, expected)
                && networkEvidenceMatches(scenarioId, variantId, actor, businessExecuted);
        if ("E001-SEC-001".equals(scenarioId)) {
            matches = matches && tokenAgeSeconds < ExperimentConfig.TOKEN_SECONDS;
        }

        ScenarioEngine.PhaseTwoDetails details = new ScenarioEngine.PhaseTwoDetails(
                expected.decision(),
                actualTerminal,
                response.status(),
                response.wwwAuthenticate(),
                response.problemType(),
                expected.securityClass(),
                tokenPresented,
                senderConstraint,
                businessExecuted,
                actor,
                "E001-SEC-001".equals(scenarioId) ? tokenAgeSeconds : null,
                observationsFor(scenarioId, variantId));
        return new ScenarioEngine.ScenarioResult(
                scenarioId, variantId, actualDecision, matches ? "pass" : "fail",
                checkpoints, auditRef, response.contractValidated() ? "pass" : "not-applicable",
                "pending-collection", details);
    }

    static ScenarioEngine.PhaseTwoDetails inconclusiveDetailsFor(
            String scenarioId, String variantId) {
        PhaseTwoOracles.ExpectedOutcome expected = new PhaseTwoOracles()
                .expected(scenarioId, variantId);
        return new ScenarioEngine.PhaseTwoDetails(
                expected.decision(),
                "safe-evidence-finalization",
                null,
                null,
                null,
                expected.securityClass(),
                !("E001-TOK-001".equals(scenarioId) && "missing".equals(variantId)),
                "unknown",
                null,
                "E001-SEC-001".equals(scenarioId) || "E001-SEC-002".equals(scenarioId)
                        ? ATTACKER_ACTOR : CONSUMER_ACTOR,
                null,
                List.of("harness-error.safe-finalization"));
    }

    private void configureLocalPolicyDeny() {
        URI uri = environment.producerEndpoint().resolve("/__policy/deny");
        try {
            HttpResponse<Void> response = controlClient.send(HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(2))
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Producer policy control rejected configuration");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Producer policy control failed", e);
        }
    }

    private static ECKey attackerKey() {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID("E001-logical-attacker-dpop").generate();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create synthetic attacker key", e);
        }
    }

    private boolean checkpointOrderMatches(List<JsonNode> decisions,
                                           PhaseTwoOracles.ExpectedOutcome expected) {
        int token = checkpointIndex(decisions, "producer.token-validation");
        int sender = checkpointIndex(decisions, "producer.sender-constraint");
        int authorization = checkpointIndex(decisions, "producer.authorization");
        int operation = checkpointIndex(decisions, "producer.business-operation");
        if (token < 0) {
            return false;
        }
        if ("bearer".equals(expected.securityClass()) && sender >= 0) {
            return false;
        }
        if ("dpop".equals(expected.securityClass()) && sender <= token) {
            return false;
        }
        return switch (expected.terminalCheckpoint()) {
            case "producer.token-validation" -> sender < 0 && authorization < 0 && operation < 0
                    && decisionIs(decisions.get(token), "deny");
            case "producer.sender-constraint" -> authorization < 0 && operation < 0
                    && decisionIs(decisions.get(token), "allow")
                    && decisionIs(decisions.get(sender), "deny");
            case "producer.authorization" -> authorization > token && operation < 0
                    && (sender < 0 || authorization > sender)
                    && decisionIs(decisions.get(token), "allow")
                    && decisionIs(decisions.get(authorization), "deny");
            case "business-operation" -> authorization > token && operation > authorization
                    && (sender < 0 || authorization > sender)
                    && decisionIs(decisions.get(token), "allow")
                    && decisionIs(decisions.get(authorization), "allow")
                    && decisionIs(decisions.get(operation), "allow");
            default -> false;
        };
    }

    private boolean networkEvidenceMatches(String scenarioId, String variantId,
                                           String actor, boolean businessExecuted) {
        List<JsonNode> producerCalls = jsonLines(
                runtimeRoot.resolve("events/network/payload-call-ledger.jsonl"),
                scenarioId, variantId).stream()
                .filter(node -> "producer-b".equals(node.path("receiver").asText()))
                .toList();
        return producerCalls.size() == 1
                && actor.equals(producerCalls.getFirst().required("sender").textValue())
                && businessExecuted == producerCalls.getFirst()
                .required("apiDataReceived").booleanValue();
    }

    private String senderConstraint(List<JsonNode> decisions, String securityClass) {
        if ("bearer".equals(securityClass)) {
            return "not-applicable";
        }
        int sender = checkpointIndex(decisions, "producer.sender-constraint");
        return sender < 0 ? "unknown" : decisions.get(sender).required("result").textValue();
    }

    private static String actualTerminal(List<String> checkpoints) {
        if (checkpoints.contains("producer.business-operation")) {
            return "business-operation";
        }
        for (String checkpoint : List.of(
                "producer.authorization", "producer.sender-constraint", "producer.token-validation")) {
            if (checkpoints.contains(checkpoint)) {
                return checkpoint;
            }
        }
        return "safe-evidence-finalization";
    }

    private static int checkpointIndex(List<JsonNode> decisions, String checkpoint) {
        for (int i = 0; i < decisions.size(); i++) {
            if (checkpoint.equals(decisions.get(i).path("checkpoint").asText())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean decisionIs(JsonNode node, String expected) {
        return expected.equals(node.required("result").textValue());
    }

    private List<JsonNode> decisionEvents(String scenarioId, String variantId) {
        return jsonLines(runtimeRoot.resolve("events/telemetry/decisions.jsonl"),
                scenarioId, variantId).stream()
                .filter(node -> List.of(
                        "client_authentication", "sender_constraint", "token_issuance",
                        "token_validation", "authorization", "business_operation")
                        .contains(node.path("category").asText()))
                .toList();
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
                if (!line.isBlank()) {
                    JsonNode node = JsonSupport.MAPPER.readTree(line);
                    if (scenarioId.equals(node.path("scenarioId").asText())
                            && variantId.equals(node.path("variantId").asText())) {
                        result.add(node);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read safe Phase 2 evidence", e);
        }
    }

    private static boolean equalNullable(String left, String right) {
        return java.util.Objects.equals(left, right);
    }

    private static List<String> observationsFor(String scenarioId, String variantId) {
        return switch (scenarioId) {
            case "E001-FLOW-002" -> List.of(
                    "dpop-comparison.same-identity-context",
                    "dpop-comparison.same-issuer-profile",
                    "dpop-comparison.same-audience-profile",
                    "dpop-comparison.same-scope-profile",
                    "dpop-comparison.same-token-lifetime",
                    "dpop-comparison.same-producer-policy",
                    "bearer.no-sender-constraint");
            case "E001-SEC-001" -> List.of(
                    "bearer.copied-token-reused",
                    "attacker-context.separate",
                    "bearer.no-sender-constraint",
                    "token-age.observed");
            case "E001-SEC-002" -> List.of(
                    "dpop.copied-token",
                    "dpop.wrong-proof-key",
                    "authorization.not-reached",
                    "business-operation.not-executed");
            case "E001-AUTHZ-001" -> List.of(
                    "authorization." + variantId,
                    "credentials.accepted",
                    "business-operation.not-executed");
            case "E001-TOK-001" -> List.of(
                    "token-validation." + variantId,
                    "later-checkpoints.not-reached",
                    "business-operation.not-executed");
            default -> throw new IllegalArgumentException("Not a Phase 2 scenario");
        };
    }
}
