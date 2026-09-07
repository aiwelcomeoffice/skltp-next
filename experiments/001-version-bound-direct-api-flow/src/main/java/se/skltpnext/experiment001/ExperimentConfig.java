package se.skltpnext.experiment001;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public final class ExperimentConfig {
    public static final String RELEASE_ID = "E001-RELEASE";
    public static final String RELEASE_VERSION = "1.0.0";
    public static final String PARAMETER_SET_ID = "E001-PARAMS-1.0.0";
    public static final String CONTRACT_ID = "E001-READ-API";
    public static final String CONTRACT_VERSION = "1.0.0";
    public static final String FEDERATION_ID = "urn:skltp-next:experiment-001:federation";
    public static final String TEST_CONTEXT_ID = "urn:skltp-next:experiment-001:context:local";
    public static final String ORGANIZATION_A = "urn:skltp-next:experiment-001:organization:a";
    public static final String ORGANIZATION_B = "urn:skltp-next:experiment-001:organization:b";
    public static final String SYSTEM_A = "urn:skltp-next:experiment-001:system:consumer-a";
    public static final String CLIENT_ID = "urn:skltp-next:experiment-001:client:consumer-a";
    public static final String API_PROFILE_ID = "urn:skltp-next:experiment-001:api:synthetic-read";
    public static final String SCOPE_READ = "synthetic.read";
    public static final String SCOPE_INSUFFICIENT = "synthetic.insufficient";
    public static final String AUDIENCE = "urn:skltp-next:experiment-001:audience:producer-b";
    public static final String METADATA_ISSUER = "urn:skltp-next:experiment-001:metadata-authority";
    public static final String POLICY_ID = "E001-PRODUCER-POLICY";
    public static final String POLICY_VERSION = "1.0.0";
    public static final Instant T0 = Instant.parse("2026-08-18T12:00:00Z");
    public static final int TOKEN_SECONDS = 120;
    public static final int ASSERTION_SECONDS = 30;
    public static final int DPOP_VERIFIER_WINDOW_SECONDS = 7;
    public static final Set<String> PHASE_1_VARIANTS = Set.of(
            "E001-REL-001/valid",
            "E001-DIS-001/baseline",
            "E001-FLOW-001/baseline",
            "E001-CON-001/baseline");
    public static final Set<String> PHASE_2_VARIANTS = Set.of(
            "E001-FLOW-002/baseline",
            "E001-SEC-001/baseline",
            "E001-SEC-002/baseline",
            "E001-AUTHZ-001/insufficient-scope",
            "E001-AUTHZ-001/local-policy-deny",
            "E001-TOK-001/missing",
            "E001-TOK-001/wrong-issuer",
            "E001-TOK-001/wrong-audience",
            "E001-TOK-001/bad-signature",
            "E001-TOK-001/disallowed-algorithm",
            "E001-TOK-001/wrong-type",
            "E001-TOK-001/expired",
            "E001-TOK-001/not-yet-valid",
            "E001-TOK-001/missing-required-claim",
            "E001-TOK-001/wrong-client-id",
            "E001-TOK-001/wrong-sub");
    public static final Set<String> PHASE_3_VARIANTS = Set.of(
            "E001-DIS-002/baseline",
            "E001-LIFE-001/inactive-B-before-token",
            "E001-LIFE-001/inactive-A-token-request-after-offboarding",
            "E001-LIFE-001/inactive-A-existing-token-after-offboarding",
            "E001-LIFE-001/unpublished-service",
            "E001-META-002/service-stale",
            "E001-META-002/membership-stale",
            "E001-META-002/iam-stale",
            "E001-META-001/iam-as-signing-key-revoked-after-bound",
            "E001-META-001/service-manipulated",
            "E001-META-001/service-rollback",
            "E001-META-001/service-cross-context",
            "E001-META-001/service-wrong-metadata-issuer",
            "E001-META-001/service-wrong-key",
            "E001-META-001/membership-manipulated",
            "E001-META-001/membership-rollback",
            "E001-META-001/membership-cross-context",
            "E001-META-001/membership-wrong-metadata-issuer",
            "E001-META-001/membership-wrong-key",
            "E001-META-001/iam-manipulated",
            "E001-META-001/iam-rollback",
            "E001-META-001/iam-cross-context",
            "E001-META-001/iam-wrong-metadata-issuer",
            "E001-META-001/iam-wrong-key",
            "E001-META-001/iam-unknown-organization",
            "E001-META-001/iam-unknown-system",
            "E001-META-001/iam-unknown-client",
            "E001-META-001/iam-client-wrong-system",
            "E001-META-001/iam-system-wrong-organization",
            "E001-META-001/iam-client-wrong-key",
            "E001-DIS-003/missing-endpoint",
            "E001-DIS-003/ambiguous-endpoint",
            "E001-REL-001/missing-ref",
            "E001-REL-001/ambiguous-ref",
            "E001-REL-001/digest-mutation");
    public static final Set<String> IMPLEMENTED_VARIANTS;

    static {
        Set<String> variants = new HashSet<>(PHASE_1_VARIANTS);
        variants.addAll(PHASE_2_VARIANTS);
        variants.addAll(PHASE_3_VARIANTS);
        IMPLEMENTED_VARIANTS = Set.copyOf(variants);
    }

    private ExperimentConfig() {
    }

    public static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (!cwd.resolve("pom.xml").toFile().isFile()) {
            throw new IllegalStateException("Run the CLI from the experiment module directory");
        }
        return cwd;
    }

    public static Path runtimeRoot(String runId) {
        validateRunId(runId);
        return moduleRoot().resolve("target/experiment-001/runtime").resolve(runId);
    }

    public static Path evidenceRoot(String runId) {
        validateRunId(runId);
        return moduleRoot().resolve("target/experiment-001/evidence").resolve(runId);
    }

    public static void validateRunId(String runId) {
        if (runId == null || !runId.matches("[a-zA-Z0-9._-]{1,64}")) {
            throw new IllegalArgumentException("run-id must match [a-zA-Z0-9._-]{1,64}");
        }
    }

    public static URI resourceUri(URI producerEndpoint) {
        return producerEndpoint.resolve("/synthetic-records/synthetic-record-001");
    }
}
