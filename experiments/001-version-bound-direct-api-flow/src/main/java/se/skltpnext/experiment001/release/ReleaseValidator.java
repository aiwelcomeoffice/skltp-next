package se.skltpnext.experiment001.release;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.util.HashSet;
import java.util.Set;

public final class ReleaseValidator {
    private static final String ROOT = "experiment-001/";
    private static final Set<String> REQUIRED_TYPES = Set.of(
            "purpose", "responsibility", "semantics", "contract", "contract-overlay",
            "discovery-profile", "membership-profile", "iam-profile",
            "observability-profile", "audit-profile", "producer-policy", "parameters",
            "scenario-catalog");

    public ReleaseSelection validate() {
        JsonNode index = JsonSupport.readResource(ROOT + "release/index-1.0.0.json");
        JsonNode schema = JsonSupport.readResource(ROOT + "schemas/release-index.schema.json");
        JsonSupport.validate(schema, index, "release index");

        Set<String> types = new HashSet<>();
        for (JsonNode reference : index.required("references")) {
            String type = reference.required("type").textValue();
            if (!types.add(type)) {
                throw new IllegalArgumentException("Ambiguous release reference type: " + type);
            }
            String path = reference.required("path").textValue();
            String actual = JsonSupport.sha256(JsonSupport.readResourceBytes(ROOT + path));
            String expected = reference.required("sha256").textValue();
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException("Digest mismatch for immutable release reference " + type);
            }
        }
        if (!types.equals(REQUIRED_TYPES)) {
            throw new IllegalArgumentException("Release references are not exact: " + types);
        }
        String compact = index.toString();
        for (String forbidden : Set.of("endpointUri", "membershipStatus", "privateKey", "accessToken")) {
            if (compact.contains(forbidden)) {
                throw new IllegalArgumentException("Release index contains dynamic runtime value: " + forbidden);
            }
        }
        return new ReleaseSelection(index.required("releaseId").textValue(),
                index.required("releaseVersion").textValue(), types.size());
    }

    public record ReleaseSelection(String releaseId, String releaseVersion, int referenceCount) {
    }
}
