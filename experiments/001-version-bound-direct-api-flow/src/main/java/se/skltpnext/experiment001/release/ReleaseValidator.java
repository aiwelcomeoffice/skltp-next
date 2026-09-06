package se.skltpnext.experiment001.release;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.util.HashSet;
import java.util.Set;

public final class ReleaseValidator {
    private static final String ROOT = "experiment-001/";
    private static final String DEFAULT_INDEX = ROOT + "release/index-1.0.0.json";
    private static final Set<String> REQUIRED_TYPES = Set.of(
            "purpose", "responsibility", "semantics", "contract", "contract-overlay",
            "discovery-profile", "membership-profile", "iam-profile",
            "observability-profile", "audit-profile", "producer-policy", "parameters",
            "scenario-catalog");

    public ReleaseSelection validate() {
        return validateIndex(DEFAULT_INDEX);
    }

    /**
     * Validates one of the immutable negative release-index fixtures used by the
     * {@code E001-REL-001} failure variants ({@code missing-ref}, {@code ambiguous-ref},
     * {@code digest-mutation}). The pinned {@code valid} release index is never mutated.
     */
    public ReleaseSelection validateFixture(String variantId) {
        return validateIndex(ROOT + "release/fixtures/index-" + variantId + "-1.0.0.json");
    }

    private ReleaseSelection validateIndex(String indexResourcePath) {
        JsonNode index = JsonSupport.readResource(indexResourcePath);
        JsonNode schema = JsonSupport.readResource(ROOT + "schemas/release-index.schema.json");
        JsonSupport.validate(schema, index, "release index");

        Set<String> types = new HashSet<>();
        for (JsonNode reference : index.required("references")) {
            String type = reference.required("type").textValue();
            if (!types.add(type)) {
                throw new ReleaseValidationException(FailureCategory.AMBIGUOUS_REF,
                        "Ambiguous release reference type: " + type);
            }
            String path = reference.required("path").textValue();
            String actual = JsonSupport.sha256(JsonSupport.readResourceBytes(ROOT + path));
            String expected = reference.required("sha256").textValue();
            if (!expected.equals(actual)) {
                throw new ReleaseValidationException(FailureCategory.DIGEST_MUTATION,
                        "Digest mismatch for immutable release reference " + type);
            }
        }
        if (!types.equals(REQUIRED_TYPES)) {
            throw new ReleaseValidationException(FailureCategory.MISSING_REF,
                    "Release references are not exact: " + types);
        }
        String compact = index.toString();
        for (String forbidden : Set.of("endpointUri", "membershipStatus", "privateKey", "accessToken")) {
            if (compact.contains(forbidden)) {
                throw new ReleaseValidationException(FailureCategory.DYNAMIC_VALUE_PRESENT,
                        "Release index contains dynamic runtime value: " + forbidden);
            }
        }
        return new ReleaseSelection(index.required("releaseId").textValue(),
                index.required("releaseVersion").textValue(), types.size());
    }

    public record ReleaseSelection(String releaseId, String releaseVersion, int referenceCount) {
    }

    public enum FailureCategory {
        MISSING_REF, AMBIGUOUS_REF, DIGEST_MUTATION, DYNAMIC_VALUE_PRESENT
    }

    public static final class ReleaseValidationException extends IllegalArgumentException {
        private final FailureCategory category;

        public ReleaseValidationException(FailureCategory category, String message) {
            super(message);
            this.category = category;
        }

        public FailureCategory category() {
            return category;
        }
    }
}
