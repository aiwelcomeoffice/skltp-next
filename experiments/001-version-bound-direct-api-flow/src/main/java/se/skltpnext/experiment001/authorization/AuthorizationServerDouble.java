package se.skltpnext.experiment001.authorization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientAuthenticationVerifier;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientCredentialsSelector;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.ExpendedJTIChecker;
import com.nimbusds.oauth2.sdk.auth.verifier.Hint;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.auth.verifier.JWTAudienceCheck;
import com.nimbusds.oauth2.sdk.dpop.JWKThumbprintConfirmation;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPIssuer;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPTokenRequestVerifier;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DefaultDPoPSingleUseChecker;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.evidence.CanaryRegistry;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthorizationServerDouble implements HttpHandler {
    private final Path runtimeRoot;
    private final String runId;
    private final URI tokenEndpoint;
    private final CryptoMaterial material;
    private final MetadataStores metadataStores;
    private volatile DefaultDPoPSingleUseChecker dpopChecker;
    private volatile Map<String, Date> assertionReplay;

    public AuthorizationServerDouble(Path runtimeRoot, String runId, URI tokenEndpoint,
                                     CryptoMaterial material) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        this.tokenEndpoint = tokenEndpoint;
        this.material = material;
        metadataStores = new MetadataStores(runtimeRoot);
        reset();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String scenario = safeHeader(exchange, "X-Experiment-Scenario", "runtime");
        String variant = safeHeader(exchange, "X-Experiment-Variant", "baseline");
        try (TelemetryRecorder telemetry = new TelemetryRecorder(
                runtimeRoot, runId, scenario, variant, "authorization-server")) {
            telemetry.network("consumer-a", "authorization-server", "AS-LISTENER",
                    exchange.getRequestMethod(), "/token", false);
            if (!"POST".equals(exchange.getRequestMethod()) || !"/token".equals(exchange.getRequestURI().getPath())) {
                send(exchange, 404, "application/problem+json",
                        "{\"type\":\"urn:skltp-next:experiment-001:error:not-found\",\"title\":\"Not found\",\"status\":404}");
                return;
            }
            try {
                Map<String, List<String>> form = parseForm(new String(
                        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                if (!List.of("client_credentials").equals(form.get("grant_type"))
                        || !List.of(ExperimentConfig.SCOPE_READ).equals(form.get("scope"))) {
                    throw new IllegalArgumentException("unsupported token request profile");
                }
                PrivateKeyJWT authentication = PrivateKeyJWT.parse(form);
                new CanaryRegistry(runtimeRoot.resolve("private"))
                        .register("client_assertion", authentication.getClientAssertion().serialize());
                clientAuthenticationVerifier().verify(authentication, Set.<Hint>of(), new Context<>());
                telemetry.decision("authorization-server.client-authentication",
                        "client_authentication", "allow", "private-key-jwt-valid");

                if (!metadataStores.membershipActive(ExperimentConfig.ORGANIZATION_A, "consumer")) {
                    throw new IllegalArgumentException("consumer membership inactive");
                }
                String dpopHeader = exchange.getRequestHeaders().getFirst("DPoP");
                if (dpopHeader == null) {
                    throw new IllegalArgumentException("missing DPoP proof");
                }
                SignedJWT proof = SignedJWT.parse(dpopHeader);
                new CanaryRegistry(runtimeRoot.resolve("private"))
                        .register("dpop_proof", proof.serialize());
                DPoPTokenRequestVerifier proofVerifier = NimbusDpopGate.tokenVerifier(tokenEndpoint, dpopChecker);
                JWKThumbprintConfirmation confirmation = proofVerifier.verify(
                        new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID)), proof);
                telemetry.decision("authorization-server.sender-constraint", "sender_constraint",
                        "allow", "dpop-token-proof-valid");

                String token = issueToken(confirmation);
                new CanaryRegistry(runtimeRoot.resolve("private")).register("access_token", token);
                ObjectNode response = JsonSupport.MAPPER.createObjectNode();
                response.put("access_token", token);
                response.put("token_type", "DPoP");
                response.put("expires_in", ExperimentConfig.TOKEN_SECONDS);
                response.put("scope", ExperimentConfig.SCOPE_READ);
                telemetry.decision("authorization-server.token-issuance", "token_issuance",
                        "allow", "rfc9068-dpop-bound");
                send(exchange, 200, "application/json", JsonSupport.compact(response));
            } catch (Exception e) {
                telemetry.decision("authorization-server.client-authentication",
                        "client_authentication", "deny", "invalid-client-or-proof");
                send(exchange, 401, "application/problem+json",
                        "{\"type\":\"urn:skltp-next:experiment-001:error:invalid-client\",\"title\":\"Invalid client\",\"status\":401}");
            }
        }
    }

    public synchronized void reset() {
        if (dpopChecker != null) {
            dpopChecker.shutdown();
        }
        dpopChecker = NimbusDpopGate.newChecker();
        assertionReplay = new ConcurrentHashMap<>();
    }

    public String warmUpSecurityLibraries() {
        try {
            Instant now = Instant.now();
            JWTAuthenticationClaimsSet claims = new JWTAuthenticationClaimsSet(
                    new ClientID(ExperimentConfig.CLIENT_ID),
                    List.of(new Audience(tokenEndpoint.toString())),
                    Date.from(now.plusSeconds(ExperimentConfig.ASSERTION_SECONDS)),
                    Date.from(now.minusSeconds(1)), Date.from(now),
                    new JWTID("E001-WARMUP-ASSERTION-" + UUID.randomUUID()));
            PrivateKeyJWT authentication = new PrivateKeyJWT(
                    claims, JWSAlgorithm.ES256,
                    material.clientAuthenticationKey().toECPrivateKey(),
                    material.clientAuthenticationKey().getKeyID(), null);
            clientAuthenticationVerifier().verify(authentication, Set.<Hint>of(), new Context<>());

            DefaultDPoPProofFactory proofFactory = new DefaultDPoPProofFactory(
                    material.dpopKey(), JWSAlgorithm.ES256);
            SignedJWT proof = proofFactory.createDPoPJWT(
                    new JWTID("E001-WARMUP-DPOP-" + UUID.randomUUID()),
                    "POST", tokenEndpoint, Date.from(now), null);
            JWKThumbprintConfirmation confirmation = NimbusDpopGate
                    .tokenVerifier(tokenEndpoint, dpopChecker)
                    .verify(new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID)), proof);
            return issueToken(confirmation);
        } catch (Exception e) {
            throw new IllegalStateException("Authorization security-library warm-up failed", e);
        } finally {
            reset();
        }
    }

    private ClientAuthenticationVerifier<Void> clientAuthenticationVerifier() {
        ClientCredentialsSelector<Void> selector = new ClientCredentialsSelector<>() {
            @Override
            public List<Secret> selectClientSecrets(ClientID clientID, ClientAuthenticationMethod method,
                                                    Context<Void> context) throws InvalidClientException {
                return List.of();
            }

            @Override
            public List<? extends PublicKey> selectPublicKeys(ClientID clientID,
                    ClientAuthenticationMethod method, JWSHeader header, boolean forceRefresh,
                    Context<Void> context) throws InvalidClientException {
                if (!ExperimentConfig.CLIENT_ID.equals(clientID.getValue())
                        || !ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(method)
                        || !JWSAlgorithm.ES256.equals(header.getAlgorithm())
                        || !material.clientAuthenticationKey().getKeyID().equals(header.getKeyID())) {
                    throw InvalidClientException.NO_MATCHING_JWK;
                }
                try {
                    return List.of(material.clientAuthenticationKey().toECPublicKey());
                } catch (Exception e) {
                    throw new InvalidClientException("Cannot select client public key");
                }
            }
        };
        ExpendedJTIChecker<Void> replayChecker = new ExpendedJTIChecker<>() {
            @Override
            public boolean isExpended(JWTID jti, ClientID clientId,
                                      ClientAuthenticationMethod method, Context<Void> context) {
                Date expires = assertionReplay.get(jti.getValue());
                return expires != null && expires.after(new Date());
            }

            @Override
            public void markExpended(JWTID jti, Date expiration, ClientID clientId,
                                     ClientAuthenticationMethod method, Context<Void> context) {
                assertionReplay.put(jti.getValue(), expiration);
            }
        };
        return new ClientAuthenticationVerifier<>(selector,
                Set.of(new Audience(tokenEndpoint.toString())), JWTAudienceCheck.STRICT, replayChecker);
    }

    private String issueToken(JWKThumbprintConfirmation confirmation) throws Exception {
        Instant now = Instant.now();
        String sensitiveMarker = new CanaryRegistry(runtimeRoot.resolve("private"))
                .newValue("sensitive_claim");
        Map.Entry<String, net.minidev.json.JSONObject> cnf = confirmation.toJWTClaim();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(tokenEndpoint.resolve("/").toString())
                .subject(ExperimentConfig.CLIENT_ID)
                .audience(ExperimentConfig.AUDIENCE)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(ExperimentConfig.TOKEN_SECONDS)))
                .jwtID("E001-TOKEN-" + UUID.randomUUID())
                .claim("client_id", ExperimentConfig.CLIENT_ID)
                .claim("scope", ExperimentConfig.SCOPE_READ)
                .claim(cnf.getKey(), cnf.getValue())
                .claim("synthetic_sensitive_marker", sensitiveMarker)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("at+jwt"))
                .keyID(material.authorizationServerSigningKey().getKeyID()).build(), claims);
        jwt.sign(new ECDSASigner(material.authorizationServerSigningKey()));
        return jwt.serialize();
    }

    private static Map<String, List<String>> parseForm(String form) {
        Map<String, List<String>> values = new HashMap<>();
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return values;
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
}
