package se.skltpnext.experiment001.producer;

import se.skltpnext.experiment001.dependency.DoubleFault;
import se.skltpnext.experiment001.evidence.PhaseFourEvents;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import se.skltpnext.experiment001.authorization.AccessTokenValidator;
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
import java.util.Objects;
import java.util.Set;

public final class ProducerDouble implements HttpHandler {
    static final String MISSING_BEARER_CHALLENGE = "Bearer realm=\"experiment-001\"";
    static final String INVALID_BEARER_CHALLENGE =
            "Bearer realm=\"experiment-001\", error=\"invalid_token\"";
    static final String INSUFFICIENT_SCOPE_CHALLENGE =
            "Bearer realm=\"experiment-001\", error=\"insufficient_scope\", scope=\"synthetic.read\"";
    static final String INVALID_DPOP_CHALLENGE = "DPoP error=\"invalid_token\", algs=\"ES256\"";

    private static final JsonNode CONTRACT_FAULTS = JsonSupport.readResource(
            "experiment-001/scenarios/contract-faults-phase-4-1.0.0.json");
    private final Path runtimeRoot;
    private final String runId;
    private final URI producerEndpoint;
    private final String endpointId;
    private final MetadataStores metadataStores;
    private final ContractValidators contracts = new ContractValidators();
    private volatile DefaultDPoPSingleUseChecker dpopChecker;
    private volatile LocalPolicyDecision localPolicyDecision;
    private DoubleFault fault = DoubleFault.NONE;
    public synchronized void setFault(DoubleFault value) { fault = value; }
    public synchronized void drain() { /* Acquiring the handler lock proves completion. */ }


    public ProducerDouble(Path runtimeRoot, String runId, URI producerEndpoint) {
        this(runtimeRoot, runId, producerEndpoint, "PRODUCER-ENDPOINT-REV-1");
    }

    public ProducerDouble(Path runtimeRoot, String runId, URI producerEndpoint, String endpointId) {
        this.endpointId = endpointId;
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        this.producerEndpoint = producerEndpoint;
        metadataStores = new MetadataStores(runtimeRoot, endpointId, null);
        reset();
    }

    @Override
    public synchronized void handle(HttpExchange exchange) throws IOException {
        String scenario = safeHeader(exchange, "X-Experiment-Scenario", "runtime");
        String variant = safeHeader(exchange, "X-Experiment-Variant", "baseline");
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenario, variant, "producer")) {
            Span span = telemetry.startServerSpan(exchange.getRequestHeaders());
            String callerContext = safeCallerContext(exchange);
            boolean success = false;
            try (var ignored = span.makeCurrent()) {
                URI requestUri = producerEndpoint.resolve(exchange.getRequestURI().toString());
                long arrival = System.nanoTime();
                if (fault != DoubleFault.NONE) {
                    faultEvent(scenario, variant, "server-arrival", 0, "received");
                    if (fault == DoubleFault.UNAVAILABLE) {
                        telemetry.network(callerContext, "producer-b", endpointId, "GET", "/synthetic-records/{recordId}", false);
                        send(exchange, 503, "application/json", "{}");
                        faultEvent(scenario, variant, "server-completion", System.nanoTime() - arrival, "unavailable");
                        return;
                    }
                }
                var providerRequest = contracts.observeRequest("provider", requestUri,
                        exchange.getRequestHeaders().getFirst("Accept"));
                telemetry.contract(providerRequest.role(), providerRequest.phase(), providerRequest.result());
                if (!providerRequest.passed()) {
                    telemetry.decision("producer.contract-request", "contract_validation", "deny", "invalid-request");
                    telemetry.network(callerContext, "producer-b", endpointId, "GET", "/synthetic-records/{recordId}", false);
                    sendProblem(exchange, telemetry, 404, "urn:skltp-next:experiment-001:error:not-found", "Not found", null);
                    return;
                }

                PresentedToken presentedToken = presentedToken(
                        exchange.getRequestHeaders().getFirst("Authorization"));
                new CanaryRegistry(runtimeRoot.resolve("private"))
                        .register("access_token", presentedToken.serialized());
                var currentMetadata = new MetadataStores(runtimeRoot, endpointId, telemetry);
                JWTClaimsSet claims;
                try {
                    claims = tokenValidator(currentMetadata).validate(presentedToken.serialized()).claims();
                } catch (AccessTokenValidator.TokenValidationException e) {
                    throw new TokenFailure(e.safeReason(), true, presentedToken.scheme(), e);
                }
                telemetry.decision("producer.token-validation", "token_validation",
                        "allow", "rfc9068-token-valid");

                if (presentedToken.scheme() == TokenScheme.DPOP) {
                    validateSenderConstraint(exchange, requestUri, presentedToken, claims);
                    telemetry.decision("producer.sender-constraint", "sender_constraint",
                            "allow", "dpop-resource-proof-valid");
                }

                if (!currentMetadata.bothMembershipsActive()) {
                    throw new AuthorizationFailure("membership-inactive", false,
                            ExperimentConfig.POLICY_VERSION);
                }
                if (!Set.of(claims.getStringClaim("scope").split(" "))
                        .contains(ExperimentConfig.SCOPE_READ)) {
                    throw new AuthorizationFailure("scope-insufficient", true,
                            ExperimentConfig.POLICY_VERSION);
                }
                JsonNode policy = JsonSupport.readResource(
                        "experiment-001/profiles/producer-policy-1.0.0.json");
                if (localPolicyDecision == LocalPolicyDecision.DENY
                        || !"allow".equals(policy.required("decision").textValue())) {
                    throw new AuthorizationFailure("local-policy-deny", false,
                            localPolicyDecision.policyVersion());
                }
                telemetry.decision("producer.authorization", "authorization",
                        "allow", "local-policy-allow", localPolicyDecision.policyVersion());
                telemetry.audit("producer.authorization", "allow", "local-policy-allow",
                        localPolicyDecision.policyVersion());

                if (fault == DoubleFault.INTERNAL_DETAIL) {
                    var problem = JsonSupport.MAPPER.createObjectNode();
                    problem.put("type", "urn:skltp-next:experiment-001:error:local-policy-deny");
                    problem.put("title", "Forbidden by local policy"); problem.put("status", 403);
                    String internal = new CanaryRegistry(runtimeRoot.resolve("private")).newValue("sensitive_claim");
                    problem.put("detail", internal);
                    var candidate = contracts.observeResponse("provider", 403, "application/problem+json", JsonSupport.compact(problem));
                    telemetry.contract("provider", "problem-candidate", candidate.result());
                    if (candidate.passed()) throw new IllegalStateException("Problem guard conformance failure");
                    telemetry.decision("producer.problem-details", "contract_validation", "deny", "internal-detail-blocked");
                    // Block the entire candidate. Construct a separate safe error, never forward exception detail.
                    telemetry.network(callerContext, "producer-b", endpointId, "GET", "/synthetic-records/{recordId}", false);
                    sendProblem(exchange, telemetry, 403, "urn:skltp-next:experiment-001:error:local-policy-deny", "Forbidden by local policy", null);
                    faultEvent(scenario, variant, "server-completion", System.nanoTime() - arrival, "candidate-blocked");
                    return;
                }
                if (fault == DoubleFault.UNDOCUMENTED_ERROR) {
                    int status = CONTRACT_FAULTS.required("undocumentedStatus").asInt();
                    String body = JsonSupport.compact(JsonSupport.MAPPER.createObjectNode()
                            .put("type", "urn:skltp-next:experiment-001:error:undocumented")
                            .put("title", "Synthetic error").put("status", status));
                    var validation = contracts.observeResponse("provider", status, "application/problem+json", body);
                    telemetry.contract(validation.role(), validation.phase(), validation.result());
                    telemetry.decision("producer.contract-response", "contract_validation", "deny", "undocumented-error");
                    telemetry.network(callerContext, "producer-b", endpointId, "GET", "/synthetic-records/{recordId}", false);
                    // Explicit faulty-provider stimulus: retain the exact response for independent consumer validation.
                    send(exchange, status, "application/problem+json", body);
                    faultEvent(scenario, variant, "server-completion", System.nanoTime() - arrival, "fault-response-sent");
                    return;
                }
                telemetry.decision("producer.business-operation", "business_operation",
                        "allow", "synthetic-read-executed");
                if (fault != DoubleFault.NONE)
                    faultEvent(scenario, variant, "business-operation", System.nanoTime() - arrival, "executed");
                String body = fault == DoubleFault.INVALID_RESPONSE
                        ? JsonSupport.compact(CONTRACT_FAULTS.required("invalidResponse"))
                        : "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}";
                new CanaryRegistry(runtimeRoot.resolve("private")).register("api_payload", body);
                var providerResponse = contracts.observeResponse("provider", 200, "application/json", body);
                telemetry.contract(providerResponse.role(), providerResponse.phase(), providerResponse.result());
                if (!providerResponse.passed())
                    telemetry.decision("producer.contract-response", "contract_validation", "deny", "invalid-response");
                success = providerResponse.passed();
                telemetry.network(callerContext, "producer-b", endpointId,
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", true);
                fault.delayResponse();
                try {
                    send(exchange, 200, "application/json", body);
                    if (fault != DoubleFault.NONE)
                        faultEvent(scenario, variant, "server-completion", System.nanoTime() - arrival,
                                fault == DoubleFault.SLOW ? "late-response-sent" : "fault-response-sent");
                } catch (IOException e) {
                    if (fault != DoubleFault.SLOW) throw e;
                    exchange.close();
                    faultEvent(scenario, variant, "server-completion", System.nanoTime() - arrival, "late-response-disconnected");
                }
            } catch (TokenFailure e) {
                if (e.safeReason().equals("signing-key-revoked")) telemetry.audit("producer.token-validation", "deny", e.safeReason());
                telemetry.decision("producer.token-validation", "token_validation",
                        "deny", e.safeReason());
                String problemType = e.presented()
                        ? "urn:skltp-next:experiment-001:error:invalid-token"
                        : "urn:skltp-next:experiment-001:error:missing-token";
                String challenge = e.presented()
                        ? challengeFor(e.scheme()) : MISSING_BEARER_CHALLENGE;
                telemetry.network(callerContext, "producer-b", endpointId,
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", false);
                sendProblem(exchange, telemetry, 401, problemType,
                        e.presented() ? "Invalid token" : "Missing token", challenge);
            } catch (SenderConstraintFailure e) {
                telemetry.decision("producer.sender-constraint", "sender_constraint",
                        "deny", e.safeReason());
                telemetry.network(callerContext, "producer-b", endpointId,
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", false);
                sendProblem(exchange, telemetry, 401,
                        "urn:skltp-next:experiment-001:error:sender-constraint",
                        "Sender constraint failed", INVALID_DPOP_CHALLENGE);
            } catch (AuthorizationFailure e) {
                telemetry.decision("producer.authorization", "authorization",
                        "deny", e.safeReason(), e.policyVersion());
                telemetry.audit("producer.authorization", "deny", e.safeReason(), e.policyVersion());
                telemetry.network(callerContext, "producer-b", endpointId,
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", false);
                if (e.insufficientScope()) {
                    sendProblem(exchange, telemetry, 403,
                            "urn:skltp-next:experiment-001:error:insufficient-scope",
                            "Insufficient scope", INSUFFICIENT_SCOPE_CHALLENGE);
                } else {
                    sendProblem(exchange, telemetry, 403,
                            "urn:skltp-next:experiment-001:error:local-policy-deny",
                            "Forbidden by local policy", null);
                }
            } catch (Exception e) {
                telemetry.decision("producer.token-validation", "token_validation",
                        "deny", "invalid-credential-or-contract");
                telemetry.network(callerContext, "producer-b", endpointId,
                        exchange.getRequestMethod(), "/synthetic-records/{recordId}", false);
                sendProblem(exchange, telemetry, 401,
                        "urn:skltp-next:experiment-001:error:invalid-token",
                        "Invalid token", INVALID_BEARER_CHALLENGE);
            } finally {
                telemetry.endAndExport(span, success);
            }
        } catch (Exception e) {
            recordSafeHandlerFailure(scenario, variant, e);
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException("Producer handler failed", e);
        }
    }

    private void faultEvent(String scenario, String variant, String kind, long nanos, String result) {
        PhaseFourEvents.record(runtimeRoot, runId, scenario, variant,
                kind, java.util.Map.of("dependency", "producer", "durationMillis", nanos / 1_000_000,
                        "result", result, "fault", fault.name()));
    }

    public synchronized void reset() {
        if (dpopChecker != null) {
            dpopChecker.shutdown();
        }
        dpopChecker = NimbusDpopGate.newChecker();
        localPolicyDecision = LocalPolicyDecision.ALLOW;
        fault = DoubleFault.NONE;
    }

    /** Called only by the harness control plane; request scenario headers never select policy. */
    public void setLocalPolicyDecision(LocalPolicyDecision decision) {
        localPolicyDecision = Objects.requireNonNull(decision, "decision");
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
            JWTClaimsSet claims = tokenValidator().validate(accessToken).claims();
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

    private AccessTokenValidator tokenValidator() throws AccessTokenValidator.TokenValidationException {
        return tokenValidator(metadataStores);
    }

    private AccessTokenValidator tokenValidator(MetadataStores stores) throws AccessTokenValidator.TokenValidationException {
        JsonNode iam = stores.readAndValidate("iam");
        if ("revoked".equals(iam.required("authorizationServerSigningKeyStatus").asText()))
            throw new AccessTokenValidator.TokenValidationException("signing-key-revoked");
        try {
            ECKey asKey = ECKey.parse(iam.required("authorizationServerSigningJwk").toString());
            String issuer = iam.required("oauthIssuer").textValue();
            return new AccessTokenValidator(issuer, asKey);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Invalid authorization-server public key metadata", e);
        }
    }

    private void validateSenderConstraint(HttpExchange exchange, URI requestUri,
                                          PresentedToken token, JWTClaimsSet claims)
            throws SenderConstraintFailure {
        try {
            String proofHeader = exchange.getRequestHeaders().getFirst("DPoP");
            if (proofHeader == null) {
                throw new SenderConstraintFailure("dpop-proof-required");
            }
            SignedJWT proof = SignedJWT.parse(proofHeader);
            new CanaryRegistry(runtimeRoot.resolve("private"))
                    .register("dpop_proof", proof.serialize());
            JWKThumbprintConfirmation confirmation = JWKThumbprintConfirmation.parse(claims);
            if (confirmation == null) {
                throw new SenderConstraintFailure("cnf-jkt-required");
            }
            DPoPProtectedResourceRequestVerifier verifier = NimbusDpopGate.resourceVerifier(dpopChecker);
            verifier.verify("GET", requestUri,
                    new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID)), proof,
                    new DPoPAccessToken(token.serialized()), confirmation);
        } catch (SenderConstraintFailure e) {
            throw e;
        } catch (Exception e) {
            throw new SenderConstraintFailure("dpop-key-or-proof-mismatch", e);
        }
    }

    private static PresentedToken presentedToken(String authorization) throws TokenFailure {
        if (authorization == null || authorization.isBlank()) {
            throw new TokenFailure("missing-token", false, TokenScheme.BEARER);
        }
        if (authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            if (token.isBlank()) {
                throw new TokenFailure("missing-token", false, TokenScheme.BEARER);
            }
            return new PresentedToken(TokenScheme.BEARER, token);
        }
        if (authorization.startsWith("DPoP ")) {
            String token = authorization.substring("DPoP ".length());
            if (token.isBlank()) {
                throw new TokenFailure("missing-token", false, TokenScheme.DPOP);
            }
            return new PresentedToken(TokenScheme.DPOP, token);
        }
        throw new TokenFailure("unsupported-auth-scheme", true, TokenScheme.BEARER);
    }

    private static String challengeFor(TokenScheme scheme) {
        return scheme == TokenScheme.DPOP ? INVALID_DPOP_CHALLENGE : INVALID_BEARER_CHALLENGE;
    }

    private static String safeHeader(HttpExchange exchange, String name, String fallback) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value != null && value.matches("[a-zA-Z0-9._-]{1,64}") ? value : fallback;
    }

    private static String safeCallerContext(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst("X-Experiment-Actor");
        return value != null && Set.of("consumer-a", "logical-attacker").contains(value)
                ? value : "consumer-a";
    }

    private void recordSafeHandlerFailure(String scenario, String variant, Exception failure) {
        java.util.ArrayList<String> classes = new java.util.ArrayList<>();
        Throwable current = failure;
        while (current != null && classes.size() < 8) {
            classes.add(current.getClass().getName());
            current = current.getCause();
        }
        JsonSupport.appendJsonLine(runtimeRoot.resolve("events/errors/harness.jsonl"), java.util.Map.of(
                "runId", runId,
                "scenarioId", scenario,
                "variantId", variant,
                "component", "producer",
                "failureLocation", safeFailureLocation(failure),
                "failureClasses", classes));
    }

    private static String safeFailureLocation(Throwable failure) {
        StackTraceElement[] trace = failure.getStackTrace();
        if (trace.length == 0) {
            return failure.getClass().getName() + "#unknown";
        }
        StackTraceElement location = trace[0];
        return location.getClassName() + "#" + location.getMethodName() + ":" + location.getLineNumber();
    }

    private void sendProblem(HttpExchange exchange, TelemetryRecorder telemetry, int status,
                             String problemType, String title, String challenge) throws IOException {
        var problem = JsonSupport.MAPPER.createObjectNode();
        problem.put("type", problemType);
        problem.put("title", title);
        problem.put("status", status);
        String body = JsonSupport.compact(problem);
        var providerResponse = contracts.validateProviderError(status, body);
        telemetry.contract(providerResponse.role(), providerResponse.phase(), providerResponse.result());
        telemetry.externalError(status, challenge == null ? "none" : challengeClass(challenge), problemType);
        if (challenge != null) {
            exchange.getResponseHeaders().set("WWW-Authenticate", challenge);
        }
        send(exchange, status, "application/problem+json", body);
    }

    private static String challengeClass(String challenge) {
        if (MISSING_BEARER_CHALLENGE.equals(challenge)) {
            return "bearer-missing";
        }
        if (INVALID_BEARER_CHALLENGE.equals(challenge)) {
            return "bearer-invalid-token";
        }
        if (INSUFFICIENT_SCOPE_CHALLENGE.equals(challenge)) {
            return "bearer-insufficient-scope";
        }
        if (INVALID_DPOP_CHALLENGE.equals(challenge)) {
            return "dpop-invalid-token";
        }
        throw new IllegalArgumentException("Unknown security challenge");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public enum LocalPolicyDecision {
        ALLOW(ExperimentConfig.POLICY_VERSION),
        DENY("phase-2-local-deny-1.0.0");

        private final String policyVersion;

        LocalPolicyDecision(String policyVersion) {
            this.policyVersion = policyVersion;
        }

        public String policyVersion() {
            return policyVersion;
        }
    }

    private enum TokenScheme {
        BEARER,
        DPOP
    }

    private record PresentedToken(TokenScheme scheme, String serialized) {
    }

    private static final class TokenFailure extends Exception {
        private final String safeReason;
        private final boolean presented;
        private final TokenScheme scheme;

        private TokenFailure(String safeReason, boolean presented, TokenScheme scheme) {
            this(safeReason, presented, scheme, null);
        }

        private TokenFailure(String safeReason, boolean presented, TokenScheme scheme, Throwable cause) {
            super(safeReason, cause);
            this.safeReason = safeReason;
            this.presented = presented;
            this.scheme = scheme;
        }

        private String safeReason() {
            return safeReason;
        }

        private boolean presented() {
            return presented;
        }

        private TokenScheme scheme() {
            return scheme;
        }
    }

    private static final class SenderConstraintFailure extends Exception {
        private final String safeReason;

        private SenderConstraintFailure(String safeReason) {
            super(safeReason);
            this.safeReason = safeReason;
        }

        private SenderConstraintFailure(String safeReason, Throwable cause) {
            super(safeReason, cause);
            this.safeReason = safeReason;
        }

        private String safeReason() {
            return safeReason;
        }
    }

    private static final class AuthorizationFailure extends Exception {
        private final String safeReason;
        private final boolean insufficientScope;
        private final String policyVersion;

        private AuthorizationFailure(String safeReason, boolean insufficientScope,
                                     String policyVersion) {
            super(safeReason);
            this.safeReason = safeReason;
            this.insufficientScope = insufficientScope;
            this.policyVersion = policyVersion;
        }

        private String safeReason() {
            return safeReason;
        }

        private boolean insufficientScope() {
            return insufficientScope;
        }

        private String policyVersion() {
            return policyVersion;
        }
    }
}
