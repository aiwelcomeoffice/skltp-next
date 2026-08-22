package se.skltpnext.experiment001.authorization;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.evidence.CanaryRegistry;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Generates only the synthetic malformed bearer-token stimuli required by
 * E001-TOK-001. The serialized values remain private runtime material.
 */
public final class AccessTokenFixtures {
    public static final String INSUFFICIENT_SCOPE = ExperimentConfig.SCOPE_INSUFFICIENT;
    public static final Set<String> VARIANTS = Set.of(
            "missing",
            "wrong-issuer",
            "wrong-audience",
            "bad-signature",
            "disallowed-algorithm",
            "wrong-type",
            "expired",
            "not-yet-valid",
            "missing-required-claim",
            "wrong-client-id",
            "wrong-sub");

    private AccessTokenFixtures() {
    }

    public static String create(Path runtimeRoot, String expectedIssuer, String variant) {
        return fixture(runtimeRoot, expectedIssuer, variant).serialized();
    }

    public static TokenFixture fixture(Path runtimeRoot, String expectedIssuer, String variant) {
        if (!VARIANTS.contains(variant)) {
            throw new IllegalArgumentException("Unknown E001-TOK-001 variant");
        }
        if ("missing".equals(variant)) {
            return new TokenFixture(false, null);
        }

        try {
            CryptoMaterial material = CryptoMaterial.load(runtimeRoot);
            Instant now = Instant.now();
            JWTClaimsSet claims = claims(expectedIssuer, variant, now, runtimeRoot);
            JOSEObjectType type = "wrong-type".equals(variant)
                    ? new JOSEObjectType("JWT") : new JOSEObjectType("at+jwt");
            SignedJWT jwt;
            if ("disallowed-algorithm".equals(variant)) {
                byte[] secret = new byte[32];
                new SecureRandom().nextBytes(secret);
                jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256)
                        .type(type)
                        .keyID(material.authorizationServerSigningKey().getKeyID())
                        .build(), claims);
                jwt.sign(new MACSigner(secret));
            } else {
                ECKey signingKey = material.authorizationServerSigningKey();
                if ("bad-signature".equals(variant)) {
                    signingKey = new ECKeyGenerator(Curve.P_256)
                            .keyID(material.authorizationServerSigningKey().getKeyID())
                            .generate();
                }
                jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .type(type)
                        .keyID(material.authorizationServerSigningKey().getKeyID())
                        .build(), claims);
                jwt.sign(new ECDSASigner(signingKey));
            }
            String serialized = jwt.serialize();
            new CanaryRegistry(runtimeRoot.resolve("private"))
                    .register("access_token", serialized);
            return new TokenFixture(true, serialized);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create synthetic access-token fixture", e);
        }
    }

    private static JWTClaimsSet claims(String expectedIssuer, String variant, Instant now,
                                       Path runtimeRoot) {
        Instant issueTime = now.minusSeconds(1);
        Instant notBefore = now.minusSeconds(1);
        Instant expiration = now.plusSeconds(ExperimentConfig.TOKEN_SECONDS);
        if ("expired".equals(variant)) {
            issueTime = now.minusSeconds(ExperimentConfig.TOKEN_SECONDS + 10L);
            notBefore = issueTime;
            expiration = now.minusSeconds(10);
        } else if ("not-yet-valid".equals(variant)) {
            notBefore = now.plusSeconds(30);
        }

        String issuer = "wrong-issuer".equals(variant)
                ? "urn:skltp-next:experiment-001:issuer:wrong" : expectedIssuer;
        String audience = "wrong-audience".equals(variant)
                ? "urn:skltp-next:experiment-001:audience:wrong" : ExperimentConfig.AUDIENCE;
        String subject = "wrong-sub".equals(variant)
                ? "urn:skltp-next:experiment-001:client:wrong-sub" : ExperimentConfig.CLIENT_ID;
        String clientId = "wrong-client-id".equals(variant)
                ? "urn:skltp-next:experiment-001:client:wrong-client-id" : ExperimentConfig.CLIENT_ID;

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issueTime(Date.from(issueTime))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration))
                .jwtID("E001-TOKEN-FIXTURE-" + UUID.randomUUID())
                .claim("scope", ExperimentConfig.SCOPE_READ)
                .claim("synthetic_sensitive_marker",
                        new CanaryRegistry(runtimeRoot.resolve("private"))
                                .newValue("sensitive_claim"));
        if (!"missing-required-claim".equals(variant)) {
            builder.claim("client_id", clientId);
        }
        return builder.build();
    }

    public record TokenFixture(boolean presented, String serialized) {
        public String bearerAuthorizationHeader() {
            if (!presented || serialized == null) {
                throw new IllegalStateException("The missing-token fixture has no Authorization header");
            }
            return "Bearer " + serialized;
        }
    }
}
