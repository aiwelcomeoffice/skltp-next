package se.skltpnext.experiment001;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
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

