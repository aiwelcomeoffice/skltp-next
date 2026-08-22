package se.skltpnext.experiment001.authorization;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import se.skltpnext.experiment001.ExperimentConfig;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

/** Nimbus-backed cryptographic verification plus the local RFC 9068 profile checks. */
public final class AccessTokenValidator {
    private static final Set<String> REQUIRED_CLAIMS = Set.of(
            "iss", "exp", "aud", "sub", "client_id", "iat", "jti", "scope");
    private static final long CLOCK_SKEW_SECONDS = 2;

    private final String expectedIssuer;
    private final ECKey authorizationServerPublicKey;

    public AccessTokenValidator(String expectedIssuer, ECKey authorizationServerPublicKey) {
        this.expectedIssuer = expectedIssuer;
        this.authorizationServerPublicKey = authorizationServerPublicKey.toPublicJWK();
    }

    public ValidationResult validate(String serialized) throws TokenValidationException {
        final SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(serialized);
        } catch (Exception e) {
            throw new TokenValidationException("malformed-token", e);
        }
        if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())) {
            throw new TokenValidationException("disallowed-algorithm");
        }
        if (!new JOSEObjectType("at+jwt").equals(jwt.getHeader().getType())) {
            throw new TokenValidationException("wrong-type");
        }
        if (!authorizationServerPublicKey.getKeyID().equals(jwt.getHeader().getKeyID())) {
            throw new TokenValidationException("unknown-signing-key");
        }
        try {
            if (!jwt.verify(new ECDSAVerifier(authorizationServerPublicKey))) {
                throw new TokenValidationException("bad-signature");
            }
        } catch (TokenValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenValidationException("bad-signature", e);
        }

        final JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw new TokenValidationException("malformed-claims", e);
        }
        if (!claims.getClaims().keySet().containsAll(REQUIRED_CLAIMS)) {
            throw new TokenValidationException("missing-required-claim");
        }
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new TokenValidationException("wrong-issuer");
        }
        if (!List.of(ExperimentConfig.AUDIENCE).equals(claims.getAudience())) {
            throw new TokenValidationException("wrong-audience");
        }

        Instant now = Instant.now();
        Date expiration = claims.getExpirationTime();
        if (expiration == null
                || expiration.toInstant().isBefore(now.minusSeconds(CLOCK_SKEW_SECONDS))) {
            throw new TokenValidationException("expired");
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null
                && notBefore.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw new TokenValidationException("not-yet-valid");
        }
        Date issueTime = claims.getIssueTime();
        if (issueTime == null
                || issueTime.toInstant().isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw new TokenValidationException("not-yet-valid");
        }
        try {
            if (!ExperimentConfig.CLIENT_ID.equals(claims.getStringClaim("client_id"))) {
                throw new TokenValidationException("wrong-client-id");
            }
        } catch (TokenValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenValidationException("wrong-client-id", e);
        }
        if (!ExperimentConfig.CLIENT_ID.equals(claims.getSubject())) {
            throw new TokenValidationException("wrong-sub");
        }
        return new ValidationResult(claims);
    }

    public record ValidationResult(JWTClaimsSet claims) {
    }

    public static final class TokenValidationException extends Exception {
        private final String safeReason;

        public TokenValidationException(String safeReason) {
            super(safeReason);
            this.safeReason = safeReason;
        }

        public TokenValidationException(String safeReason, Throwable cause) {
            super(safeReason, cause);
            this.safeReason = safeReason;
        }

        public String safeReason() {
            return safeReason;
        }
    }
}
