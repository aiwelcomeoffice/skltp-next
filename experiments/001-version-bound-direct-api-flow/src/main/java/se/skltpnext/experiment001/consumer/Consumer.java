package se.skltpnext.experiment001.consumer;

import se.skltpnext.experiment001.evidence.PhaseFourEvents;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import io.opentelemetry.api.trace.Span;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.contract.ContractValidators;
import se.skltpnext.experiment001.evidence.CanaryRegistry;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Consumer {
    private static final String EXTERNAL_TRACEPARENT =
            "00-11111111111111111111111111111111-2222222222222222-01";
    private final Path runtimeRoot;
    private final String runId;
    private final CryptoMaterial material;
    private final HttpClient httpClient;
    private final boolean observeFaults;
    private final ContractValidators contracts = new ContractValidators();

    public Consumer(Path runtimeRoot, String runId) { this(runtimeRoot, runId, false); }

    public Consumer(Path runtimeRoot, String runId, boolean observeFaults) {
        this.observeFaults = observeFaults;
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        material = CryptoMaterial.load(runtimeRoot);
        httpClient = HttpClient.newBuilder()
                .sslContext(TlsMaterial.clientContext(runtimeRoot))
                .connectTimeout(Duration.ofMillis(300))
                .build();
    }

    public FlowResult execute(String scenarioId, String variantId,
                              MetadataStores.DiscoveryResult discovery) {
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenarioId, variantId, "consumer")) {
            long tokenStart = System.nanoTime();
            String token = requestToken(scenarioId, variantId, discovery);
            telemetry.dependency("authorization-server", "success",
                    Duration.ofNanos(System.nanoTime() - tokenStart).toMillis());

            URI resourceUri = ExperimentConfig.resourceUri(discovery.producerEndpoint());
            var consumerRequest = contracts.validateConsumerRequest(resourceUri);
            telemetry.contract(consumerRequest.role(), consumerRequest.phase(), consumerRequest.result());

            DPoPAccessToken dpopAccessToken = new DPoPAccessToken(token);
            DefaultDPoPProofFactory dpopFactory = new DefaultDPoPProofFactory(
                    material.dpopKey(), JWSAlgorithm.ES256);
            var resourceProof = dpopFactory.createDPoPJWT(
                    new JWTID("E001-RESOURCE-PROOF-" + UUID.randomUUID()),
                    "GET", resourceUri, Date.from(Instant.now()), dpopAccessToken);
            new CanaryRegistry(runtimeRoot.resolve("private"))
                    .register("dpop_proof", resourceProof.serialize());

            Span span = telemetry.startConsumerSpan(EXTERNAL_TRACEPARENT);
            Map<String, String> traceHeaders = new LinkedHashMap<>();
            try (var ignored = span.makeCurrent()) {
                telemetry.inject(io.opentelemetry.context.Context.current(), traceHeaders);
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resourceUri)
                        .timeout(Duration.ofMillis(300))
                        .GET()
                        .header("Accept", "application/json")
                        .header("Authorization", dpopAccessToken.toAuthorizationHeader())
                        .header("DPoP", resourceProof.serialize())
                        .header("X-Experiment-Scenario", scenarioId)
                        .header("X-Experiment-Variant", variantId);
                traceHeaders.forEach(requestBuilder::header);
                long producerStart = System.nanoTime();
                HttpResponse<String> response = httpClient.send(
                        requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                telemetry.dependency("producer", response.statusCode() == 200 ? "success" : "error",
                        Duration.ofNanos(System.nanoTime() - producerStart).toMillis());
                if (response.statusCode() != 200
                        || !response.headers().firstValue("Content-Type").orElse("")
                        .startsWith("application/json")) {
                    telemetry.endAndExport(span, false);
                    throw new IllegalStateException("Producer returned a non-baseline response");
                }
                var consumerResponse = contracts.validateConsumerResponse(response.body());
                telemetry.contract(consumerResponse.role(), consumerResponse.phase(), consumerResponse.result());
                telemetry.endAndExport(span, true);
                return new FlowResult(true, true, true, true, true,
                        span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Direct flow failed at a protected checkpoint", e);
        }
    }

    public IssuedToken obtainToken(String scenarioId, String variantId,
                                   MetadataStores.DiscoveryResult discovery,
                                   TokenKind tokenKind, String scope) {
        long requestStarted = System.nanoTime();
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenarioId, variantId, "consumer")) {
            long tokenStart = System.nanoTime();
            IssuedToken token = requestAccessToken(
                    scenarioId, variantId, discovery, tokenKind, scope);
            telemetry.dependency("authorization-server", "success",
                    Duration.ofNanos(System.nanoTime() - tokenStart).toMillis());
            return token;
        } catch (DependencyFailure e) {
            throw e;
        } catch (TokenRequestDenied e) {
            try (var telemetry = new TelemetryRecorder(runtimeRoot, runId, scenarioId, variantId, "consumer")) {
                telemetry.dependency("authorization-server", "error", Duration.ofNanos(System.nanoTime() - requestStarted).toMillis());
            }
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Token request failed at a protected checkpoint", e);
        }
    }

    public ResourceResponse callResource(String scenarioId, String variantId,
                                         MetadataStores.DiscoveryResult discovery,
                                         String authorizationScheme, String accessToken,
                                         ECKey proofKey, String actorContext) {
        return callResource(scenarioId, variantId, discovery, authorizationScheme, accessToken,
                proofKey, actorContext, false);
    }

    /** The invalid URI is an explicit contract-test stimulus, sent to exercise the provider gate. */
    public ResourceResponse callResource(String scenarioId, String variantId,
                                         MetadataStores.DiscoveryResult discovery,
                                         String authorizationScheme, String accessToken,
                                         ECKey proofKey, String actorContext, boolean dispatchInvalidRequest) {
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenarioId, variantId, "consumer")) {
            URI resourceUri = dispatchInvalidRequest
                    ? discovery.producerEndpoint().resolve(JsonSupport.readResource("experiment-001/scenarios/contract-faults-phase-4-1.0.0.json").required("invalidRecordPath").asText())
                    : ExperimentConfig.resourceUri(discovery.producerEndpoint());
            var consumerRequest = contracts.observeRequest("consumer", resourceUri, "application/json");
            telemetry.contract(consumerRequest.role(), consumerRequest.phase(), consumerRequest.result());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resourceUri)
                    .timeout(Duration.ofMillis(300))
                    .GET()
                    .header("Accept", "application/json")
                    .header("X-Experiment-Scenario", scenarioId)
                    .header("X-Experiment-Variant", variantId)
                    .header("X-Experiment-Actor", actorContext);
            if (accessToken != null) {
                requestBuilder.header("Authorization", authorizationScheme + " " + accessToken);
            }
            if ("DPoP".equals(authorizationScheme) && accessToken != null && proofKey != null) {
                DPoPAccessToken dpopAccessToken = new DPoPAccessToken(accessToken);
                var resourceProof = new DefaultDPoPProofFactory(proofKey, JWSAlgorithm.ES256)
                        .createDPoPJWT(new JWTID("E001-RESOURCE-PROOF-" + UUID.randomUUID()),
                                "GET", resourceUri, Date.from(Instant.now()), dpopAccessToken);
                new CanaryRegistry(runtimeRoot.resolve("private"))
                        .register("dpop_proof", resourceProof.serialize());
                requestBuilder.header("DPoP", resourceProof.serialize());
            }

            Span span = telemetry.startConsumerSpan(EXTERNAL_TRACEPARENT);
            Map<String, String> traceHeaders = new LinkedHashMap<>();
            try (var ignored = span.makeCurrent()) {
                telemetry.inject(io.opentelemetry.context.Context.current(), traceHeaders);
                traceHeaders.forEach(requestBuilder::header);
                long producerStart = System.nanoTime();
                HttpResponse<String> response = sendObserved(requestBuilder.build(), scenarioId, variantId, "producer");
                telemetry.dependency("producer", response.statusCode() < 400 ? "success" : "error",
                        Duration.ofNanos(System.nanoTime() - producerStart).toMillis());

                String contentType = response.headers().firstValue("Content-Type").orElse("");
                var validation = contracts.observeResponse("consumer", response.statusCode(), contentType, response.body());
                telemetry.contract(validation.role(), validation.phase(), validation.result());
                boolean typeDocumented = response.statusCode() < 400 || contracts.documentedProblemType(response.statusCode(), response.body());
                boolean contractPassed = validation.passed() && typeDocumented;
                String problemType = null;
                if (response.statusCode() >= 400 && contentType.startsWith("application/problem+json"))
                    problemType = JsonSupport.MAPPER.readTree(response.body()).path("type").asText();
                if (!contractPassed) telemetry.decision("consumer.contract-response", "contract_validation", "deny",
                        response.statusCode() == 200 ? "invalid-response" : "undocumented-error");
                if (observeFaults) PhaseFourEvents.record(runtimeRoot, runId,
                        scenarioId, variantId, "client-response", Map.of("httpStatus", response.statusCode(),
                                "contractPassed", contractPassed, "internalDetailAbsent", internalDetailAbsent(response.body()),
                                "mediaType", contentType, "problemTypeDocumented", typeDocumented));
                telemetry.endAndExport(span, response.statusCode() < 400 && contractPassed);
                return new ResourceResponse(
                        response.statusCode(),
                        response.headers().firstValue("WWW-Authenticate").orElse(null),
                        problemType,
                        contractPassed,
                        span.getSpanContext().getTraceId(),
                        span.getSpanContext().getSpanId());
            } catch (Exception e) {
                telemetry.endAndExport(span, false);
                throw e;
            }
        } catch (DependencyFailure e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Direct resource call failed at a protected checkpoint", e);
        }
    }

    private String requestToken(String scenarioId, String variantId,
                                MetadataStores.DiscoveryResult discovery) throws Exception {
        return requestAccessToken(scenarioId, variantId, discovery,
                TokenKind.DPOP, ExperimentConfig.SCOPE_READ).value();
    }

    private IssuedToken requestAccessToken(String scenarioId, String variantId,
                                           MetadataStores.DiscoveryResult discovery,
                                           TokenKind tokenKind, String scope) throws Exception {
        Instant now = Instant.now();
        JWTAuthenticationClaimsSet claims = new JWTAuthenticationClaimsSet(
                new ClientID(ExperimentConfig.CLIENT_ID),
                List.of(new Audience(discovery.tokenEndpoint().toString())),
                Date.from(now.plusSeconds(ExperimentConfig.ASSERTION_SECONDS)),
                Date.from(now.minusSeconds(1)),
                Date.from(now),
                new JWTID("E001-ASSERTION-" + UUID.randomUUID()));
        PrivateKeyJWT authentication = new PrivateKeyJWT(
                claims, JWSAlgorithm.ES256,
                material.clientAuthenticationKey().toECPrivateKey(),
                material.clientAuthenticationKey().getKeyID(), null);
        new CanaryRegistry(runtimeRoot.resolve("private"))
                .register("client_assertion", authentication.getClientAssertion().serialize());

        Map<String, List<String>> form = new LinkedHashMap<>(authentication.toParameters());
        form.put("grant_type", List.of("client_credentials"));
        form.put("scope", List.of(scope));
        HttpRequest.Builder request = HttpRequest.newBuilder(discovery.tokenEndpoint())
                .timeout(Duration.ofMillis(300))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Experiment-Scenario", scenarioId)
                .header("X-Experiment-Variant", variantId)
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)));
        if (tokenKind == TokenKind.DPOP) {
            var tokenProof = new DefaultDPoPProofFactory(material.dpopKey(), JWSAlgorithm.ES256)
                    .createDPoPJWT(new JWTID("E001-TOKEN-PROOF-" + UUID.randomUUID()),
                            "POST", discovery.tokenEndpoint(), Date.from(now), null);
            new CanaryRegistry(runtimeRoot.resolve("private"))
                    .register("dpop_proof", tokenProof.serialize());
            request.header("DPoP", tokenProof.serialize());
        }
        HttpResponse<String> response = sendObserved(request.build(), scenarioId, variantId, "authorization-server");
        if (response.statusCode() != 200) {
            throw new TokenRequestDenied(response.statusCode());
        }
        JsonNode json = JsonSupport.MAPPER.readTree(response.body());
        if (!tokenKind.tokenType.equals(json.required("token_type").textValue())) {
            throw new IllegalStateException("Authorization server returned wrong token type");
        }
        return new IssuedToken(json.required("access_token").textValue(), tokenKind,
                Instant.now(), json.required("expires_in").intValue(), scope);
    }

    private HttpResponse<String> sendObserved(HttpRequest request, String scenario, String variant,
                                               String dependency) throws Exception {
        if (!observeFaults) return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        PhaseFourEvents.record(runtimeRoot, runId, scenario, variant,
                "client-attempt", Map.of("dependency", dependency, "attempts", 1));
        long start = System.nanoTime();
        String outcome = "success";
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 503) { outcome = "unavailable"; throw new DependencyFailure(dependency, outcome); }
            return response;
        } catch (java.net.http.HttpTimeoutException e) {
            outcome = "timeout";
            throw new DependencyFailure(dependency, outcome);
        } catch (java.io.IOException e) {
            outcome = "transport-error";
            throw new DependencyFailure(dependency, outcome);
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            PhaseFourEvents.record(runtimeRoot, runId, scenario, variant,
                    "client-completion", Map.of("dependency", dependency, "result", outcome,
                            "durationMillis", elapsed, "timeoutMillis", 300, "retryBudgetMillis", 350, "attempts", 1,
                            "failureLocation", "consumer.http." + (dependency.equals("producer") ? "producer" : "token")));
            if (!outcome.equals("success")) try (var telemetry = new TelemetryRecorder(runtimeRoot, runId, scenario, variant, "consumer")) {
                telemetry.dependency(dependency, outcome, elapsed);
                telemetry.decision("consumer.dependency." + (dependency.equals("producer") ? "producer" : "token"),
                        "dependency_failure", "deny", outcome);
            }
        }
    }

    private boolean internalDetailAbsent(String body) throws Exception {
        var json = JsonSupport.MAPPER.readTree(body);
        if (json.has("detail") || json.has("stackTrace")) return false;
        for (String line : java.nio.file.Files.readAllLines(runtimeRoot.resolve("private/canaries.jsonl"))) {
            var canary = JsonSupport.MAPPER.readTree(line);
            if (canary.path("type").asText().equals("sensitive_claim")
                    && body.contains(canary.path("value").asText())) return false;
        }
        return true;
    }

    public static final class DependencyFailure extends RuntimeException {
        private final String dependency;
        private final String reason;
        DependencyFailure(String dependency, String reason) {
            super("Dependency failed: " + dependency + "/" + reason);
            this.dependency = dependency; this.reason = reason;
        }
        public String dependency() { return dependency; }
        public String reason() { return reason; }
    }

    private static String formEncode(Map<String, List<String>> form) {
        List<String> pairs = new ArrayList<>();
        form.forEach((key, values) -> values.forEach(value -> pairs.add(
                URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(value, StandardCharsets.UTF_8))));
        return String.join("&", pairs);
    }

    public static final class TokenRequestDenied extends RuntimeException {
        private final int status;
        TokenRequestDenied(int status) { super("Token request denied"); this.status = status; }
        public int status() { return status; }
    }

    public record FlowResult(
            boolean privateKeyJwtValidated,
            boolean dpopTokenBound,
            boolean producerTokenValidated,
            boolean producerSenderConstraintValidated,
            boolean producerAuthorized,
            String consumerTraceId,
            String consumerSpanId) {
    }

    public enum TokenKind {
        BEARER("Bearer"),
        DPOP("DPoP");

        private final String tokenType;

        TokenKind(String tokenType) {
            this.tokenType = tokenType;
        }

        public String tokenType() {
            return tokenType;
        }
    }

    public record IssuedToken(
            String value,
            TokenKind kind,
            Instant obtainedAt,
            int expiresInSeconds,
            String requestedScope) {
    }

    public record ResourceResponse(
            int status,
            String wwwAuthenticate,
            String problemType,
            boolean contractValidated,
            String consumerTraceId,
            String consumerSpanId) {
    }
}
