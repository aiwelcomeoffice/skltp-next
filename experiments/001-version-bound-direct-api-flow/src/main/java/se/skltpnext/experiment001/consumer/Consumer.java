package se.skltpnext.experiment001.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
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
    private final ContractValidators contracts = new ContractValidators();

    public Consumer(Path runtimeRoot, String runId) {
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

    private String requestToken(String scenarioId, String variantId,
                                MetadataStores.DiscoveryResult discovery) throws Exception {
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

        DefaultDPoPProofFactory dpopFactory = new DefaultDPoPProofFactory(
                material.dpopKey(), JWSAlgorithm.ES256);
        var tokenProof = dpopFactory.createDPoPJWT(
                new JWTID("E001-TOKEN-PROOF-" + UUID.randomUUID()),
                "POST", discovery.tokenEndpoint(), Date.from(now), null);
        new CanaryRegistry(runtimeRoot.resolve("private"))
                .register("dpop_proof", tokenProof.serialize());

        Map<String, List<String>> form = new LinkedHashMap<>(authentication.toParameters());
        form.put("grant_type", List.of("client_credentials"));
        form.put("scope", List.of(ExperimentConfig.SCOPE_READ));
        HttpRequest request = HttpRequest.newBuilder(discovery.tokenEndpoint())
                .timeout(Duration.ofMillis(300))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("DPoP", tokenProof.serialize())
                .header("X-Experiment-Scenario", scenarioId)
                .header("X-Experiment-Variant", variantId)
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Authorization server denied baseline token request");
        }
        JsonNode json = JsonSupport.MAPPER.readTree(response.body());
        if (!"DPoP".equals(json.required("token_type").textValue())) {
            throw new IllegalStateException("Authorization server returned wrong token type");
        }
        return json.required("access_token").textValue();
    }

    private static String formEncode(Map<String, List<String>> form) {
        List<String> pairs = new ArrayList<>();
        form.forEach((key, values) -> values.forEach(value -> pairs.add(
                URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(value, StandardCharsets.UTF_8))));
        return String.join("&", pairs);
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
}

