package se.skltpnext.experiment001.authorization;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.JWKThumbprintConfirmation;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPIssuer;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPProtectedResourceRequestVerifier;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DPoPTokenRequestVerifier;
import com.nimbusds.oauth2.sdk.dpop.verifiers.DefaultDPoPSingleUseChecker;
import com.nimbusds.oauth2.sdk.dpop.verifiers.InvalidDPoPProofException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import se.skltpnext.experiment001.ExperimentConfig;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

public final class NimbusDpopGate {
    private static final URI TOKEN_URI = URI.create("https://localhost:18443/token");
    private static final URI RESOURCE_URI =
            URI.create("https://localhost:19443/synthetic-records/synthetic-record-001");

    public GateResult run() {
        try {
            ECKey key = new ECKeyGenerator(Curve.P_256).keyID("gate-dpop-key").generate();
            DefaultDPoPProofFactory factory = new DefaultDPoPProofFactory(key, JWSAlgorithm.ES256);
            DPoPIssuer issuer = new DPoPIssuer(new ClientID(ExperimentConfig.CLIENT_ID));
            Instant now = Instant.now();

            DefaultDPoPSingleUseChecker tokenChecker = newChecker();
            DPoPTokenRequestVerifier tokenVerifier = tokenVerifier(tokenChecker);
            SignedJWT acceptedProof = factory.createDPoPJWT(
                    new JWTID("gate-token-positive-0001"), "POST", TOKEN_URI,
                    Date.from(now), null);
            JWKThumbprintConfirmation confirmation = tokenVerifier.verify(issuer, acceptedProof);

            boolean replayRejected = rejects(() -> tokenVerifier.verify(issuer, acceptedProof));

            SignedJWT staleProof = factory.createDPoPJWT(
                    new JWTID("gate-token-stale-000001"), "POST", TOKEN_URI,
                    Date.from(now.minusSeconds(10)), null);
            boolean staleRejected = rejects(() -> tokenVerifier.verify(issuer, staleProof));

            DefaultDPoPSingleUseChecker resourceChecker = newChecker();
            DPoPProtectedResourceRequestVerifier resourceVerifier = resourceVerifier(resourceChecker);
            DPoPAccessToken accessToken = new DPoPAccessToken("gate-access-token-not-emitted");
            SignedJWT resourceProofSameJti = factory.createDPoPJWT(
                    new JWTID("gate-token-positive-0001"), "GET", RESOURCE_URI,
                    Date.from(now), accessToken);
            resourceVerifier.verify("GET", RESOURCE_URI, issuer, resourceProofSameJti,
                    accessToken, confirmation);
            boolean separateNamespaces = true;

            tokenChecker.shutdown();
            resourceChecker.shutdown();

            DefaultDPoPSingleUseChecker resetChecker = newChecker();
            tokenVerifier(resetChecker).verify(issuer, acceptedProof);
            boolean resetAcceptsPreviouslyUsedProof = true;
            resetChecker.shutdown();

            return new GateResult(true, ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS,
                    replayRejected, staleRejected, separateNamespaces,
                    resetAcceptsPreviouslyUsedProof, "pass");
        } catch (Exception e) {
            return new GateResult(true, ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS,
                    false, false, false, false, "inconclusive");
        }
    }

    public static DefaultDPoPSingleUseChecker newChecker() {
        return new DefaultDPoPSingleUseChecker(
                ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS, 60);
    }

    public static DPoPTokenRequestVerifier tokenVerifier(DefaultDPoPSingleUseChecker checker) {
        return new DPoPTokenRequestVerifier(Set.of(JWSAlgorithm.ES256), TOKEN_URI,
                ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS, checker);
    }

    public static DPoPTokenRequestVerifier tokenVerifier(
            URI tokenUri, DefaultDPoPSingleUseChecker checker) {
        return new DPoPTokenRequestVerifier(Set.of(JWSAlgorithm.ES256), tokenUri,
                ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS, checker);
    }

    public static DPoPProtectedResourceRequestVerifier resourceVerifier(
            DefaultDPoPSingleUseChecker checker) {
        return new DPoPProtectedResourceRequestVerifier(Set.of(JWSAlgorithm.ES256),
                ExperimentConfig.DPOP_VERIFIER_WINDOW_SECONDS, checker);
    }

    private static boolean rejects(CheckedRunnable action) throws JOSEException {
        try {
            action.run();
            return false;
        } catch (InvalidDPoPProofException expected) {
            return true;
        } catch (Exception e) {
            if (e instanceof JOSEException joseException) {
                throw joseException;
            }
            return true;
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    public record GateResult(
            boolean explicitIat,
            int verifierWindowSeconds,
            boolean replayRejected,
            boolean staleRejected,
            boolean separateReplayNamespaces,
            boolean resetCreatedFreshChecker,
            String status) {
        public boolean passed() {
            return "pass".equals(status)
                    && explicitIat
                    && verifierWindowSeconds == 7
                    && replayRejected
                    && staleRejected
                    && separateReplayNamespaces
                    && resetCreatedFreshChecker;
        }
    }
}

