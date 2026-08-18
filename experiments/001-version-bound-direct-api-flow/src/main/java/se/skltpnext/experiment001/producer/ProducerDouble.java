package se.skltpnext.experiment001.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.dpop.JWKThumbprintConfirmation;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPIssuer;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPProtectedResourceRequestVerifier;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DefaultDPoPSingleUseChecker;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.NimbusDpopGate;
import se.skltpnext.experiment001.contract.ContractValidators;
import se.skltpnext.experiment001.evidence.CanaryRegistry;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProducerDouble implements HttpHandler {
    private final Path runtimeRoot;
    private final String runId;
    private final URI producerEndpoint;
    private final MetadataStores metadataStores;
    private final ContractValidators contracts = new ContractValidators();
    private volatile DefaultDPoPSingleUseChecker dpopChecker;

    public ProducerDouble(Path runtimeRoot, String runId, URI producerEndpoint) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        this.producerEndpoint = producerEndpoint;
        metadataStores = new MetadataStores(runtimeRoot);
        reset();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String scenario = safeHeader(exchange, "X-Experiment-Scenario", "runtime");
        String variant = safeHeader(exchange, "X-Experiment-Variant", "baseline");
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenario, variant, "producer")) {
            Span span = telemetry.startServerSpan(exchange.getRequestHeaders());
            boolean success = false;
            try (var ignored = span.makeCurrent()) {
                telemetry.network("consumer-a", "producer-b", "PRODUCER-ENDPOINT-REV-1",
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", true);
                URI requestUri = producerEndpoint.resolve(exchange.getRequestURI().toString());
                var providerRequest = contracts.validateProviderRequest(requestUri);
                telemetry.contract(providerRequest.role(), providerRequest.phase(), providerRequest.result());

                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                if (authorization == null || !authorization.startsWith("DPoP ")) {
                    throw new CredentialException("dpop-auth-scheme-required");
                }
                String rawToken = authorization.substring("DPoP ".length());
                JWTClaimsSet claims = validateToken(rawToken);
                telemetry.decision("producer.token-validation", "token_validation",
                        "allow", "rfc9068-token-valid");

                String proofHeader = exchange.getRequestHeaders().getFirst("DPoP");
                if (proofHeader == null) {
                    throw new CredentialException("dpop-proof-required");
                }
                SignedJWT proof = SignedJWT.parse(proofHeader);
                JWKThumbprintConfirmation confirmation = JWKThumbprintConfirmation.parse(claims);
                if (confirmation == null) {
                    throw new CredentialException("cnf-jkt-required");
                }
                DPoPProtectedResourceRequestVerifier verifier = NimbusDpopGate.resourceVerifier(dpopChecker);
                verifier.verify("GET", requestUri,
                        new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID)), proof,
                        new DPoPAccessToken(rawToken), confirmation);
                telemetry.decision("producer.sender-constraint", "sender_constraint",
                        "allow", "dpop-resource-proof-valid");

                if (!metadataStores.membershipActive(ExperimentConfig.ORGANIZATION_A, "consumer")
                        || !metadataStores.membershipActive(ExperimentConfig.ORGANIZATION_B, "producer")) {
                    throw new AuthorizationException("membership-inactive");
                }
                if (!Set.of(claims.getStringClaim("scope").split(" "))
                        .contains(ExperimentConfig.SCOPE_READ)) {
                    throw new AuthorizationException("scope-insufficient");
                }
                JsonNode policy = JsonSupport.readResource(
                        "experiment-001/profiles/producer-policy-1.0.0.json");
                if (!"allow".equals(policy.required("decision").textValue())) {
                    throw new AuthorizationException("local-policy-deny");
                }
                telemetry.decision("producer.authorization", "authorization",
                        "allow", "local-policy-allow");
                telemetry.audit("producer.authorization", "allow", "local-policy-allow");

                String body = "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}";
                new CanaryRegistry(runtimeRoot.resolve("private")).register("api_payload", body);
                var providerResponse = contracts.validateProviderResponse(body);
                telemetry.contract(providerResponse.role(), providerResponse.phase(), providerResponse.result());
                success = true;
                send(exchange, 200, "application/json", body);
            } catch (CredentialException e) {
                telemetry.decision("producer.token-validation", "token_validation",
                        "deny", e.safeReason);
                send(exchange, 401, "application/problem+json",
                        "{\"type\":\"urn:skltp-next:experiment-001:error:invalid-token\",\"title\":\"Invalid token\",\"status\":401}");
            } catch (AuthorizationException e) {
                telemetry.decision("producer.authorization", "authorization",
                        "deny", e.safeReason);
                telemetry.audit("producer.authorization", "deny", e.safeReason);
                send(exchange, 403, "application/problem+json",
                        "{\"type\":\"urn:skltp-next:experiment-001:error:forbidden\",\"title\":\"Forbidden\",\"status\":403}");
            } catch (Exception e) {
                telemetry.decision("producer.token-validation", "token_validation",
                        "deny", "invalid-credential-or-contract");
                send(exchange, 401, "application/problem+json",
                        "{\"type\":\"urn:skltp-next:experiment-001:error:invalid-token\",\"title\":\"Invalid token\",\"status\":401}");
            } finally {
                telemetry.endAndExport(span, success);
            }
        }
    }

    public synchronized void reset() {
        if (dpopChecker != null) {
            dpopChecker.shutdown();
        }
        dpopChecker = NimbusDpopGate.newChecker();
    }

    public void warmUpContractValidation() {
        URI resourceUri = ExperimentConfig.resourceUri(producerEndpoint);
        if (!contracts.validateProviderRequest(resourceUri).passed()
                || !contracts.validateProviderResponse(
                "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}").passed()) {
            throw new IllegalStateException("Provider contract warm-up failed");
        }
    }

    public void warmUpSecurityLibraries(String accessToken, ECKey dpopKey) {
        try {
            JWTClaimsSet claims = validateToken(accessToken);
            URI resourceUri = ExperimentConfig.resourceUri(producerEndpoint);
            DPoPAccessToken token = new DPoPAccessToken(accessToken);
            SignedJWT proof = new DefaultDPoPProofFactory(dpopKey, JWSAlgorithm.ES256)
                    .createDPoPJWT(new JWTID("E001-WARMUP-RESOURCE-" + java.util.UUID.randomUUID()),
                            "GET", resourceUri, Date.from(Instant.now()), token);
            JWKThumbprintConfirmation confirmation = JWKThumbprintConfirmation.parse(claims);
            NimbusDpopGate.resourceVerifier(dpopChecker).verify(
                    "GET", resourceUri,
                    new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID)),
                    proof, token, confirmation);
        } catch (Exception e) {
            throw new IllegalStateException("Producer security-library warm-up failed", e);
        } finally {
            reset();
        }
    }

    private JWTClaimsSet validateToken(String rawToken) throws Exception {
        JsonNode iam = metadataStores.readAndValidate("iam");
        ECKey asKey = ECKey.parse(iam.required("authorizationServerSigningJwk").toString());
        String issuer = iam.required("oauthIssuer").textValue();

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt")));
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256,
                new ImmutableJWKSet<>(new JWKSet(asKey))));
        JWTClaimsSet exact = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(ExperimentConfig.CLIENT_ID)
                .claim("client_id", ExperimentConfig.CLIENT_ID)
                .build();
        DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
                Set.of(ExperimentConfig.AUDIENCE), exact,
                Set.of("iss", "sub", "aud", "exp", "iat", "jti", "client_id", "scope", "cnf"),
                Set.of());
        claimsVerifier.setMaxClockSkew(2);
        processor.setJWTClaimsSetVerifier(claimsVerifier);
        return processor.process(rawToken, null);
    }

    private static String safeHeader(HttpExchange exchange, String name, String fallback) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value != null && value.matches("[a-zA-Z0-9._-]{1,64}") ? value : fallback;
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class CredentialException extends Exception {
        private final String safeReason;

        private CredentialException(String safeReason) {
            this.safeReason = safeReason;
        }
    }

    private static final class AuthorizationException extends Exception {
        private final String safeReason;

        private AuthorizationException(String safeReason) {
            this.safeReason = safeReason;
        }
    }
}
